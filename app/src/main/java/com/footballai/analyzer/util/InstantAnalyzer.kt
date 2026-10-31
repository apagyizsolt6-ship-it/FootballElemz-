package com.footballai.analyzer.util

import com.footballai.analyzer.domain.model.Match
import com.footballai.analyzer.domain.model.MatchExtraData

/**
 * Azonnali, helyi elemzés – LLM nélkül, statisztikák + állás alapján.
 */
object InstantAnalyzer {

    fun analyze(match: Match, extra: MatchExtraData): String {
        val home = match.homeTeamName
        val away = match.awayTeamName
        val isLive = match.isLive
        val finished = match.status.lowercase().let {
            it.contains("finished") || it == "ft"
        }

        val homeStats = extra.statistics.getOrNull(0)?.stats ?: emptyMap()
        val awayStats = extra.statistics.getOrNull(1)?.stats ?: emptyMap()

        // ha a sorrend nem egyezik a névvel, próbáljuk párosítani
        val (hs, as_) = resolveTeamStats(home, away, extra)

        val possH = num(hs, "Ball Possession", "Possession", "Birtoklás")
        val possA = num(as_, "Ball Possession", "Possession", "Birtoklás")
        val shotsH = num(hs, "Total Shots", "Shots", "Lövések")
        val shotsA = num(as_, "Total Shots", "Shots", "Lövések")
        val onH = num(hs, "Shots on Goal", "Shots on Target", "Kapura lövések")
        val onA = num(as_, "Shots on Goal", "Shots on Target", "Kapura lövések")
        val cornersH = num(hs, "Corner Kicks", "Corners", "Szögletek")
        val cornersA = num(as_, "Corner Kicks", "Corners", "Szögletek")
        val yellowH = num(hs, "Yellow Cards", "Sárga lapok")
        val yellowA = num(as_, "Yellow Cards", "Sárga lapok")

        val pressureHome = listOfNotNull(possH, shotsH, onH).averageOrNull() ?: 50.0
        val pressureAway = listOfNotNull(possA, shotsA, onA).averageOrNull() ?: 50.0

        val (pHome, pDraw, pAway) = oddsHint(pressureHome, pressureAway, match.score, isLive, finished)

        val expGoals = expectedGoals(shotsH, shotsA, onH, onA, isLive, match.minute)
        val expCorners = expectedCorners(cornersH, cornersA, isLive, match.minute, finished)
        val expCards = expectedCards(yellowH, yellowA, isLive, match.minute, finished)

        val sb = StringBuilder()
        sb.appendLine("## Esélyek (1X2)")
        sb.appendLine("Becsült arány: **$home $pHome%** · döntetlen **$pDraw%** · **$away $pAway%**.")
        when {
            finished -> sb.appendLine("A meccs véget ért (${match.score}). Az arányok a lezárt eredményt tükrözik.")
            isLive -> sb.appendLine("Élő állás: ${match.score} (${match.minute ?: "?"}'). A nyomás alapján friss becslés.")
            else -> sb.appendLine("Még nem kezdődött el – forma és tipikus ligaátlag alapján.")
        }
        sb.appendLine()

        sb.appendLine("## Várható gólok")
        sb.appendLine(expGoals)
        sb.appendLine()

        sb.appendLine("## Szögletek")
        sb.appendLine(expCorners)
        sb.appendLine()

        sb.appendLine("## Lapok (sárga/piros)")
        sb.appendLine(expCards)
        sb.appendLine()

        sb.appendLine("## Kulcsmomentumok")
        val keys = mutableListOf<String>()
        if (possH != null && possA != null) {
            keys += if (possH >= possA) "Birtoklás: $home felé billen ($possH%–$possA%)."
            else "Birtoklás: $away felé billen ($possA%–$possH%)."
        }
        if (shotsH != null && shotsA != null) {
            keys += "Lövések: $home $shotsH · $away $shotsA" +
                if (onH != null && onA != null) " (kapura: $onH–$onA)." else "."
        }
        if (cornersH != null && cornersA != null) {
            keys += "Szögletek eddig/összesen: $home $cornersH · $away $cornersA."
        }
        if (keys.isEmpty()) {
            keys += "Részletes statisztika még nem elérhető – az elemzés a megkezdett adatból frissül."
        }
        keys.forEach { sb.appendLine("- $it") }
        sb.appendLine()

        sb.appendLine("## Összegzés / tipp")
        val tip = when {
            finished -> "Végeredmény: ${match.score}."
            pHome >= pAway && pHome >= pDraw -> "Rövid távon a **$home** tűnik esélyesebbnek."
            pAway >= pHome && pAway >= pDraw -> "Rövid távon a **$away** tűnik esélyesebbnek."
            else -> "Szoros meccs, **döntetlen** vagy kevés gól forgatókönyv is él."
        }
        sb.appendLine(tip)
        sb.appendLine()
        sb.appendLine("_Azonnali helyi elemzés · a háttérben részletes LLM futhat._")

        return sb.toString().trim()
    }

