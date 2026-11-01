package com.footballai.analyzer.data.remote

import com.footballai.analyzer.data.local.ApiKeyProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmBackendApi @Inject constructor(
    private val apiKeyProvider: ApiKeyProvider
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun baseUrl(): String {
        var u = apiKeyProvider.getFcmServerUrlBlocking().trim()
        if (u.isBlank()) return ""
        if (!u.startsWith("http")) u = "https://$u"
        if (u.endsWith("/")) u = u.dropLast(1)
        return u
    }

    private fun secret(): String = apiKeyProvider.getFcmServerSecretBlocking().trim()

    suspend fun registerToken(token: String): Boolean =
        post("/register", JSONObject().put("token", token))

    suspend fun watch(token: String, matchId: Long): Boolean =
        post(
            "/watch",
            JSONObject().put("token", token).put("matchId", matchId.toString())
        )

    suspend fun unwatch(token: String, matchId: Long): Boolean =
        post(
            "/unwatch",
            JSONObject().put("token", token).put("matchId", matchId.toString())
        )

    suspend fun syncWatched(token: String, matchIds: Collection<Long>): Boolean {
        val arr = JSONArray()
        matchIds.forEach { arr.put(it.toString()) }
        return post("/sync", JSONObject().put("token", token).put("matchIds", arr))
    }

    private suspend fun post(path: String, body: JSONObject): Boolean = withContext(Dispatchers.IO) {
        val base = baseUrl()
        if (base.isBlank()) return@withContext false
        try {
            val b = Request.Builder()
                .url(base + path)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .header("Content-Type", "application/json")
            val s = secret()
            if (s.isNotBlank()) b.header("x-api-secret", s)
            client.newCall(b.build()).execute().isSuccessful
        } catch (_: Exception) {
            false
        }
    }
}
