package com.footballai.analyzer.fcm

import com.footballai.analyzer.data.local.ApiKeyProvider
import com.footballai.analyzer.data.local.UserPreferences
import com.footballai.analyzer.data.remote.FcmBackendApi
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmRegistrar @Inject constructor(
    private val apiKeyProvider: ApiKeyProvider,
    private val fcmBackend: FcmBackendApi,
    private val userPreferences: UserPreferences
) {
    fun registerInBackground(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                apiKeyProvider.saveFcmToken(token)
                fcmBackend.registerToken(token)
                val watched = userPreferences.watchedMatches.first()
                    .mapNotNull { it.toLongOrNull() }
                if (watched.isNotEmpty()) {
                    fcmBackend.syncWatched(token, watched)
                }
            } catch (_: Exception) {
                // Nincs Firebase / nincs net – csendben kihagyjuk
            }
        }
    }

    suspend fun syncWatch(matchId: Long, enabled: Boolean) {
        val token = apiKeyProvider.getFcmToken().ifBlank {
            try {
                FirebaseMessaging.getInstance().token.await().also {
                    apiKeyProvider.saveFcmToken(it)
                }
            } catch (_: Exception) {
                return
            }
        }
        fcmBackend.registerToken(token)
        if (enabled) fcmBackend.watch(token, matchId)
        else fcmBackend.unwatch(token, matchId)
    }
}
