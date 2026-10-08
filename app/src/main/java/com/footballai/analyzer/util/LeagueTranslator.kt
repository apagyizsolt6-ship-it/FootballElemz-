package com.footballai.analyzer.util

object LeagueTranslator {

    private val countryMap = mapOf(
        "England" to "Anglia",
        "Spain" to "Spanyolország",
        "Italy" to "Olaszország",
        "Germany" to "Németország",
        "France" to "Franciaország",
        "Hungary" to "Magyarország",
        "Portugal" to "Portugália",
        "Netherlands" to "Hollandia",
        "Belgium" to "Belgium",
        "Turkey" to "Törökország",
        "Brazil" to "Brazília",
        "Argentina" to "Argentína",
        "Mexico" to "Mexikó",
        "USA" to "USA",
        "United States" to "USA",
        "Japan" to "Japán",
        "South Korea" to "Dél-Korea",
        "China" to "Kína",
        "Australia" to "Ausztrália",
        "Morocco" to "Marokkó",
        "Egypt" to "Egyiptom",
        "Saudi Arabia" to "Szaúd-Arábia",
        "UAE" to "Egyesült Arab Emírségek",
        "Qatar" to "Katar",
        "Azerbaijan" to "Azerbajdzsán",
        "Romania" to "Románia",
        "Poland" to "Lengyelország",
        "Czech Republic" to "Csehország",
        "Austria" to "Ausztria",
        "Switzerland" to "Svájc",
        "Croatia" to "Horvátország",
        "Serbia" to "Szerbia",
        "Greece" to "Görögország",
        "Scotland" to "Skócia",
        "Denmark" to "Dánia",
        "Sweden" to "Svédország",
        "Norway" to "Norvégia",
        "Ukraine" to "Ukrajna",
        "Russia" to "Oroszország",
        "Israel" to "Izrael",
        "Iraq" to "Irak",
        "Iran" to "Irán",
        "India" to "India",
        "Vietnam" to "Vietnám",
        "Thailand" to "Thaiföld",
        "Indonesia" to "Indonézia",
        "Malaysia" to "Malajzia",
        "Singapore" to "Szingapúr",
        "Bolivia" to "Bolívia",
        "Chile" to "Chile",
        "Colombia" to "Kolumbia",
        "Peru" to "Peru",
        "Ecuador" to "Ecuador",
        "Uruguay" to "Uruguay",
        "Paraguay" to "Paraguay",
        "World" to "Világ",
        "Europe" to "Európa",
        "South America" to "Dél-Amerika",
        "Africa" to "Afrika",
        "Asia" to "Ázsia"
    )

    private val leagueMap = mapOf(
        "Premier League" to "Premier League",
        "La Liga" to "La Liga",
        "LaLiga" to "La Liga",
        "Serie A" to "Serie A",
        "Bundesliga" to "Bundesliga",
        "Ligue 1" to "Ligue 1",
        "Eredivisie" to "Eredivisie",
        "Primeira Liga" to "Primeira Liga",
        "Süper Lig" to "Süper Lig",
        "Championship" to "Championship",
        "Liga Portugal" to "Liga Portugal",
        "NB I" to "NB I",
        "OTP Bank Liga" to "NB I",
        "NB II" to "NB II",
        "Magyar Kupa" to "Magyar Kupa",
        "UEFA Champions League" to "Bajnokok Ligája",
        "Champions League" to "Bajnokok Ligája",
        "UEFA Europa League" to "Európa-liga",
        "Europa League" to "Európa-liga",
        "UEFA Conference League" to "Konferencia Liga",
        "Conference League" to "Konferencia Liga",
        "FIFA Club World Cup" to "Klubvilágbajnokság",
        "World Cup" to "Világbajnokság",
        "European Championship" to "Európa-bajnokság",
        "Botola Pro" to "Botola Pro",
        "Copa do Brasil" to "Brazil Kupa",
        "Copa do Brasil U20" to "Brazil Kupa U20",
        "Copa Libertadores" to "Copa Libertadores",
        "Copa Sudamericana" to "Copa Sudamericana",
        "AFC Champions League" to "AFC Bajnokok Ligája",
        "ASEAN Club Championship" to "ASEAN Klubbajnokság",
        "Birinci Dəstə" to "Azerbajdzsán 1. liga",
        "Copa de la División Profesional" to "Bolíviai Profi Kupa",
        "Iraqi League" to "Iraki Liga",
        "Liga I" to "Román Liga I",
        "Liga Alef" to "Izraeli Liga Alef",
        "Cup" to "Kupa"
    )

    fun translateCountry(name: String?): String {
        if (name.isNullOrBlank()) return ""
        return countryMap[name] ?: name
    }

    fun translateLeague(name: String?): String {
        if (name.isNullOrBlank()) return "Egyéb"
        return leagueMap[name] ?: name
    }

    fun formatHeader(country: String?, league: String?): String {
        val c = translateCountry(country)
        val l = translateLeague(league)
        return if (c.isNotBlank()) {
            "${c.uppercase()}: $l"
        } else {
            l.uppercase()
        }
    }
}
