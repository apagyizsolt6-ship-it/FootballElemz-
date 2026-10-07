package com.footballai.analyzer.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class HighlightsResponse(
    val data: List<HighlightDto> = emptyList(),
    val pagination: PaginationDto? = null
)

@Serializable
data class HighlightDto(
    val id: Long? = null,
    val title: String? = null,
    val url: String? = null,
    val type: String? = null,
    val matchId: Long? = null,
    val imgUrl: String? = null
)
