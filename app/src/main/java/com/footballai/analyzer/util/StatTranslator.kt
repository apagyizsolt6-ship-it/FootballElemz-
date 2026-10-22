package com.footballai.analyzer.util

object StatTranslator {

    private val stats = mapOf(
        "Ball Possession" to "Labdabirtoklás",
        "Possession" to "Labdabirtoklás",
        "Total Shots" to "Összes lövés",
        "Shots" to "Lövések",
        "Shots on Goal" to "Kapura lövés",
        "Shots on Target" to "Kapura lövés",
        "Shots off Goal" to "Kapu mellé",
        "Shots off Target" to "Kapu mellé",
        "Blocked Shots" to "Blokkolt lövés",
        "Shots insidebox" to "Lövések a 16-oson belül",
        "Shots outsidebox" to "Lövések a 16-oson kívül",
        "Shots accuracy" to "Lövés pontosság",
        "Corner Kicks" to "Szögletek",
        "Corners" to "Szögletek",
        "Fouls" to "Szabálytalanságok",
        "Offsides" to "Lések",
        "Yellow Cards" to "Sárga lapok",
        "Red Cards" to "Piros lapok",
        "Goalkeeper Saves" to "Kapus védések",
        "Saves" to "Védések",
        "Total passes" to "Összes passz",
        "Passes" to "Passzok",
        "Passes accurate" to "Pontos passz",
        "Accurate Passes" to "Pontos passz",
        "Passes %" to "Passz pontosság",
        "Pass Accuracy" to "Passz pontosság",
        "Expected Goals" to "Várható gól (xG)",
        "xG" to "xG",
        "Expected goals" to "Várható gól (xG)",
        "Attacks" to "Támadások",
        "Dangerous Attacks" to "Veszélyes támadások",
        "Throw-ins" to "Bedobások",
        "Goal Kicks" to "Kapura rúgás",
        "Free Kicks" to "Szabadrúgások",
        "Crosses" to "Beívelések",
        "Tackles" to "Szerelések",
        "Interceptions" to "Labdaszerzések",
        "Duels won" to "Megnyert párharcok",
        "Aerials won" to "Megnyert fejpárbajok"
    )

    private val positions = mapOf(
        "Goalkeeper" to "Kapus",
        "Defender" to "Védő",
        "Midfielder" to "Középpályás",
        "Forward" to "Csatár",
        "Attacker" to "Csatár",
        "Striker" to "Csatár",
        "Winger" to "Szélső",
        "G" to "K",
        "D" to "V",
        "M" to "KP",
        "F" to "CS"
    )

    fun stat(name: String?): String {
        if (name.isNullOrBlank()) return "-"
        stats[name]?.let { return it }
        val lower = name.lowercase()
        stats.entries.firstOrNull { it.key.lowercase() == lower }?.let { return it.value }
        // partial
        stats.entries.firstOrNull { lower.contains(it.key.lowercase()) }?.let { return it.value }
        return name
    }

    fun position(name: String?): String {
        if (name.isNullOrBlank()) return ""
        return positions[name] ?: positions.entries.firstOrNull {
            it.key.equals(name, ignoreCase = true)
        }?.value ?: name
    }
}
