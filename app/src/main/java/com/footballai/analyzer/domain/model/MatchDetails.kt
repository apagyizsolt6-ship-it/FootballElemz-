package com.footballai.analyzer.domain.model

data class TeamStat(
    val teamName: String,
    val stats: Map<String, String>
)

data class Player(
    val name: String,
    val number: Int?,
    val position: String?
)

data class TeamLineup(
    val teamName: String,
    val formation: String?,
    val starters: List<Player>,
    val substitutes: List<Player>
)

data class Highlight(
    val title: String,
    val url: String?,
    val imageUrl: String?
)

data class MatchExtraData(
    val statistics: List<TeamStat> = emptyList(),
    val lineups: List<TeamLineup> = emptyList(),
    val highlights: List<Highlight> = emptyList()
)
