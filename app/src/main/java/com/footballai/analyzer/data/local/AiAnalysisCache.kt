package com.footballai.analyzer.data.local

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session-szintű cache: ugyanarra a meccsre ne hívjon újra LLM-et.
 * App újraindításakor ürül (költségvédelem + friss adat).
 */
@Singleton
class AiAnalysisCache @Inject constructor() {
    private val map = ConcurrentHashMap<Long, String>()

    fun get(matchId: Long): String? = map[matchId]

    fun put(matchId: Long, analysis: String) {
        map[matchId] = analysis
    }

    fun clear() {
        map.clear()
    }
}
