package com.footballai.analyzer.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

@Serializable
data class TeamStatisticsDto(
    val team: StatTeamDto? = null,
    val statistics: List<StatItemDto> = emptyList()
)

@Serializable
data class StatTeamDto(
    val id: Long? = null,
    val logo: String? = null,
    val name: String? = null,
    val type: String? = null
)

@Serializable
data class StatItemDto(
    val displayName: String? = null,
    val value: JsonElement? = null
) {
    fun valueAsString(): String {
        val v = value ?: return "-"
        if (v is JsonPrimitive) {
            val d = v.doubleOrNull
            if (d != null) {
                // 0.62 -> 62% ha accuracy jellegű, egyebkent szam
                return if (d in 0.0..1.0 && (displayName?.contains("accuracy", true) == true ||
                        displayName?.contains("possession", true) == true)
                ) {
                    ((d * 100).toInt()).toString() + "%"
                } else if (d == d.toLong().toDouble()) {
                    d.toLong().toString()
                } else {
                    String.format("%.1f", d)
                }
            }
            return v.contentOrNull ?: "-"
        }
        return v.toString()
    }
}
