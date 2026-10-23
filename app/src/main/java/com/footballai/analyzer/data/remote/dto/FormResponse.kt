package com.footballai.analyzer.data.remote.dto

import kotlinx.serialization.Serializable

/** last-five-games és head-2-head válasz elemei (egyszerűsített) */
@Serializable
data class FormMatchDto(
    val id: Long? = null,
    val date: String? = null,
    val homeTeam: TeamDto? = null,
    val awayTeam: TeamDto? = null,
    val state: MatchStateDto? = null
)
