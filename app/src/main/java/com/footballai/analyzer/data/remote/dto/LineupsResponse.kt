package com.footballai.analyzer.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LineupsResponse(
    val data: List<TeamLineupDto> = emptyList()
)

@Serializable
data class TeamLineupDto(
    val teamId: Int? = null,
    val teamName: String? = null,
    val formation: String? = null,
    val startXI: List<PlayerDto> = emptyList(),
    val substitutes: List<PlayerDto> = emptyList()
)

@Serializable
data class PlayerDto(
    val id: Int? = null,
    val name: String? = null,
    val number: Int? = null,
    val pos: String? = null,
    val grid: String? = null
)
