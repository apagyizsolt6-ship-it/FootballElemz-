package com.footballai.analyzer.util

object LeagueTranslator {

    private val countryMap = mapOf(
        "England" to "Anglia", "Spain" to "Spanyolország", "Italy" to "Olaszország",
        "Germany" to "Németország", "France" to "Franciaország", "Hungary" to "Magyarország",
        "Portugal" to "Portugália", "Netherlands" to "Hollandia", "Belgium" to "Belgium",
        "Turkey" to "Törökország", "Poland" to "Lengyelország", "Czech Republic" to "Csehország",
        "Czechia" to "Csehország", "Austria" to "Ausztria", "Switzerland" to "Svájc",
        "Croatia" to "Horvátország", "Serbia" to "Szerbia", "Greece" to "Görögország",
        "Scotland" to "Skócia", "Denmark" to "Dánia", "Sweden" to "Svédország",
        "Norway" to "Norvégia", "Ukraine" to "Ukrajna", "Russia" to "Oroszország",
        "Romania" to "Románia", "Bulgaria" to "Bulgária", "Slovakia" to "Szlovákia",
        "Slovenia" to "Szlovénia", "Bosnia and Herzegovina" to "Bosznia-Hercegovina",
        "Bosnia" to "Bosznia-Hercegovina", "North Macedonia" to "Észak-Macedónia",
        "Macedonia" to "Észak-Macedónia", "Albania" to "Albánia", "Montenegro" to "Montenegró",
        "Kosovo" to "Koszovó", "Finland" to "Finnország", "Iceland" to "Izland",
        "Ireland" to "Írország", "Northern Ireland" to "Észak-Írország", "Wales" to "Wales",
        "Belarus" to "Fehéroroszország", "Moldova" to "Moldova", "Lithuania" to "Litvánia",
        "Latvia" to "Lettország", "Estonia" to "Észtország", "Luxembourg" to "Luxemburg",
        "Malta" to "Málta", "Cyprus" to "Ciprus", "Georgia" to "Grúzia",
        "Armenia" to "Örményország", "Azerbaijan" to "Azerbajdzsán",
        "Japan" to "Japán", "South Korea" to "Dél-Korea", "Korea Republic" to "Dél-Korea",
        "China" to "Kína", "India" to "India", "Iran" to "Irán", "Iraq" to "Irak",
        "Saudi Arabia" to "Szaúd-Arábia", "UAE" to "Egyesült Arab Emírségek",
        "United Arab Emirates" to "Egyesült Arab Emírségek", "Qatar" to "Katar",
        "Kuwait" to "Kuvait", "Bahrain" to "Bahrein", "Oman" to "Omán",
        "Jordan" to "Jordánia", "Lebanon" to "Libanon", "Syria" to "Szíria",
        "Israel" to "Izrael", "Vietnam" to "Vietnám", "Thailand" to "Thaiföld",
        "Indonesia" to "Indonézia", "Malaysia" to "Malajzia", "Singapore" to "Szingapúr",
        "Philippines" to "Fülöp-szigetek", "Uzbekistan" to "Üzbegisztán",
        "Kazakhstan" to "Kazahsztán", "Kyrgyzstan" to "Kirgizisztán",
        "Bangladesh" to "Banglades", "Bhutan" to "Bhután", "Hong Kong" to "Hongkong",
        "Egypt" to "Egyiptom", "Morocco" to "Marokkó", "Algeria" to "Algéria",
        "Tunisia" to "Tunézia", "South Africa" to "Dél-Afrika", "Nigeria" to "Nigéria",
        "Ghana" to "Ghána", "Cameroon" to "Kamerun", "Senegal" to "Szenegál",
        "Ivory Coast" to "Elefántcsontpart", "Zimbabwe" to "Zimbabwe", "Kenya" to "Kenya",
        "USA" to "USA", "United States" to "USA", "Canada" to "Kanada", "Mexico" to "Mexikó",
        "Costa Rica" to "Costa Rica", "Jamaica" to "Jamaica",
        "Brazil" to "Brazília", "Argentina" to "Argentína", "Uruguay" to "Uruguay",
        "Paraguay" to "Paraguay", "Chile" to "Chile", "Colombia" to "Kolumbia",
        "Peru" to "Peru", "Ecuador" to "Ecuador", "Bolivia" to "Bolívia",
        "Venezuela" to "Venezuela", "Australia" to "Ausztrália", "New Zealand" to "Új-Zéland",
        "World" to "Világ", "Europe" to "Európa", "South America" to "Dél-Amerika",
        "Africa" to "Afrika", "Asia" to "Ázsia", "International" to "Nemzetközi"
    )

    private val leagueMap = mapOf(
        "Premier League" to "Premier League", "La Liga" to "La Liga", "LaLiga" to "La Liga",
        "Serie A" to "Serie A", "Bundesliga" to "Bundesliga", "Ligue 1" to "Ligue 1",
        "Eredivisie" to "Eredivisie", "Primeira Liga" to "Primeira Liga",
        "Süper Lig" to "Süper Lig", "Championship" to "Championship",
        "NB I" to "NB I", "OTP Bank Liga" to "NB I", "NB II" to "NB II",
        "Magyar Kupa" to "Magyar Kupa",
        "UEFA Champions League" to "Bajnokok Ligája", "Champions League" to "Bajnokok Ligája",
        "UEFA Europa League" to "Európa-liga", "Europa League" to "Európa-liga",
        "UEFA Conference League" to "Konferencia Liga", "Conference League" to "Konferencia Liga",
        "FIFA Club World Cup" to "Klubvilágbajnokság", "World Cup" to "Világbajnokság",
        "European Championship" to "Európa-bajnokság",
        "Copa Libertadores" to "Copa Libertadores", "Copa Sudamericana" to "Copa Sudamericana",
        "Copa do Brasil" to "Brazil Kupa", "Copa do Brasil U20" to "Brazil Kupa U20",
        "Primera División" to "Primera División", "Segunda División" to "Segunda División",
        "Primera B" to "Primera B",
        "Copa de la División Profesional" to "Bolíviai Profi Kupa",
        "Botola Pro" to "Botola Pro", "Iraqi League" to "Iraki Liga",
        "Persian Gulf Pro League" to "Perzsa-öböl Pro League",
        "AFC Champions League" to "AFC Bajnokok Ligája",
        "ASEAN Club Championship" to "ASEAN Klubbajnokság",
        "Birinci Dəstə" to "Azerbajdzsán 1. liga", "Liga I" to "Román Liga I",
        "Liga Alef" to "Izraeli Liga Alef", "Stars League" to "Stars League",
        "V.League 1" to "V.League 1", "Pro League A" to "Pro League A",
        "League Cup" to "Ligakupa", "Premier Soccer League" to "Premier Soccer League",
        "First League" to "Első liga", "Premier Division" to "Premier Division",
        "Division 1" to "Division 1", "IFA Shield" to "IFA Shield",
        "Veikkausliiga" to "Veikkausliiga", "Cup" to "Kupa",
        "Friendlies" to "Barátságos", "Friendlies Women" to "Női barátságos",
        "League" to "Liga"
    )

    fun translateCountry(name: String?): String {
        if (name.isNullOrBlank()) return ""
        return countryMap[name.trim()] ?: name.trim()
    }

    fun translateLeague(name: String?): String {
        if (name.isNullOrBlank()) return "Egyéb"
        val trimmed = name.trim()
        return leagueMap[trimmed] ?: trimmed
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
