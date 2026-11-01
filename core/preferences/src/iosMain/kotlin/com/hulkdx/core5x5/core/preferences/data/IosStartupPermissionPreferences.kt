package com.hulkdx.core5x5.core.preferences.data

import com.hulkdx.core5x5.core.preferences.domain.StartupPermissionPreferences
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUserDefaults

@OptIn(ExperimentalForeignApi::class)
class IosStartupPermissionPreferences(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : StartupPermissionPreferences {
    override val notificationPermissionRequested: Boolean
        get() = defaults.boolForKey(KEY)

    override fun markNotificationPermissionRequested() {
        defaults.setBool(true, forKey = KEY)
    }

    private companion object {
        const val KEY = "com.hulkdx.core5x5.notification_permission_requested"
    }
}