    private fun resolveTeamStats(
        home: String,
        away: String,
        extra: MatchExtraData
    ): Pair<Map<String, String>, Map<String, String>> {
        if (extra.statistics.isEmpty()) return emptyMap<String, String>() to emptyMap()
        if (extra.statistics.size == 1) {
            val only = extra.statistics[0]
            return if (only.teamName.contains(home, true) || home.contains(only.teamName, true)) {
                only.stats to emptyMap()
            } else {
                emptyMap<String, String>() to only.stats
            }
        }
        val a = extra.statistics[0]
        val b = extra.statistics[1]
        val aIsHome = a.teamName.contains(home, true) || home.contains(a.teamName, true)
        return if (aIsHome) a.stats to b.stats else b.stats to a.stats
    }

    private fun num(map: Map<String, String>, vararg keys: String): Double? {
        for (k in keys) {
            map.entries.firstOrNull { it.key.equals(k, true) || it.key.contains(k, true) }?.value?.let { v ->
                val cleaned = v.replace("%", "").replace(",", ".").trim()
                cleaned.toDoubleOrNull()?.let { return it }
                // "3 - 5" style skip
            }
        }
        return null
    }

    private fun List<Double>.averageOrNull(): Double? =
        if (isEmpty()) null else sum() / size

    private fun oddsHint(
        pressureHome: Double,
        pressureAway: Double,
        score: String,
        isLive: Boolean,
        finished: Boolean
    ): Triple<Int, Int, Int> {
        if (finished) {
            val parts = score.replace("–", "-").split("-").map { it.trim().toIntOrNull() }
            if (parts.size == 2 && parts[0] != null && parts[1] != null) {
                return when {
                    parts[0]!! > parts[1]!! -> Triple(100, 0, 0)
                    parts[0]!! < parts[1]!! -> Triple(0, 0, 100)
                    else -> Triple(0, 100, 0)
                }
            }
        }
        var h = 33.0
        var d = 28.0
        var a = 39.0
        val diff = pressureHome - pressureAway
        h += diff * 0.25
        a -= diff * 0.25
        if (isLive) {
            val parts = score.replace("–", "-").split("-").map { it.trim().toIntOrNull() ?: 0 }
            if (parts.size == 2) {
                val gd = parts[0] - parts[1]
                h += gd * 8
                a -= gd * 8
                d -= kotlin.math.abs(gd) * 3
            }
        }
        h = h.coerceIn(10.0, 75.0)
        a = a.coerceIn(10.0, 75.0)
        d = d.coerceIn(10.0, 40.0)
        val sum = h + d + a
        return Triple(
            ((h / sum) * 100).toInt(),
            ((d / sum) * 100).toInt(),
            ((a / sum) * 100).toInt()
        )
    }

    private fun expectedGoals(
        shotsH: Double?, shotsA: Double?, onH: Double?, onA: Double?,
        isLive: Boolean, minute: Int?
    ): String {
        val baseH = ((onH ?: (shotsH?.times(0.35))) ?: 1.2)
        val baseA = ((onA ?: (shotsA?.times(0.35))) ?: 1.0)
        var eh = (baseH * 0.9).coerceIn(0.4, 3.5)
        var ea = (baseA * 0.9).coerceIn(0.3, 3.2)
        if (isLive && minute != null && minute in 1..89) {
            val left = (90 - minute) / 90.0
            eh *= (0.4 + left * 0.6)
            ea *= (0.4 + left * 0.6)
        }
        val total = eh + ea
        return "Becsült gólvárakozás: **${"%.1f".format(total)}** összesen " +
            "(hazai ~${"%.1f".format(eh)} · vendég ~${"%.1f".format(ea)})."
    }

    private fun expectedCorners(
        cH: Double?, cA: Double?, isLive: Boolean, minute: Int?, finished: Boolean
    ): String {
        if (finished && cH != null && cA != null) {
            return "Végső szögletek: hazai **${cH.toInt()}** · vendég **${cA.toInt()}** (összesen ${(cH + cA).toInt()})."
        }
        var h = cH ?: 5.0
        var a = cA ?: 4.5
        if (!isLive && cH == null) {
            h = 5.2; a = 4.6
        }
        if (isLive && minute != null && minute in 1..89 && cH != null && cA != null) {
            val pace = (cH + cA) / minute.coerceAtLeast(1)
            val rest = pace * (90 - minute)
            return "Eddig: ${cH.toInt()}–${cA.toInt()}. Tempó alapján várható összesen **${((cH + cA + rest).toInt()).coerceIn(6, 18)}** szöglet."
        }
        return "Várható szögletek: összesen **${(h + a).toInt().coerceIn(7, 14)}** " +
            "(hazai ~${h.toInt()} · vendég ~${a.toInt()})."
    }

    private fun expectedCards(
        yH: Double?, yA: Double?, isLive: Boolean, minute: Int?, finished: Boolean
    ): String {
        if (finished && yH != null && yA != null) {
            return "Sárga lapok: **${yH.toInt()}** – **${yA.toInt()}**. Piros: a rendelkezésre álló adat szerint."
        }
        val base = 3.5
        val total = when {
            yH != null && yA != null && isLive && minute != null -> {
                val pace = (yH + yA) / minute.coerceAtLeast(1)
                (yH + yA + pace * (90 - minute)).coerceIn(2.0, 8.0)
            }
            else -> base
        }
        return "Várható sárga lapok: kb. **${total.toInt().coerceIn(2, 7)}** összesen. " +
            "Piros lap kockázata: **alacsony–közepes** (ligaátlag)."
    }
}
