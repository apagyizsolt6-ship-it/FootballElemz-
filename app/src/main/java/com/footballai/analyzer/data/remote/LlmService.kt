package com.footballai.analyzer.data.remote

import com.footballai.analyzer.data.local.ApiKeyProvider
import com.footballai.analyzer.domain.model.Match
import com.footballai.analyzer.domain.model.MatchExtraData
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
    val temperature: Double = 0.7
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
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeMatch(match: Match, extra: MatchExtraData): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val apiKey = apiKeyProvider.getLlmKey()
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(
                        Exception("Nincs LLM API kulcs. Allitsd be a Beallitasokban.")
                    )
                }

                var baseUrl = apiKeyProvider.getLlmBaseUrl()
                if (!baseUrl.endsWith("/")) baseUrl += "/"
                val model = apiKeyProvider.getLlmModel()

                val prompt = buildPrompt(match, extra)

                val body = ChatRequest(
                    model = model,
                    messages = listOf(
                        ChatMessage(
                            role = "system",
                            content = "Te egy profi labdarugo-elemzo vagy. Magyarul irj. " +
                                "Adj reszletes, szakertori meccselemzest: helyzetertékelés, taktika, " +
                                "kulcsjatekosok, statisztikai olvasat, elorejelzes. " +
                                "Legyel konkret es szakszeru, ne sablonos."
                        ),
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
                        Exception("LLM hiba (" + response.code + "): " + responseBody.take(200))
                    )
                }

                val parsed = json.decodeFromString(ChatResponse.serializer(), responseBody)
                val text = parsed.choices.firstOrNull()?.message?.content
                if (text == null) {
                    return@withContext Result.failure(Exception("Ures LLM valasz"))
                }

                Result.success(text)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun buildPrompt(match: Match, extra: MatchExtraData): String {
        val sb = StringBuilder()
        sb.appendLine("Elemezd a kovetkezo focimeccset reszletesen:")
        sb.appendLine()
        sb.appendLine("Hazai: " + match.homeTeamName)
        sb.appendLine("Vendeg: " + match.awayTeamName)
        sb.appendLine("Liga: " + match.leagueName)
        sb.appendLine("Orszag: " + (match.countryName ?: "-"))
        sb.appendLine("Allas: " + match.score)
        sb.appendLine("Statusz: " + match.status)
        if (match.isLive) {
            sb.appendLine("Perc: " + (match.minute?.toString() ?: "?") + "'")
        }
        sb.appendLine("Datum: " + (match.date ?: "-"))
        sb.appendLine()

        if (extra.statistics.isNotEmpty()) {
            sb.appendLine("STATISZTIKAK:")
            for (team in extra.statistics) {
                sb.appendLine(team.teamName + ":")
                for ((k, v) in team.stats) {
                    sb.appendLine("  - " + k + ": " + v)
                }
            }
            sb.appendLine()
        }

        if (extra.lineups.isNotEmpty()) {
            sb.appendLine("FELALLASOK:")
            for (team in extra.lineups) {
                val formation = team.formation ?: "?"
                sb.appendLine(team.teamName + " (" + formation + "):")
                val starterNames = team.starters.joinToString(", ") { p -> p.name }
                sb.appendLine("  Kezdo: " + starterNames)
                if (team.substitutes.isNotEmpty()) {
                    val subNames = team.substitutes.joinToString(", ") { p -> p.name }
                    sb.appendLine("  Cserek: " + subNames)
                }
            }
            sb.appendLine()
        }

        if (extra.highlights.isNotEmpty()) {
            val titles = extra.highlights.joinToString(", ") { h -> h.title }
            sb.appendLine("HIGHLIGHTOK: " + titles)
            sb.appendLine()
        }

        sb.appendLine("Irj 400-700 szo koruli, strukturált magyar elemzest.")
        return sb.toString()
    }
}
