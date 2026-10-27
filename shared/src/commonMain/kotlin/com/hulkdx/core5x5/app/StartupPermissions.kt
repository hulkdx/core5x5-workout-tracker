package com.hulkdx.core5x5.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import com.hulkdx.core5x5.core.common.runCatchingSuspend
import com.hulkdx.core5x5.core.preferences.domain.StartupPermissionPreferences
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissions
import com.hulkdx.core5x5.feature.settings.permissions.rememberNotificationPermissionRequester
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun StartupPermissions() {
    val preferences: StartupPermissionPreferences = koinInject()
    val permissions: RestAlertPermissions = koinInject()
    val scope = rememberCoroutineScope()
    // Register even when history skips launching, so restored results still have a callback.
    val requestNotifications = rememberNotificationPermissionRequester(onFinished = {
        scope.launch {
            // Read full notification settings after either grant or denial.
            runCatchingSuspend { permissions.refreshStatus() }
        }
    })
    StartupPermissionFollowUp()

    LaunchedEffect(Unit) {
        // Give the initial content a frame before opening system permission UI.
        withFrameNanos { }
        runCatchingSuspend {
            if (!preferences.notificationPermissionRequested) {
                // Record the attempt before native UI so recreation cannot repeat it.
                preferences.markNotificationPermissionRequested()
                requestNotifications()
            }
        }
    }
}

/** Mounts platform-specific follow-up UI that observes the refreshed permission status. */
@Composable
expect fun StartupPermissionFollowUp()
