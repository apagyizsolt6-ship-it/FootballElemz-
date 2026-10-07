package com.footballai.analyzer.data.repository

import com.footballai.analyzer.data.remote.HighlightlyApi
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
            val live = response.data
                .filter { it.state?.description in listOf("First half", "Second half", "Half time", "Extra time", "Penalties", "In progress") }
                .map { it.toDomain() }
            Result.success(live)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMatchById(id: Long): Result<Match> {
        return try {
            val response = api.getMatchById(id)
            Result.success(response.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun MatchDto.toDomain(): Match {
        val status = state?.description ?: "Unknown"
        val isLive = status in listOf("First half", "Second half", "Half time", "Extra time", "Penalties", "In progress")
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

    private fun com.footballai.analyzer.data.remote.dto.MatchDetailResponse.toDomain(): Match {
        val status = state?.description ?: "Unknown"
        val isLive = status in listOf("First half", "Second half", "Half time", "Extra time", "Penalties", "In progress")
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
