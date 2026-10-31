package com.footballai.analyzer.domain.model

data class OddsRow(
    val bookmaker: String,
    val home: String?,
    val draw: String?,
    val away: String?
)

data class MomentumSnapshot(
    val homePercent: Float, // 0f..1f
    val label: String
)
