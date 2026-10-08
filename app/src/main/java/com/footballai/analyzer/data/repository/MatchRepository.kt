package com.footballai.analyzer.data.repository

import com.footballai.analyzer.data.remote.HighlightlyApi
import com.footballai.analyzer.data.remote.dto.MatchDetailResponse
import com.footballai.analyzer.data.remote.dto.MatchDto
import com.footballai.analyzer.domain.model.Highlight
import com.footballai.analyzer.domain.model.Match
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
            val response = api.getMatches(
                date = dateStr,
                timezone = "Europe/Budapest",
                limit = 100
            )
            Result.success(response.data.map { it.toDomain() })
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
            val resp = api.getStatistics(matchId)
            resp.data.map { team ->
                TeamStat(
                    teamName = team.teamName ?: "Csapat",
                    stats = team.statistics.associate {
                        (it.type ?: "") to (it.value ?: "-")
                    }
                )
            }
        } catch (_: Exception) {
            emptyList()
        }

        val lineups = try {
            val resp = api.getLineups(matchId)
            resp.data.map { team ->
                TeamLineup(
                    teamName = team.teamName ?: "Csapat",
                    formation = team.formation,
                    starters = team.startXI.map {
                        Player(it.name ?: "?", it.number, it.pos)
                    },
                    substitutes = team.substitutes.map {
                        Player(it.name ?: "?", it.number, it.pos)
                    }
                )
            }
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

        return MatchExtraData(stats, lineups, highlights)
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
