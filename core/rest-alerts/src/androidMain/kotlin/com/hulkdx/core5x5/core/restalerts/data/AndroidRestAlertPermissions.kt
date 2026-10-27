package com.hulkdx.core5x5.core.restalerts.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissionStatus
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissions
import com.hulkdx.core5x5.core.restalerts.domain.RestNotificationPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidRestAlertPermissions(
    private val context: Context,
) : RestAlertPermissions {
    private val mutableStatus = MutableStateFlow(RestAlertPermissionStatus())
    override val status = mutableStatus.asStateFlow()

    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override suspend fun refreshStatus() {
        val notificationsEnabled = hasRuntimePermission() && notificationManager.areNotificationsEnabled()
        val preciseTimingAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            null
        }
        mutableStatus.value = RestAlertPermissionStatus(
            notificationPermission = if (notificationsEnabled) {
                RestNotificationPermission.GRANTED
            } else {
                RestNotificationPermission.DENIED
            },
            preciseTimingAllowed = preciseTimingAllowed,
        )
    }

    private fun hasRuntimePermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}
