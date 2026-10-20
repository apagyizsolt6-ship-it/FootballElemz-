package com.footballai.analyzer.util

import java.text.Collator
import java.util.Locale

object LeagueTranslator {

    private val countryMap = mapOf(
        "England" to "Anglia", "Spain" to "Spanyolország", "Italy" to "Olaszország",
        "Germany" to "Németország", "France" to "Franciaország", "Portugal" to "Portugália",
        "Netherlands" to "Hollandia", "Belgium" to "Belgium", "Turkey" to "Törökország",
        "Scotland" to "Skócia", "Greece" to "Görögország", "Austria" to "Ausztria",
        "Switzerland" to "Svájc", "Poland" to "Lengyelország", "Czech Republic" to "Csehország",
        "Czechia" to "Csehország", "Hungary" to "Magyarország", "Romania" to "Románia",
        "Croatia" to "Horvátország", "Serbia" to "Szerbia", "Ukraine" to "Ukrajna",
        "Russia" to "Oroszország", "Denmark" to "Dánia", "Sweden" to "Svédország",
        "Norway" to "Norvégia", "Finland" to "Finnország", "Ireland" to "Írország",
        "Northern Ireland" to "Észak-Írország", "Wales" to "Wales", "Iceland" to "Izland",
        "Slovakia" to "Szlovákia", "Slovenia" to "Szlovénia", "Bulgaria" to "Bulgária",
        "Bosnia and Herzegovina" to "Bosznia-Hercegovina", "Bosnia" to "Bosznia",
        "Montenegro" to "Montenegró", "Albania" to "Albánia", "North Macedonia" to "Észak-Macedónia",
        "Macedonia" to "Észak-Macedónia", "Kosovo" to "Koszovó", "Israel" to "Izrael",
        "Cyprus" to "Ciprus", "Malta" to "Málta", "Georgia" to "Grúzia",
        "Armenia" to "Örményország", "Azerbaijan" to "Azerbajdzsán", "Kazakhstan" to "Kazahsztán",
        "Uzbekistan" to "Üzbegisztán", "Japan" to "Japán", "South Korea" to "Dél-Korea",
        "Korea Republic" to "Dél-Korea", "China" to "Kína", "Australia" to "Ausztrália",
        "USA" to "USA", "United States" to "USA", "Mexico" to "Mexikó",
        "Brazil" to "Brazília", "Argentina" to "Argentína", "Chile" to "Chile",
        "Colombia" to "Kolumbia", "Uruguay" to "Uruguay", "Paraguay" to "Paraguay",
        "Peru" to "Peru", "Ecuador" to "Ecuador", "Bolivia" to "Bolívia",
        "Venezuela" to "Venezuela", "Saudi Arabia" to "Szaúd-Arábia", "Qatar" to "Katar",
        "UAE" to "Egyesült Arab Emírségek", "United Arab Emirates" to "Egyesült Arab Emírségek",
        "Egypt" to "Egyiptom", "Morocco" to "Marokkó", "Tunisia" to "Tunézia",
        "Algeria" to "Algéria", "South Africa" to "Dél-Afrika", "Nigeria" to "Nigéria",
        "Ghana" to "Ghána", "Cameroon" to "Kamerun", "Senegal" to "Szenegál",
        "Ivory Coast" to "Elefántcsontpart", "Cote d'Ivoire" to "Elefántcsontpart",
        "Lebanon" to "Libanon", "India" to "India", "Thailand" to "Thaiföld",
        "Vietnam" to "Vietnám", "Indonesia" to "Indonézia", "Malaysia" to "Malajzia",
        "Singapore" to "Szingapúr", "New Zealand" to "Új-Zéland", "Canada" to "Kanada",
        "World" to "Világ", "Europe" to "Európa", "International" to "Nemzetközi",
        "Estonia" to "Észtország", "Latvia" to "Lettország", "Lithuania" to "Litvánia",
        "Belarus" to "Fehéroroszország", "Moldova" to "Moldova", "Luxembourg" to "Luxemburg",
        "Ethiopia" to "Etiópia", "Kenya" to "Kenya", "Jamaica" to "Jamaica",
        "Uganda" to "Uganda", "Mauritania" to "Mauritánia", "Kyrgyzstan" to "Kirgizisztán",
        "Congo" to "Kongó", "DR Congo" to "Kongó DR", "Congo DR" to "Kongó DR"
    )

