package com.footballai.analyzer.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TeamStat(
    val teamName: String,
    val stats: Map<String, String>
)

@Serializable
data class Player(
    val name: String,
    val number: Int?,
    val position: String?,
    val id: Long? = null,
    val photoUrl: String? = null
)

@Serializable
data class TeamLineup(
    val teamName: String,
    val teamLogo: String? = null,
    val formation: String?,
    val formationRows: List<List<Player>> = emptyList(),
    val starters: List<Player>,
    val substitutes: List<Player>
)

@Serializable
data class Highlight(
    val title: String,
    val url: String?,
    val imageUrl: String?
)

@Serializable
data class MatchEvent(
    val minute: String,
    val type: String,
    val player: String?,
    val assist: String?,
    val substituted: String?,
    val teamName: String?,
    val teamLogo: String?
)

@Serializable
data class MatchExtraData(
    val statistics: List<TeamStat> = emptyList(),
    val lineups: List<TeamLineup> = emptyList(),
    val highlights: List<Highlight> = emptyList(),
    val events: List<MatchEvent> = emptyList()
)
