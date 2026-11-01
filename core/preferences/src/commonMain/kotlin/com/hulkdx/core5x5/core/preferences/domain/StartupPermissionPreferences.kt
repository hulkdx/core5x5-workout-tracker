package com.hulkdx.core5x5.core.preferences.domain

/** Persisted startup-prompt history, independent of current OS permission status. */
interface StartupPermissionPreferences {
    val notificationPermissionRequested: Boolean

    fun markNotificationPermissionRequested()
}
