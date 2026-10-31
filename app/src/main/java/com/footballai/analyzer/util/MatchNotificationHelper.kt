package com.footballai.analyzer.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.footballai.analyzer.MainActivity
import com.footballai.analyzer.domain.model.Match
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "focielemzo_match_alerts"
        const val CHANNEL_NAME = "Meccs értesítések"
    }

    private val lastScores = java.util.concurrent.ConcurrentHashMap<Long, String>()

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Gólok és állásváltozások a figyelt meccseknél"
            enableVibration(true)
        }
        mgr.createNotificationChannel(channel)
    }

    fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Ha a figyelt meccs állása változott, helyi értesítés.
     * @return true ha küldtünk értesítést
     */
    fun notifyScoreIfChanged(match: Match, watched: Boolean): Boolean {
        if (!watched) {
            lastScores.remove(match.id)
            return false
        }
        ensureChannel()
        val score = match.score.trim()
        val prev = lastScores.put(match.id, score)
        if (prev == null) {
            // első látás – ne zavarjuk gól-értesítéssel
            return false
        }
        if (prev == score) return false
        if (!hasPermission()) return false

        val title = if (match.isLive) "ÉLŐ · ${match.homeTeamName} – ${match.awayTeamName}"
        else "${match.homeTeamName} – ${match.awayTeamName}"
        val text = "Állás: $score" + if (match.isLive && match.minute != null) " (${match.minute}')" else ""

        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("matchId", match.id)
        }
        val pending = PendingIntent.getActivity(
            context,
            match.id.toInt(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$prev → $score\n$text"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(match.id.toInt(), notification)
            return true
        } catch (_: SecurityException) {
            return false
        }
    }

    fun notifyWatchEnabled(match: Match) {
        if (!hasPermission()) return
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Értesítések bekapcsolva")
            .setContentText("${match.homeTeamName} – ${match.awayTeamName}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context)
                .notify(("watch_" + match.id).hashCode(), notification)
        } catch (_: SecurityException) { }
    }
}
