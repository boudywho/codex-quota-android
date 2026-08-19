package com.codex.quota.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.codex.quota.R
import com.codex.quota.domain.model.CodexAccount
import com.codex.quota.ui.MainActivity

class SignedOutNotificationManager(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_auth_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_auth_description)
                enableVibration(true)
            }
            val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            systemManager.createNotificationChannel(channel)
        }
    }

    fun showSignedOutNotification(account: CodexAccount) {
        val deepLinkUri = Uri.parse("codexquota://account/${account.id}")
        val intent = Intent(Intent.ACTION_VIEW, deepLinkUri, context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            account.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Codex account signed out")
            .setContentText("${account.nickname} needs you to sign in again.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${account.nickname} (${account.planType.displayName}) credentials have expired or were revoked. Tap to re-authenticate.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Sign In",
                pendingIntent
            )
            .build()

        try {
            notificationManager.notify(NOTIFICATION_TAG_PREFIX + account.id, NOTIFICATION_ID_BASE + account.id.hashCode(), notification)
        } catch (e: SecurityException) {
            // Notifications permission not granted on Android 13+
        }
    }

    fun clearNotification(accountId: String) {
        notificationManager.cancel(NOTIFICATION_TAG_PREFIX + accountId, NOTIFICATION_ID_BASE + accountId.hashCode())
    }

    companion object {
        const val CHANNEL_ID = "channel_codex_auth_alerts"
        private const val NOTIFICATION_TAG_PREFIX = "auth_alert_"
        private const val NOTIFICATION_ID_BASE = 1000
    }
}
