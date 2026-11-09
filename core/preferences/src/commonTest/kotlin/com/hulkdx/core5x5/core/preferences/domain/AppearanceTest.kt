package com.hulkdx.core5x5.core.preferences.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppearanceTest {
    @Test
    fun explicitChoicesOverrideDeviceAppearanceAndSystemFollowsIt() {
        for (deviceIsDark in listOf(false, true)) {
            assertFalse(Appearance.LIGHT.isDark(deviceIsDark))
            assertTrue(Appearance.DARK.isDark(deviceIsDark))
        }
        assertFalse(Appearance.SYSTEM.isDark(false))
        assertTrue(Appearance.SYSTEM.isDark(true))
    }
}
