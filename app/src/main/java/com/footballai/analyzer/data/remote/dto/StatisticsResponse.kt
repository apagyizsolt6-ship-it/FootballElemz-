package com.footballai.analyzer.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class StatisticsResponse(
    val data: List<TeamStatisticsDto> = emptyList()
)

@Serializable
data class TeamStatisticsDto(
    val teamId: Int? = null,
    val teamName: String? = null,
    val statistics: List<StatItemDto> = emptyList()
)

@Serializable
data class StatItemDto(
    val type: String? = null,
    val value: String? = null
)
