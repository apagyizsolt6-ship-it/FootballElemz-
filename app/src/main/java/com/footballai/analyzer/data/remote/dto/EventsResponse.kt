package com.footballai.analyzer.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LiveEventDto(
    val team: LiveEventTeamDto? = null,
    val time: String? = null,
    val type: String? = null,
    val playerId: Long? = null,
    val player: String? = null,
    val assistingPlayerId: Long? = null,
    val assist: String? = null,
    val substituted: String? = null
)

@Serializable
data class LiveEventTeamDto(
    val id: Long? = null,
    val logo: String? = null,
    val name: String? = null
)
