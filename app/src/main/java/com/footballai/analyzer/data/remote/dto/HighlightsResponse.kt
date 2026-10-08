package com.footballai.analyzer.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class HighlightsResponse(
    val data: List<HighlightDto> = emptyList()
)

@Serializable
data class HighlightDto(
    val id: Long? = null,
    val title: String? = null,
    val url: String? = null,
    val imgUrl: String? = null,
    val type: String? = null
)
