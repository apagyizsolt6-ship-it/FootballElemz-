package com.footballai.analyzer.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class OddsResponse(
    val data: List<OddsItemDto> = emptyList()
)

@Serializable
data class OddsItemDto(
    val bookmaker: String? = null,
    val market: String? = null,
    val home: String? = null,
    val draw: String? = null,
    val away: String? = null,
    val label: String? = null,
    val value: String? = null
)
