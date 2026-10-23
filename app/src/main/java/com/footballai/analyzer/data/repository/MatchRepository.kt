package com.footballai.analyzer.data.repository

import com.footballai.analyzer.data.remote.HighlightlyApi
import com.footballai.analyzer.data.remote.dto.MatchDetailResponse
import com.footballai.analyzer.data.remote.dto.MatchDto
import com.footballai.analyzer.domain.model.Highlight
import com.footballai.analyzer.domain.model.Match
import com.footballai.analyzer.domain.model.MatchEvent
import com.footballai.analyzer.domain.model.MatchExtraData
import com.footballai.analyzer.domain.model.Player
import com.footballai.analyzer.domain.model.TeamLineup
import com.footballai.analyzer.domain.model.TeamStat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchRepository @Inject constructor(
    private val api: HighlightlyApi,
    private val cache: com.footballai.analyzer.data.local.MatchLocalCache
) {

    suspend fun getMatchesForDate(
        date: LocalDate,
        forceRefresh: Boolean = false
    ): Result<List<Match>> {
        val dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        if (!forceRefresh) {
            cache.getMatches(dateKey)?.let { return Result.success(it) }
        } else {
            cache.invalidateDate(dateKey)
        }

        return try {
            val allMatches = mutableListOf<MatchDto>()
            var offset = 0
            val pageSize = 100
            val maxPages = 10

            repeat(maxPages) {
                val response = api.getMatches(
                    date = dateKey,
                    timezone = "Europe/Budapest",
                    limit = pageSize,
                    offset = offset
                )
                val page = response.data
                if (page.isEmpty()) return@repeat
                allMatches.addAll(page)
                val total = response.pagination?.totalCount ?: page.size
                offset += pageSize
                if (offset >= total || page.size < pageSize) return@repeat
            }

            val domain = allMatches.map { it.toDomain() }
            val hasLive = domain.any { it.isLive }
            val today = LocalDate.now()
            val ttl = when {
                hasLive -> com.footballai.analyzer.data.local.MatchLocalCache.TTL_LIVE_MS
                date.isBefore(today) -> com.footballai.analyzer.data.local.MatchLocalCache.TTL_PAST_MS
                date.isAfter(today) -> com.footballai.analyzer.data.local.MatchLocalCache.TTL_FUTURE_MS
                else -> com.footballai.analyzer.data.local.MatchLocalCache.TTL_TODAY_MS
            }
            cache.putMatches(dateKey, domain, ttl)
            Result.success(domain)
        } catch (e: Exception) {
            // hálózat hiba: próbáljunk lejárt cache-t is? csak friss cache van a getMatches-ben
            cache.getMatches(dateKey)?.let { return Result.success(it) }
            Result.failure(e)
        }
    }

    suspend fun getMatchById(id: Long, forceRefresh: Boolean = false): Result<Match> {
        if (!forceRefresh) {
            cache.getMatch(id)?.let { return Result.success(it) }
        }
        return try {
            val response = api.getMatchById(id)
            val matchDto = response.firstOrNull()
                ?: return Result.failure(Exception("Meccs nem található"))
            val domain = matchDto.toDomain()
            val ttl = if (domain.isLive) {
                com.footballai.analyzer.data.local.MatchLocalCache.TTL_LIVE_MS
            } else {
                com.footballai.analyzer.data.local.MatchLocalCache.TTL_MATCH_MS
            }
            cache.putMatch(domain, ttl)
            Result.success(domain)
        } catch (e: Exception) {
            cache.getMatch(id)?.let { return Result.success(it) }
            Result.failure(e)
        }
    }

    suspend fun getMatchExtraData(
        matchId: Long,
        forceRefresh: Boolean = false,
        isLiveHint: Boolean = false
    ): MatchExtraData {
        if (!forceRefresh) {
            cache.getExtra(matchId)?.let { return it }
        } else {
            cache.invalidateMatch(matchId)
        }

        val stats = try {
            val list = api.getStatistics(matchId)
            list.map { teamDto ->
                TeamStat(
                    teamName = teamDto.team?.name ?: "Csapat",
                    stats = teamDto.statistics.associate { item ->
                        com.footballai.analyzer.util.StatTranslator.stat(item.displayName) to item.valueAsString()
                    }
                )
            }
        } catch (_: Exception) {
            emptyList()
        }

        val lineups = try {
            val resp = api.getLineups(matchId)
            val result = mutableListOf<TeamLineup>()
            fun mapPlayer(p: com.footballai.analyzer.data.remote.dto.LineupsPlayerDto): Player {
                val id = p.id
                val photo = if (id != null && id > 0) {
                    "https://highlightly.net/soccer/images/players/" + id + ".png"
                } else null
                return Player(
                    name = p.name ?: "?",
                    number = p.number,
                    position = p.position,
                    id = id,
                    photoUrl = photo
                )
            }
            resp.homeTeam?.let { team ->
                val rows = team.initialLineup.map { row -> row.map { mapPlayer(it) } }
                val starters = rows.flatten()
                val subs = team.substitutes.map { mapPlayer(it) }
                result.add(
                    TeamLineup(
                        teamName = team.name ?: "Hazai",
                        teamLogo = team.logo,
                        formation = team.formation,
                        formationRows = rows,
                        starters = starters,
                        substitutes = subs
                    )
                )
            }
            resp.awayTeam?.let { team ->
                val rows = team.initialLineup.map { row -> row.map { mapPlayer(it) } }
                val starters = rows.flatten()
                val subs = team.substitutes.map { mapPlayer(it) }
                result.add(
                    TeamLineup(
                        teamName = team.name ?: "Vendég",
                        teamLogo = team.logo,
                        formation = team.formation,
                        formationRows = rows,
                        starters = starters,
                        substitutes = subs
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }

        val highlights = try {
            val resp = api.getHighlights(matchId = matchId)
            resp.data.map {
                Highlight(
                    title = it.title ?: "Highlight",
                    url = it.url,
                    imageUrl = it.imgUrl
                )
            }
        } catch (_: Exception) {
            emptyList()
        }

        val events = try {
            api.getEvents(matchId).map { e ->
                MatchEvent(
                    minute = e.time ?: "?",
                    type = e.type ?: "Event",
                    player = e.player,
                    assist = e.assist?.takeIf { it.isNotBlank() },
                    substituted = e.substituted?.takeIf { it.isNotBlank() },
                    teamName = e.team?.name,
                    teamLogo = e.team?.logo
                )
            }
        } catch (_: Exception) {
            emptyList()
        }

        val extra = MatchExtraData(stats, lineups, highlights, events)
        val ttl = when {
            isLiveHint -> com.footballai.analyzer.data.local.MatchLocalCache.TTL_EXTRA_LIVE_MS
            events.isNotEmpty() || stats.isNotEmpty() -> com.footballai.analyzer.data.local.MatchLocalCache.TTL_EXTRA_FINISHED_MS
            else -> com.footballai.analyzer.data.local.MatchLocalCache.TTL_EXTRA_OPEN_MS
        }
        cache.putExtra(matchId, extra, ttl)
        return extra
    }

    private fun MatchDto.toDomain(): Match {
        val status = state?.description ?: "Unknown"
        val liveStatuses = listOf(
            "First half", "Second half", "Half time",
            "Extra time", "Penalties", "In progress"
        )
        return Match(
            id = id,
            homeTeamName = homeTeam?.name ?: "Home",
            awayTeamName = awayTeam?.name ?: "Away",
            homeTeamLogo = homeTeam?.logo,
            awayTeamLogo = awayTeam?.logo,
            homeTeamId = homeTeam?.id?.toLong(),
            awayTeamId = awayTeam?.id?.toLong(),
            score = state?.score?.current ?: "- -",
            minute = state?.clock,
            status = status,
            leagueName = league?.name ?: "",
            leagueLogo = league?.logo,
            leagueId = league?.id?.toLong(),
            season = league?.season,
            countryName = country?.name,
            date = date,
            isLive = status in liveStatuses
        )
    }

    private fun MatchDetailResponse.toDomain(): Match {
        val status = state?.description ?: "Unknown"
        val liveStatuses = listOf(
            "First half", "Second half", "Half time",
            "Extra time", "Penalties", "In progress"
        )
        return Match(
            id = id,
            homeTeamName = homeTeam?.name ?: "Home",
            awayTeamName = awayTeam?.name ?: "Away",
            homeTeamLogo = homeTeam?.logo,
            awayTeamLogo = awayTeam?.logo,
            homeTeamId = homeTeam?.id?.toLong(),
            awayTeamId = awayTeam?.id?.toLong(),
            score = state?.score?.current ?: "- -",
            minute = state?.clock,
            status = status,
            leagueName = league?.name ?: "",
            leagueLogo = league?.logo,
            leagueId = league?.id?.toLong(),
            season = league?.season,
            countryName = country?.name,
            date = date,
            isLive = status in liveStatuses
        )
    }


    suspend fun getStandings(leagueId: Long, season: Int): Result<List<com.footballai.analyzer.domain.model.StandingsGroup>> {
        return try {
            val resp = api.getStandings(leagueId, season)
            val groups = resp.groups.map { g ->
                com.footballai.analyzer.domain.model.StandingsGroup(
                    name = g.name ?: "Tabella",
                    rows = g.standings.map { r ->
                        val t = r.total
                        com.footballai.analyzer.domain.model.StandingRow(
                            position = r.position ?: 0,
                            teamId = r.team?.id,
                            teamName = r.team?.name ?: "?",
                            teamLogo = r.team?.logo,
                            played = t?.games ?: 0,
                            wins = t?.wins ?: 0,
                            draws = t?.draws ?: 0,
                            losses = t?.loses ?: 0,
                            goalsFor = t?.scoredGoals ?: 0,
                            goalsAgainst = t?.receivedGoals ?: 0,
                            points = r.points ?: 0
                        )
                    }.sortedBy { it.position }
                )
            }
            Result.success(groups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHeadToHeadData(homeTeamId: Long?, awayTeamId: Long?): com.footballai.analyzer.domain.model.HeadToHeadData {
        if (homeTeamId == null || awayTeamId == null) {
            return com.footballai.analyzer.domain.model.HeadToHeadData()
        }
        fun mapForm(list: List<com.footballai.analyzer.data.remote.dto.FormMatchDto>) =
            list.map {
                com.footballai.analyzer.domain.model.FormMatch(
                    id = it.id ?: 0L,
                    homeName = it.homeTeam?.name ?: "?",
                    awayName = it.awayTeam?.name ?: "?",
                    score = it.state?.score?.current ?: "- -",
                    date = it.date
                )
            }
        val homeForm = try { mapForm(api.getLastFiveGames(homeTeamId)) } catch (_: Exception) { emptyList() }
        val awayForm = try { mapForm(api.getLastFiveGames(awayTeamId)) } catch (_: Exception) { emptyList() }
        val h2h = try { mapForm(api.getHeadToHead(homeTeamId, awayTeamId)) } catch (_: Exception) { emptyList() }
        return com.footballai.analyzer.domain.model.HeadToHeadData(homeForm, awayForm, h2h)
    }
}
