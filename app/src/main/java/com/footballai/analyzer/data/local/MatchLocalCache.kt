package com.footballai.analyzer.data.local

import android.content.Context
import com.footballai.analyzer.domain.model.Match
import com.footballai.analyzer.domain.model.MatchExtraData
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Memória + lemez cache az API hívások csökkentésére.
 *
 * TTL:
 * - élő meccs lista / extra: 45 mp
 * - mai nap (nem élő): 3 perc
 * - múltbeli nap / lezárt: 12 óra
 * - jövőbeli nap: 10 perc
 * - meccs extra (nem élő): 15 perc, lezárt: 6 óra
 */
@Singleton
class MatchLocalCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    private val memMatches = ConcurrentHashMap<String, TimedEntry<List<Match>>>()
    private val memExtra = ConcurrentHashMap<Long, TimedEntry<MatchExtraData>>()
    private val memMatch = ConcurrentHashMap<Long, TimedEntry<Match>>()

    private val dir: File by lazy {
        File(context.cacheDir, "match_cache").also { it.mkdirs() }
    }

    suspend fun getMatches(dateKey: String): List<Match>? = withContext(Dispatchers.IO) {
        memMatches[dateKey]?.takeIf { !it.isExpired() }?.value
            ?: readDiskMatches(dateKey)?.also { (list, expiresAt) ->
                memMatches[dateKey] = TimedEntry(list, expiresAt)
            }?.first
    }

    suspend fun putMatches(dateKey: String, matches: List<Match>, ttlMs: Long) =
        withContext(Dispatchers.IO) {
            val expiresAt = System.currentTimeMillis() + ttlMs
            memMatches[dateKey] = TimedEntry(matches, expiresAt)
            writeDisk(
                file = File(dir, "matches_$dateKey.json"),
                payload = CacheEnvelope(expiresAt, matches)
            )
        }

    suspend fun getExtra(matchId: Long): MatchExtraData? = withContext(Dispatchers.IO) {
        memExtra[matchId]?.takeIf { !it.isExpired() }?.value
            ?: readDiskExtra(matchId)?.also { (data, expiresAt) ->
                memExtra[matchId] = TimedEntry(data, expiresAt)
            }?.first
    }

    suspend fun putExtra(matchId: Long, data: MatchExtraData, ttlMs: Long) =
        withContext(Dispatchers.IO) {
            val expiresAt = System.currentTimeMillis() + ttlMs
            memExtra[matchId] = TimedEntry(data, expiresAt)
            writeDisk(
                file = File(dir, "extra_$matchId.json"),
                payload = CacheEnvelope(expiresAt, data)
            )
        }

    suspend fun getMatch(matchId: Long): Match? = withContext(Dispatchers.IO) {
        memMatch[matchId]?.takeIf { !it.isExpired() }?.value
    }

    suspend fun putMatch(match: Match, ttlMs: Long) = withContext(Dispatchers.IO) {
        val expiresAt = System.currentTimeMillis() + ttlMs
        memMatch[match.id] = TimedEntry(match, expiresAt)
    }

    /** Pull-to-refresh / manuális frissítés */
    fun invalidateDate(dateKey: String) {
        memMatches.remove(dateKey)
        File(dir, "matches_$dateKey.json").delete()
    }

    fun clearAll() {
        memMatches.clear()
        memExtra.clear()
        memMatch.clear()
        dir.listFiles()?.forEach { it.delete() }
    }

    fun invalidateMatch(matchId: Long) {
        memExtra.remove(matchId)
        memMatch.remove(matchId)
        File(dir, "extra_$matchId.json").delete()
    }

    private fun readDiskMatches(dateKey: String): Pair<List<Match>, Long>? {
        return try {
            val file = File(dir, "matches_$dateKey.json")
            if (!file.exists()) return null
            val env = json.decodeFromString<CacheEnvelope<List<Match>>>(file.readText())
            if (env.expiresAt < System.currentTimeMillis()) {
                file.delete()
                null
            } else env.data to env.expiresAt
        } catch (_: Exception) {
            null
        }
    }

    private fun readDiskExtra(matchId: Long): Pair<MatchExtraData, Long>? {
        return try {
            val file = File(dir, "extra_$matchId.json")
            if (!file.exists()) return null
            val env = json.decodeFromString<CacheEnvelope<MatchExtraData>>(file.readText())
            if (env.expiresAt < System.currentTimeMillis()) {
                file.delete()
                null
            } else env.data to env.expiresAt
        } catch (_: Exception) {
            null
        }
    }

    private inline fun <reified T> writeDisk(file: File, payload: CacheEnvelope<T>) {
        try {
            file.writeText(json.encodeToString(payload))
        } catch (_: Exception) {
            // ignore disk errors
        }
    }

    private data class TimedEntry<T>(
        val value: T,
        val expiresAt: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
    }

    @Serializable
    private data class CacheEnvelope<T>(
        val expiresAt: Long,
        val data: T
    )

    companion object {
        const val TTL_LIVE_MS = 45_000L
        const val TTL_TODAY_MS = 3 * 60_000L
        const val TTL_PAST_MS = 12 * 60 * 60_000L
        const val TTL_FUTURE_MS = 10 * 60_000L
        const val TTL_EXTRA_LIVE_MS = 45_000L
        const val TTL_EXTRA_OPEN_MS = 15 * 60_000L
        const val TTL_EXTRA_FINISHED_MS = 6 * 60 * 60_000L
        const val TTL_MATCH_MS = 60_000L
    }
}
