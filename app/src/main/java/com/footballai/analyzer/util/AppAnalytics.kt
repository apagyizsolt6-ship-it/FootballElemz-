package com.footballai.analyzer.util

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Könnyű analytics réteg. Firebase hozzáadásakor ide köthető a FirebaseAnalytics.
 * Most logcat-re ír (debug) – nem törí a buildet google-services nélkül.
 */
@Singleton
class AppAnalytics @Inject constructor() {
    fun logEvent(name: String, params: Map<String, String> = emptyMap()) {
        Log.d("FociAnalytics", "$name $params")
    }

    fun screenView(screen: String) = logEvent("screen_view", mapOf("screen" to screen))
    fun matchOpen(matchId: Long) = logEvent("match_open", mapOf("id" to matchId.toString()))
    fun aiGenerate(matchId: Long) = logEvent("ai_generate", mapOf("id" to matchId.toString()))
}
