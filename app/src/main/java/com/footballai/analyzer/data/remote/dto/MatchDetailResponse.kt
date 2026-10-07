package com.footballai.analyzer.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MatchDetailResponse(
    val id: Long,
    val round: String? = null,
    val date: String? = null,
    val country: CountryDto? = null,
    val homeTeam: TeamDto? = null,
    val awayTeam: TeamDto? = null,
    val league: LeagueDto? = null,
    val state: MatchStateDto? = null
    // Később bővíthető predictions, events stb. mezőkkel
)
