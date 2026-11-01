package com.hulkdx.core5x5.app

import com.hulkdx.core5x5.core.common.Logger
import com.hulkdx.core5x5.core.common.runCatchingSuspend
import com.hulkdx.core5x5.core.restalerts.data.IosRestAlertIdentifier
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertId
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertRequest
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

/** Koin can index this Kotlin owner; Kotlin subclasses of NSObject have no supported KClass key. */
@OptIn(ExperimentalForeignApi::class)
class IosRestAlertNotificationRegistration(
    applicationScope: CoroutineScope,
    expiryHandler: Lazy<RestAlertExpiryHandler>,
) {
    private val delegate = IosRestAlertNotificationDelegate(applicationScope, expiryHandler)

    fun register() {
        UNUserNotificationCenter.currentNotificationCenter().delegate = delegate
    }
}

/** Retained by its application-owned registration; the notification center holds a weak reference. */
@OptIn(ExperimentalForeignApi::class)
class IosRestAlertNotificationDelegate(
    private val applicationScope: CoroutineScope,
    private val expiryHandler: Lazy<RestAlertExpiryHandler>,
) : NSObject(), UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
    ) {
        handleIosForegroundRestAlert(
            applicationScope = applicationScope,
            id = IosRestAlertIdentifier.decode(willPresentNotification.request.identifier),
            handleExpiry = { id, present -> expiryHandler.value.handle(id, present) },
            withCompletionHandler = withCompletionHandler,
        )
    }

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit,
    ) {
        // The native default action opens the normal app entry; no sound or custom navigation.
        withCompletionHandler()
    }
}

/** Handles foreground policy without requiring tests to construct system-owned UNNotification. */
fun handleIosForegroundRestAlert(
    applicationScope: CoroutineScope,
    id: RestAlertId?,
    handleExpiry: suspend (RestAlertId, suspend (RestAlertRequest) -> Unit) -> Unit,
    withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) {
    if (id == null) {
        withCompletionHandler(0uL)
        return
    }

    var options: UNNotificationPresentationOptions = 0uL
    val job = applicationScope.launch(dispatcher) {
        runCatchingSuspend {
            handleExpiry(id) { request ->
                if (request.soundEnabled) options = UNNotificationPresentationOptionSound
            }
        }.onFailure { error ->
            options = 0uL
            Logger.warn("RestAlerts", "Could not handle foreground rest expiry", error)
        }
    }
    // Also complete if graph closure cancels the job before its body starts.
    job.invokeOnCompletion { error ->
        withCompletionHandler(if (error == null) options else 0uL)
    }
}
