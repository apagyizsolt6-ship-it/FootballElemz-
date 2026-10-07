package com.footballai.analyzer.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LineupsResponse(
    val homeTeam: LineupTeamDto? = null,
    val awayTeam: LineupTeamDto? = null
)

@Serializable
data class LineupTeamDto(
    val id: Long? = null,
    val name: String? = null,
    val logo: String? = null,
    val formation: String? = null,
    val initialLineup: List<List<LineupPlayerDto>>? = null,
    val substitutes: List<LineupPlayerDto>? = null
)

@Serializable
data class LineupPlayerDto(
    val id: Long? = null,
    val name: String? = null,
    val number: Int? = null,
    val position: String? = null
)
