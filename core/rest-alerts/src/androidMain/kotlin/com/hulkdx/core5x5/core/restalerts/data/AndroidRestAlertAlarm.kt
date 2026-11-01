package com.hulkdx.core5x5.core.restalerts.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Build
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertId

object AndroidRestAlertAlarm {
    /** The scheduler validates the saved rest and future deadline before calling this. */
    fun schedule(context: Context, receiverClass: Class<out BroadcastReceiver>, id: RestAlertId) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            AndroidRestAlertIntent.create(context, receiverClass, id),
            PendingIntent.FLAG_IMMUTABLE,
        )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                id.deadlineEpochMillis,
                pendingIntent,
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                id.deadlineEpochMillis,
                pendingIntent,
            )
        }
    }

    fun cancel(context: Context, receiverClass: Class<out BroadcastReceiver>, id: RestAlertId) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            AndroidRestAlertIntent.create(context, receiverClass, id),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(pendingIntent)
    }
}
