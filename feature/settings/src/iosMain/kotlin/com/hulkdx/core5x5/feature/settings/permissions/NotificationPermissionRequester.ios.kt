package com.hulkdx.core5x5.feature.settings.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.hulkdx.core5x5.core.common.Logger
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberNotificationPermissionRequester(
    onFinished: () -> Unit,
): () -> Unit {
    val latestOnFinished by rememberUpdatedState(onFinished)

    return {
        val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()
        notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
            if (settings?.authorizationStatus == UNAuthorizationStatusDenied) {
                dispatch_async(dispatch_get_main_queue()) {
                    val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
                    if (settingsUrl == null) {
                        Logger.warn(
                            "RestAlerts",
                            "Could not create the app settings URL",
                            IllegalStateException("UIApplicationOpenSettingsURLString is not a valid URL"),
                        )
                    } else {
                        UIApplication.sharedApplication.openURL(
                            url = settingsUrl,
                            options = emptyMap<Any?, Any?>(),
                            completionHandler = null,
                        )
                    }
                    latestOnFinished()
                }
            } else {
                notificationCenter.requestAuthorizationWithOptions(
                    UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
                ) { _, error ->
                    if (error != null) {
                        Logger.warn(
                            "RestAlerts",
                            "Could not request notification permission",
                            IllegalStateException(error.localizedDescription),
                        )
                    }
                    dispatch_async(dispatch_get_main_queue()) {
                        latestOnFinished()
                    }
                }
            }
        }
    }
}
