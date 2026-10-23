package com.footballai.analyzer.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class StandingsResponse(
    val groups: List<StandingsGroupDto> = emptyList()
)

@Serializable
data class StandingsGroupDto(
    val name: String? = null,
    val standings: List<StandingRowDto> = emptyList()
)

@Serializable
data class StandingRowDto(
    val position: Int? = null,
    val points: Int? = null,
    val team: StandingsTeamDto? = null,
    val total: StandingsStatsDto? = null
)

@Serializable
data class StandingsTeamDto(
    val id: Long? = null,
    val logo: String? = null,
    val name: String? = null
)

@Serializable
data class StandingsStatsDto(
    val games: Int? = null,
    val wins: Int? = null,
    val draws: Int? = null,
    val loses: Int? = null,
    val scoredGoals: Int? = null,
    val receivedGoals: Int? = null
)
