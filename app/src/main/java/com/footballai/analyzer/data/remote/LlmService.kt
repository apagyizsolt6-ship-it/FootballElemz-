package com.footballai.analyzer.data.remote

import com.footballai.analyzer.data.local.ApiKeyProvider
import com.footballai.analyzer.domain.model.Match
import com.footballai.analyzer.domain.model.MatchExtraData
import com.footballai.analyzer.util.LeagueTranslator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.65
)

@Serializable
data class ChatChoice(val message: ChatMessage)

@Serializable
data class ChatResponse(val choices: List<ChatChoice> = emptyList())

@Singleton
class LlmService @Inject constructor(
    private val apiKeyProvider: ApiKeyProvider
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeMatch(match: Match, extra: MatchExtraData): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val apiKey = apiKeyProvider.getLlmKey().trim()
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(
                        Exception("Nincs LLM API kulcs. Állítsd be a Beállításokban.")
                    )
                }

                var baseUrl = apiKeyProvider.getLlmBaseUrl().trim()
                if (baseUrl.isBlank() || (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://"))) {
                    baseUrl = "https://api.x.ai/v1/"
                }
                if (!baseUrl.endsWith("/")) baseUrl += "/"

                var model = apiKeyProvider.getLlmModel().trim()
                if (model.isBlank()) model = "grok-4.7"

                val isTop = LeagueTranslator.isTopLeague(
                    LeagueTranslator.formatHeader(match.countryName, match.leagueName)
                )
                val prompt = buildPrompt(match, extra, isTop)
                val systemPrompt = buildSystemPrompt(isTop)

                val body = ChatRequest(
                    model = model,
                    messages = listOf(
                        ChatMessage(role = "system", content = systemPrompt),
                        ChatMessage(role = "user", content = prompt)
                    )
                )

                val requestJson = json.encodeToString(ChatRequest.serializer(), body)
                val requestBody = requestJson.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(baseUrl + "chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("LLM hiba (" + response.code + "): " + responseBody.take(300))
                    )
                }

                val parsed = json.decodeFromString(ChatResponse.serializer(), responseBody)
                val text = parsed.choices.firstOrNull()?.message?.content
                if (text.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("Üres LLM válasz"))
                }

                Result.success(text)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun buildSystemPrompt(isTopLeague: Boolean): String {
        val base = """
Te egy profi labdarúgó-elemző vagy. Magyarul írj, szakszerűen, konkrétan.

Kötelező struktúra (címekkel):
1) HELYZETÉRTÉKELÉS – állás, perc, státusz, meccs fázisa
2) TAKTIKAI KÉP – stílus, nyomás, átmenetek, gyenge pontok
3) STATISZTIKAI OLVASAT – ha van adat: birtoklás, lövések, következtetés
4) KULCSPONTOK – 3–5 konkrét megfigyelés
5) ELŐREJELZÉS – legvalószínűbb forgatókönyv + gólvárakozás
""".trimIndent()

        val cornersCards = if (isTopLeague) {
            """

6) VÁRHATÓ SZÖGLETEK (Top liga)
   - Becsüld a teljes meccs szögletszámát (összesen, pl. 8–11)
   - Hazai / vendég bontás (pl. 5–6 / 3–4)
   - Indoklás: játékstílus, szélsők, nyomás, standard helyzetek

7) VÁRHATÓ SÁRGA / PIROS LAPOK (Top liga)
   - Várható sárga lapok száma összesen (pl. 3–5)
   - Hazai / vendég bontás
   - Piros lap kockázata (alacsony / közepes / magas) + indok

A 6–7. pont legyen számszerű és indokolt.
""".trimIndent()
        } else {
            """

6) EXTRA MEGJEGYZÉS
   - Ha van adat szögletekről / lapokról, röviden értelmezd
   - Ha nincs, ne találj ki pontos számokat kisebb ligákra
""".trimIndent()
        }

        return base + cornersCards + "\n\nNe írj marketing-stílusú bevezetőt. Legyél tömör, de részletes (kb. 500–800 szó)."
    }

    private fun buildPrompt(match: Match, extra: MatchExtraData, isTopLeague: Boolean): String {
        val sb = StringBuilder()
        sb.appendLine("Elemezd a következő focimeccset:")
        sb.appendLine()
        sb.appendLine("Hazai: " + match.homeTeamName)
        sb.appendLine("Vendég: " + match.awayTeamName)
        sb.appendLine("Liga: " + match.leagueName)
        sb.appendLine("Ország: " + (match.countryName ?: "-"))
        sb.appendLine("Állás: " + match.score)
        sb.appendLine("Státusz: " + match.status)
        if (match.isLive) {
            sb.appendLine("Perc: " + (match.minute?.toString() ?: "?") + "'")
        }
        sb.appendLine("Dátum: " + (match.date ?: "-"))
        sb.appendLine("Top liga: " + if (isTopLeague) "IGEN" else "nem")
        sb.appendLine()

        if (extra.statistics.isNotEmpty()) {
            sb.appendLine("STATISZTIKÁK:")
            for (team in extra.statistics) {
                sb.appendLine(team.teamName + ":")
                for ((k, v) in team.stats) {
                    sb.appendLine("  - " + k + ": " + v)
                }
            }
            sb.appendLine()
        } else {
            sb.appendLine("Statisztika: még nem elérhető.")
            sb.appendLine()
        }

        if (extra.lineups.isNotEmpty()) {
            sb.appendLine("FELÁLLÁSOK:")
            for (team in extra.lineups) {
                val formation = team.formation ?: "?"
                sb.appendLine(team.teamName + " (" + formation + "):")
                val starters = team.starters.joinToString(", ") { p -> p.name }
                sb.appendLine("  Kezdő: " + starters)
                if (team.substitutes.isNotEmpty()) {
                    val subs = team.substitutes.joinToString(", ") { p -> p.name }
                    sb.appendLine("  Pad: " + subs)
                }
            }
            sb.appendLine()
        }

        if (extra.highlights.isNotEmpty()) {
            val titles = extra.highlights.joinToString(", ") { h -> h.title }
            sb.appendLine("HIGHLIGHTOK: " + titles)
            sb.appendLine()
        }

        if (isTopLeague) {
            sb.appendLine("KÜLÖN KÉRÉS (Top liga):")
            sb.appendLine("- Számszerű becslés: várható összes szöglet + hazai/vendég bontás.")
            sb.appendLine("- Számszerű becslés: várható sárga lapok + piros kockázat.")
            sb.appendLine()
        }

        sb.appendLine("Írj strukturált, magyar szakértői elemzést a megadott pontok szerint.")
        return sb.toString()
    }
}
