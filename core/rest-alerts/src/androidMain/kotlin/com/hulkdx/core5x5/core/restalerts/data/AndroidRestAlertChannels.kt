package com.hulkdx.core5x5.core.restalerts.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.provider.Settings

object AndroidRestAlertChannels {
    const val AUDIBLE = "rest_alerts_audible"
    const val SILENT = "rest_alerts_silent"

    fun create(context: Context) {
        val audible = NotificationChannel(
            AUDIBLE,
            "Rest notifications",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            setSound(Settings.System.DEFAULT_NOTIFICATION_URI, Notification.AUDIO_ATTRIBUTES_DEFAULT)
        }
        val silent = NotificationChannel(
            SILENT,
            "Silent rest notifications",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setSound(null, null)
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        // Reusing these IDs preserves the user's existing channel settings.
        notificationManager.createNotificationChannels(listOf(audible, silent))
    }
}
