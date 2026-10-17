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
        "Belarus" to "Fehéroroszország", "Moldova" to "Moldova", "Luxembourg" to "Luxemburg"
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
        "NB I" to "NB I",
        "OTP Bank Liga" to "NB I",
        "Veikkausliiga" to "Veikkausliiga",
        "First League" to "Első Liga"
    )

    // 1 PL, 2 La Liga, 3 Bundesliga, 4 Serie A, 5 Ligue 1
    private val topLeagueKeys = listOf(
        "premier league",
        "la liga",
        "bundesliga",
        "serie a",
        "ligue 1"
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

    fun topLeaguePriority(header: String): Int {
        val h = header.lowercase()
        topLeagueKeys.forEachIndexed { index, key ->
            if (h.contains(key)) return index
        }
        return 100
    }

    fun isTopLeague(header: String): Boolean = topLeaguePriority(header) < 100

    /** Magyar ABC – É, Ö, Ü a helyükön */
    fun compareHeaders(a: String, b: String): Int = collator.compare(a, b)
}
