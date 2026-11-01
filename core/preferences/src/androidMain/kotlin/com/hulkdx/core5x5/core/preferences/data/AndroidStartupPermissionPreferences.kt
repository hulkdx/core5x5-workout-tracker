package com.hulkdx.core5x5.core.preferences.data

import android.content.Context
import com.hulkdx.core5x5.core.preferences.domain.StartupPermissionPreferences

class AndroidStartupPermissionPreferences(context: Context) : StartupPermissionPreferences {
    private val preferences = context.getSharedPreferences("startup_permissions", Context.MODE_PRIVATE)

    override val notificationPermissionRequested: Boolean
        get() = preferences.getBoolean(KEY, false)

    override fun markNotificationPermissionRequested() {
        preferences.edit().putBoolean(KEY, true).apply()
    }

    val preciseTimingExplanationShown: Boolean
        get() = preferences.getBoolean(TIMING_EXPLANATION_KEY, false)

    fun markPreciseTimingExplanationShown() {
        preferences.edit().putBoolean(TIMING_EXPLANATION_KEY, true).apply()
    }

    private companion object {
        const val KEY = "notification_permission_requested"
        const val TIMING_EXPLANATION_KEY = "precise_timing_explanation_shown"
    }
}
