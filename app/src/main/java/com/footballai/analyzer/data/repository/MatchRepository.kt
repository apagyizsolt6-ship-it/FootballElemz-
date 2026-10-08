package com.footballai.analyzer.data.repository

import com.footballai.analyzer.data.remote.HighlightlyApi
import com.footballai.analyzer.data.remote.dto.MatchDetailResponse
import com.footballai.analyzer.data.remote.dto.MatchDto
import com.footballai.analyzer.domain.model.Match
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchRepository @Inject constructor(
    private val api: HighlightlyApi
) {

    suspend fun getTodayMatches(): Result<List<Match>> {
        return try {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val response = api.getMatches(date = today, timezone = "Europe/Budapest")
            val matches = response.data.map { it.toDomain() }
            Result.success(matches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLiveMatches(): Result<List<Match>> {
        return try {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val response = api.getMatches(date = today, timezone = "Europe/Budapest")
            val liveStatuses = listOf(
                "First half", "Second half", "Half time",
                "Extra time", "Penalties", "In progress"
            )
            val live = response.data
                .filter { it.state?.description in liveStatuses }
                .map { it.toDomain() }
            Result.success(live)
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

    private fun MatchDto.toDomain(): Match {
        val status = state?.description ?: "Unknown"
        val liveStatuses = listOf(
            "First half", "Second half", "Half time",
            "Extra time", "Penalties", "In progress"
        )
        val isLive = status in liveStatuses
        return Match(
            id = id,
            homeTeamName = homeTeam?.name ?: "Home",
            awayTeamName = awayTeam?.name ?: "Away",
            homeTeamLogo = homeTeam?.logo,
            awayTeamLogo = awayTeam?.logo,
            score = state?.score?.current ?: "0 - 0",
            minute = state?.clock,
            status = status,
            leagueName = league?.name ?: "",
            leagueLogo = league?.logo,
            date = date,
            isLive = isLive
        )
    }

    private fun MatchDetailResponse.toDomain(): Match {
        val status = state?.description ?: "Unknown"
        val liveStatuses = listOf(
            "First half", "Second half", "Half time",
            "Extra time", "Penalties", "In progress"
        )
        val isLive = status in liveStatuses
        return Match(
            id = id,
            homeTeamName = homeTeam?.name ?: "Home",
            awayTeamName = awayTeam?.name ?: "Away",
            homeTeamLogo = homeTeam?.logo,
            awayTeamLogo = awayTeam?.logo,
            score = state?.score?.current ?: "0 - 0",
            minute = state?.clock,
            status = status,
            leagueName = league?.name ?: "",
            leagueLogo = league?.logo,
            date = date,
            isLive = isLive
        )
    }
}
