package com.hulkdx.core5x5.core.restalerts.data

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object AndroidRestAlertNotification {
    private const val NOTIFICATION_ID = 1

    /** The expiry caller validates the saved rest and preferences before posting. */
    fun show(context: Context, soundEnabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, create(context, soundEnabled))
    }

    fun cancel(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun create(context: Context, soundEnabled: Boolean): Notification {
        val launchIntent = checkNotNull(
            context.packageManager.getLaunchIntentForPackage(context.packageName),
        ) { "Rest notifications require an app launcher activity" }
        val openApp = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE,
        )
        val channelId = if (soundEnabled) {
            AndroidRestAlertChannels.AUDIBLE
        } else {
            AndroidRestAlertChannels.SILENT
        }

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Set complete")
            .setContentText("Ready for your next set.")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setSilent(!soundEnabled)
            .build()
    }
}
