package com.footballai.analyzer.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.footballai.analyzer.MainActivity
import com.footballai.analyzer.data.local.ApiKeyProvider
import com.footballai.analyzer.data.remote.FcmBackendApi
import com.footballai.analyzer.util.MatchNotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Nincs @AndroidEntryPoint – Hilt EntryPoint-tal injektálunk.
 * (FirebaseMessagingService + kapt egyébként gyakran elhasal.)
 */
class FociFirebaseMessagingService : FirebaseMessagingService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun apiKeyProvider(): ApiKeyProvider
        fun fcmBackendApi(): FcmBackendApi
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun deps(): Deps =
        EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)

    override fun onNewToken(token: String) {
        scope.launch {
            try {
                val d = deps()
                d.apiKeyProvider().saveFcmToken(token)
                d.fcmBackendApi().registerToken(token)
            } catch (_: Exception) { }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "FociElemző"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: message.data["score"]
            ?: "Új meccs esemény"
        val matchId = message.data["matchId"]?.toLongOrNull()

        ensureChannel()
        val open = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            matchId?.let { putExtra("matchId", it) }
        }
        val pending = PendingIntent.getActivity(
            this,
            matchId?.toInt() ?: 0,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, MatchNotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        getSystemService(NotificationManager::class.java)
            ?.notify((matchId ?: System.currentTimeMillis()).toInt(), notif)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(NotificationManager::class.java) ?: return
        mgr.createNotificationChannel(
            NotificationChannel(
                MatchNotificationHelper.CHANNEL_ID,
                MatchNotificationHelper.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }
}
