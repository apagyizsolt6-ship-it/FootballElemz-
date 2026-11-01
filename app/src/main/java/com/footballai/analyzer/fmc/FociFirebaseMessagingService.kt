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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FociFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var apiKeyProvider: ApiKeyProvider
    @Inject lateinit var fcmBackend: FcmBackendApi

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch {
            apiKeyProvider.saveFcmToken(token)
            fcmBackend.registerToken(token)
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
        val mgr = getSystemService(NotificationManager::class.java)
        mgr?.notify((matchId ?: System.currentTimeMillis()).toInt(), notif)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(NotificationManager::class.java) ?: return
        val ch = NotificationChannel(
            MatchNotificationHelper.CHANNEL_ID,
            MatchNotificationHelper.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        mgr.createNotificationChannel(ch)
    }
}
