package com.footballai.analyzer.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Match(
    val id: Long,
    val homeTeamName: String,
    val awayTeamName: String,
    val homeTeamLogo: String?,
    val awayTeamLogo: String?,
    val homeTeamId: Long? = null,
    val awayTeamId: Long? = null,
    val score: String,
    val minute: Int?,
    val status: String,
    val leagueName: String,
    val leagueLogo: String?,
    val leagueId: Long? = null,
    val season: Int? = null,
    val countryName: String?,
    val date: String?,
    val isLive: Boolean
)
