package com.hulkdx.core5x5.core.preferences.data

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)
class IosStartupPermissionPreferencesTest {
    @Test
    fun promptHistoryDefaultsFalseAndSurvivesRecreationInItsOwnDomain() {
        val suite = "core5x5.test.startup.${NSUUID().UUIDString}"
        val defaults = NSUserDefaults(suiteName = suite)
        try {
            val initial = IosStartupPermissionPreferences(defaults)
            assertFalse(initial.notificationPermissionRequested)
            initial.markNotificationPermissionRequested()
            assertTrue(IosStartupPermissionPreferences(NSUserDefaults(suiteName = suite)).notificationPermissionRequested)
        } finally {
            defaults.removePersistentDomainForName(suite)
        }
    }
}
