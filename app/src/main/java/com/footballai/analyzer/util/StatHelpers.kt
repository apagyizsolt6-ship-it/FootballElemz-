package com.footballai.analyzer.util

import com.footballai.analyzer.domain.model.MatchExtraData
import com.footballai.analyzer.domain.model.MomentumSnapshot

object StatHelpers {

    fun pairStats(
        homeName: String,
        awayName: String,
        extra: MatchExtraData
    ): Pair<Map<String, String>, Map<String, String>> {
        if (extra.statistics.isEmpty()) return emptyMap<String, String>() to emptyMap()
        if (extra.statistics.size == 1) {
            val only = extra.statistics[0]
            return if (only.teamName.contains(homeName, true) || homeName.contains(only.teamName, true))
                only.stats to emptyMap()
            else emptyMap<String, String>() to only.stats
        }
        val a = extra.statistics[0]
        val b = extra.statistics[1]
        val aHome = a.teamName.contains(homeName, true) || homeName.contains(a.teamName, true)
        return if (aHome) a.stats to b.stats else b.stats to a.stats
    }

    fun num(map: Map<String, String>, vararg keys: String): Float? {
        for (k in keys) {
            map.entries.firstOrNull {
                it.key.equals(k, true) || it.key.contains(k, true)
            }?.value?.let { v ->
                v.replace("%", "").replace(",", ".").trim().toFloatOrNull()?.let { return it }
            }
        }
        return null
    }

    fun momentum(homeName: String, awayName: String, extra: MatchExtraData): MomentumSnapshot? {
        val (hs, as_) = pairStats(homeName, awayName, extra)
        if (hs.isEmpty() && as_.isEmpty()) return null
        val possH = num(hs, "Ball Possession", "Possession") ?: 50f
        val possA = num(as_, "Ball Possession", "Possession") ?: 50f
        val shotsH = num(hs, "Total Shots", "Shots") ?: 0f
        val shotsA = num(as_, "Total Shots", "Shots") ?: 0f
        val onH = num(hs, "Shots on Goal", "Shots on Target") ?: 0f
        val onA = num(as_, "Shots on Goal", "Shots on Target") ?: 0f
        val scoreH = possH + shotsH * 3f + onH * 5f
        val scoreA = possA + shotsA * 3f + onA * 5f
        val total = (scoreH + scoreA).coerceAtLeast(1f)
        val homePct = scoreH / total
        val label = when {
            homePct > 0.58f -> "Hazai dominancia"
            homePct < 0.42f -> "Vendég dominancia"
            else -> "Kiegyenlített"
        }
        return MomentumSnapshot(homePct, label)
    }

    fun statBarPairs(
        homeName: String,
        awayName: String,
        extra: MatchExtraData
    ): List<Triple<String, Float, Float>> {
        val (hs, as_) = pairStats(homeName, awayName, extra)
        val keys = listOf(
            Triple("Birtoklás %", listOf("Ball Possession", "Possession"), true),
            Triple("Lövések", listOf("Total Shots", "Shots"), false),
            Triple("Kapura", listOf("Shots on Goal", "Shots on Target"), false),
            Triple("Szögletek", listOf("Corner Kicks", "Corners"), false),
            Triple("Sárga", listOf("Yellow Cards"), false)
        )
        return keys.mapNotNull { (label, keyList, _) ->
            val h = num(hs, *keyList.toTypedArray())
            val a = num(as_, *keyList.toTypedArray())
            if (h == null && a == null) null
            else Triple(label, h ?: 0f, a ?: 0f)
        }
    }
}
