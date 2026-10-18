package com.footballai.analyzer.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LineupsResponse(
    val homeTeam: LineupsTeamDto? = null,
    val awayTeam: LineupsTeamDto? = null
)

@Serializable
data class LineupsTeamDto(
    val id: Long? = null,
    val logo: String? = null,
    val formation: String? = null,
    val name: String? = null,
    val substitutes: List<LineupsPlayerDto> = emptyList(),
    val initialLineup: List<List<LineupsPlayerDto>> = emptyList()
)

@Serializable
data class LineupsPlayerDto(
    val id: Long? = null,
    val name: String? = null,
    val number: Int? = null,
    val position: String? = null
)
