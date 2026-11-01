package com.hulkdx.core5x5.core.restalerts.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** The native notification-center operations used by the iOS alert adapter. */
@OptIn(ExperimentalForeignApi::class)
interface IosRestAlertNotificationCenter {
    suspend fun pendingRequests(): List<UNNotificationRequest>
    suspend fun addNotificationRequest(request: UNNotificationRequest)
    fun removePendingNotificationRequests(identifiers: List<String>)
    suspend fun deliveredNotificationIdentifiers(): List<String>
    fun removeDeliveredNotifications(identifiers: List<String>)
}

@OptIn(ExperimentalForeignApi::class)
class IosUserNotificationCenterAdapter : IosRestAlertNotificationCenter {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun pendingRequests(): List<UNNotificationRequest> = suspendCancellableCoroutine { continuation ->
        center.getPendingNotificationRequestsWithCompletionHandler { requests ->
            if (requests == null) {
                continuation.resumeWithException(IllegalStateException("Pending notifications were not returned"))
            } else {
                continuation.resume(requests.filterIsInstance<UNNotificationRequest>())
            }
        }
    }

    override suspend fun addNotificationRequest(request: UNNotificationRequest) {
        suspendCancellableCoroutine<Unit> { continuation ->
            center.addNotificationRequest(request) { error ->
                if (error == null) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(
                        IllegalStateException("Unable to schedule rest notification: ${error.localizedDescription}"),
                    )
                }
            }
        }
    }

    override fun removePendingNotificationRequests(identifiers: List<String>) {
        center.removePendingNotificationRequestsWithIdentifiers(identifiers)
    }

    override suspend fun deliveredNotificationIdentifiers(): List<String> = suspendCancellableCoroutine { continuation ->
        center.getDeliveredNotificationsWithCompletionHandler { notifications ->
            if (notifications == null) {
                continuation.resumeWithException(IllegalStateException("Delivered notifications were not returned"))
            } else {
                continuation.resume(
                    notifications.filterIsInstance<UNNotification>().map { it.request.identifier },
                )
            }
        }
    }

    override fun removeDeliveredNotifications(identifiers: List<String>) {
        center.removeDeliveredNotificationsWithIdentifiers(identifiers)
    }
}
