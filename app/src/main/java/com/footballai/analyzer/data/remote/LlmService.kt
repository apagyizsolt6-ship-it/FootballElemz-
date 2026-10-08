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
                        Exception("Nincs LLM API kulcs. Állítsd be a Beállításokban.")
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
                            content = """Te egy profi labdarúgó-elemző vagy. Magyarul írj.
Adj részletes, szakértői meccselemzést: helyzetértékelés, taktika, kulcsjátékosok,
statisztikai olvasat, előrejelzés. Legyél konkrét és szakszerű, ne sablonos."""
                        ),
                        ChatMessage(role = "user", content = prompt)
                    )
                )

                val requestBody = json.encodeToString(ChatRequest.serializer(), body)
                    .toRequestBody("application/json".toMediaType())

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
                        Exception("LLM hiba (${response.code}): ${responseBody.take(200)}")
                    )
                }

                val parsed = json.decodeFromString(ChatResponse.serializer(), responseBody)
                val text = parsed.choices.firstOrNull()?.message?.content
                    ?: return@withContext Result.failure(Exception("Üres LLM válasz"))

                Result.success(text)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun buildPrompt(match: Match, extra: MatchExtraData): String {
        return buildString {
            appendLine("Elemezd a következő focimeccset részletesen:")
            appendLine()
            appendLine("Hazai: ${match.homeTeamName}")
            appendLine("Vendég: ${match.awayTeamName}")
            appendLine("Liga: ${match.leagueName}")
            appendLine("Ország: ${match.countryName ?: "-"}")
            appendLine("Állás: ${match.score}")
            appendLine("Státusz: ${match.status}")
            if (match.isLive) appendLine("Perc: ${match.minute ?: "?"}'")
            appendLine("Dátum: ${match.date ?: "-"}")
            appendLine()

            if (extra.statistics.isNotEmpty()) {
                appendLine("STATISZTIKÁK:")
                extra.statistics.forEach { team ->
                    appendLine("${team.teamName}:")
                    team.stats.forEach { (k, v) -> appendLine("  - $k: $v") }
                }
                appendLine()
            }

            if (extra.lineups.isNotEmpty()) {
                appendLine("FELÁLLÁSOK:")
                extra.lineups.forEach { team ->
                    appendLine("\( {team.teamName} ( \){team.formation ?: "?"}):")
                    appendLine("  Kezdő: ${team.starters.joinToString { it.name }}")
                    if (team.substitutes.isNotEmpty()) {
                        appendLine("  Cserék: ${team.substitutes.joinToString { it.name }}")
                    }
                }
                appendLine()
            }

            if (extra.highlights.isNotEmpty()) {
                appendLine("HIGHLIGHTOK: ${extra.highlights.joinToString { it.title }}")
                appendLine()
            }

            appendLine("Írj 400–700 szó körüli, strukturált magyar elemzést.")
        }
    }
}
