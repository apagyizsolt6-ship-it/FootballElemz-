package com.footballai.analyzer.domain.model

data class StandingRow(
    val position: Int,
    val teamId: Long?,
    val teamName: String,
    val teamLogo: String?,
    val played: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val points: Int
)

data class StandingsGroup(
    val name: String,
    val rows: List<StandingRow>
)

data class FormMatch(
    val id: Long,
    val homeName: String,
    val awayName: String,
    val score: String,
    val date: String?
)

data class HeadToHeadData(
    val formHome: List<FormMatch> = emptyList(),
    val formAway: List<FormMatch> = emptyList(),
    val h2h: List<FormMatch> = emptyList()
)
