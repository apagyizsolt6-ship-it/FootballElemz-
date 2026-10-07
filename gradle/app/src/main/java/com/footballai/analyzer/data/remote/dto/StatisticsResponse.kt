package com.footballai.analyzer.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class StatisticsResponse(
    val data: List<TeamStatisticsDto> = emptyList()
)

@Serializable
data class TeamStatisticsDto(
    val team: TeamDto? = null,
    val statistics: Map<String, String>? = null
)
