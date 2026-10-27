package com.hulkdx.core5x5.app

import com.hulkdx.core5x5.di.appModule
import com.hulkdx.core5x5.di.iosAppModule
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertSame

class IosRestAlertGraphTest {
    @Test
    fun nativeGraphRetainsOneRegistrationWithoutIndexingTheObjectiveCSubclass() {
        val application = koinApplication { modules(appModule, iosAppModule) }
        try {
            val registration = application.koin.get<IosRestAlertNotificationRegistration>()
            assertSame(registration, application.koin.get<IosRestAlertNotificationRegistration>())
        } finally {
            application.close()
        }
    }
}
