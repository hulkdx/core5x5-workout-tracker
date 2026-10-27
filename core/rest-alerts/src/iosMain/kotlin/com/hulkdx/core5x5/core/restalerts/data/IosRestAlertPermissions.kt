package com.hulkdx.core5x5.core.restalerts.data

import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissionStatus
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissions
import com.hulkdx.core5x5.core.restalerts.domain.RestNotificationPermission
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
class IosRestAlertPermissions(
    private val permissionCenter: IosRestAlertPermissionCenter = IosUserNotificationPermissionCenter(),
) : RestAlertPermissions {
    private val mutableStatus = MutableStateFlow(RestAlertPermissionStatus())
    override val status = mutableStatus.asStateFlow()

    override suspend fun refreshStatus() {
        val authorizationStatus = permissionCenter.authorizationStatus()
        mutableStatus.value = RestAlertPermissionStatus(
            notificationPermission = when (authorizationStatus) {
                UNAuthorizationStatusAuthorized,
                UNAuthorizationStatusProvisional,
                UNAuthorizationStatusEphemeral -> RestNotificationPermission.GRANTED
                UNAuthorizationStatusDenied -> RestNotificationPermission.DENIED
                else -> RestNotificationPermission.UNKNOWN
            },
        )
    }
}

/** Reads the raw authorization status from the iOS notification center. */
interface IosRestAlertPermissionCenter {
    suspend fun authorizationStatus(): Long
}

@OptIn(ExperimentalForeignApi::class)
private class IosUserNotificationPermissionCenter : IosRestAlertPermissionCenter {
    override suspend fun authorizationStatus(): Long = suspendCancellableCoroutine { continuation ->
        UNUserNotificationCenter.currentNotificationCenter()
            .getNotificationSettingsWithCompletionHandler { settings ->
                if (settings == null) {
                    continuation.resumeWithException(IllegalStateException("Notification settings were not returned"))
                } else {
                    continuation.resume(settings.authorizationStatus)
                }
            }
    }
}
