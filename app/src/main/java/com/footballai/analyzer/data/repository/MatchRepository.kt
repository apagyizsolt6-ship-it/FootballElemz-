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
    private val api: HighlightlyApi
) {

    suspend fun getMatchesForDate(date: LocalDate): Result<List<Match>> {
        return try {
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val allMatches = mutableListOf<MatchDto>()
            var offset = 0
            val pageSize = 100
            val maxPages = 10

            repeat(maxPages) {
                val response = api.getMatches(
                    date = dateStr,
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

            Result.success(allMatches.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMatchById(id: Long): Result<Match> {
        return try {
            val response = api.getMatchById(id)
            val matchDto = response.firstOrNull()
                ?: return Result.failure(Exception("Meccs nem található"))
            Result.success(matchDto.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMatchExtraData(matchId: Long): MatchExtraData {
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

        return MatchExtraData(stats, lineups, highlights, events)
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
            score = state?.score?.current ?: "- -",
            minute = state?.clock,
            status = status,
            leagueName = league?.name ?: "",
            leagueLogo = league?.logo,
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
            score = state?.score?.current ?: "- -",
            minute = state?.clock,
            status = status,
            leagueName = league?.name ?: "",
            leagueLogo = league?.logo,
            countryName = country?.name,
            date = date,
            isLive = status in liveStatuses
        )
    }
}
