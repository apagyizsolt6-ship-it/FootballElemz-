package com.footballai.analyzer.domain.model

data class Match(
    val id: Long,
    val homeTeamName: String,
    val awayTeamName: String,
    val homeTeamLogo: String?,
    val awayTeamLogo: String?,
    val score: String,
    val minute: Int?,
    val status: String,
    val leagueName: String,
    val leagueLogo: String?,
    val date: String?,
    val isLive: Boolean
)