    private val leagueMap = mapOf(
        "Premier League" to "Premier League",
        "La Liga" to "La Liga",
        "Serie A" to "Serie A",
        "Bundesliga" to "Bundesliga",
        "Ligue 1" to "Ligue 1",
        "Champions League" to "Bajnokok Ligája",
        "UEFA Champions League" to "Bajnokok Ligája",
        "Europa League" to "Európa Liga",
        "UEFA Europa League" to "Európa Liga",
        "Conference League" to "Konferencia Liga",
        "UEFA Europa Conference League" to "Konferencia Liga",
        "UEFA Conference League" to "Konferencia Liga",
        "NB I" to "NB I",
        "OTP Bank Liga" to "NB I",
        "Premier League Cup" to "Premier League Cup",
        "EFL Cup" to "Ligakupa",
        "FA Cup" to "FA Kupa"
    )

    /**
     * Top prioritás sorrend:
     * 0 BL, 1 EL, 2 Konferencia, 3 PL, 4 La Liga, 5 Bundesliga, 6 Serie A, 7 Ligue 1
     * Európai kupáknál ország nem kötelező (World / Europe / több ország).
     */
    private data class TopLeagueRule(
        val priority: Int,
        val countries: Set<String>?, // null = bármely ország (UEFA kupa)
        val leagueMustContain: List<String>,
        val leagueMustNotContain: List<String> = emptyList()
    )

    private val topRules = listOf(
        // UEFA kupák – ország független
        TopLeagueRule(
            priority = 0,
            countries = null,
            leagueMustContain = listOf(
                "champions league", "bajnokok ligaja", "uefa champions"
            ),
            leagueMustNotContain = listOf("youth", "u19", "u21", "women", "noi")
        ),
        TopLeagueRule(
            priority = 1,
            countries = null,
            leagueMustContain = listOf(
                "europa league", "europa liga", "uefa europa league"
            ),
            // ne egyezzen a conference-szel
            leagueMustNotContain = listOf("conference", "konferencia", "youth", "u19", "u21")
        ),
        TopLeagueRule(
            priority = 2,
            countries = null,
            leagueMustContain = listOf(
                "conference league", "konferencia liga", "europa conference"
            ),
            leagueMustNotContain = listOf("youth", "u19", "u21")
        ),
        // Hazai top 5 – ország + liga
        TopLeagueRule(
            priority = 3,
            countries = setOf("england", "anglia"),
            leagueMustContain = listOf("premier league"),
            leagueMustNotContain = listOf("cup", "u21", "u23", "2.", "championship", "international")
        ),
        TopLeagueRule(
            priority = 4,
            countries = setOf("spain", "spanyolorszag"),
            leagueMustContain = listOf("la liga", "laliga"),
            leagueMustNotContain = listOf("2", "hypermotion", "primera federacion")
        ),
        TopLeagueRule(
            priority = 5,
            countries = setOf("germany", "nemetorszag"),
            leagueMustContain = listOf("bundesliga"),
            leagueMustNotContain = listOf("2.", "3.", "ii", "iii", "frauen")
        ),
        TopLeagueRule(
            priority = 6,
            countries = setOf("italy", "olaszorszag"),
            leagueMustContain = listOf("serie a"),
            leagueMustNotContain = listOf("serie b", "serie c", "women", "noi")
        ),
        TopLeagueRule(
            priority = 7,
            countries = setOf("france", "franciaorszag"),
            leagueMustContain = listOf("ligue 1"),
            leagueMustNotContain = listOf("ligue 2", "national")
        )
    )

    private val collator: Collator = Collator.getInstance(Locale("hu", "HU")).apply {
        strength = Collator.PRIMARY
    }

    fun translateCountry(name: String?): String {
        if (name.isNullOrBlank()) return ""
        return countryMap[name] ?: name
    }

    fun translateLeague(name: String?): String {
        if (name.isNullOrBlank()) return ""
        return leagueMap[name] ?: name
    }

    fun formatHeader(country: String?, league: String?): String {
        val c = translateCountry(country)
        val l = translateLeague(league)
        return when {
            c.isNotBlank() && l.isNotBlank() -> "$c: $l"
            l.isNotBlank() -> l
            c.isNotBlank() -> c
            else -> "Egyéb"
        }
    }

    private fun normalize(header: String): String {
        return header.lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ö", "o").replace("ő", "o")
            .replace("ú", "u").replace("ü", "u").replace("ű", "u")
    }

    fun topLeaguePriority(header: String): Int {
        val h = normalize(header)
        for (rule in topRules) {
            if (rule.countries != null) {
                val countryOk = rule.countries.any { h.contains(it) }
                if (!countryOk) continue
            }
            val leagueOk = rule.leagueMustContain.any { h.contains(it) }
            if (!leagueOk) continue
            val blocked = rule.leagueMustNotContain.any { h.contains(it) }
            if (blocked) continue
            return rule.priority
        }
        return 100
    }

    fun isTopLeague(header: String): Boolean = topLeaguePriority(header) < 100

    fun compareHeaders(a: String, b: String): Int = collator.compare(a, b)
}
