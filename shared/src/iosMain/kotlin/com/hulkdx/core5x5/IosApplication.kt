package com.hulkdx.core5x5

import com.hulkdx.core5x5.app.IosRestAlertNotificationRegistration
import com.hulkdx.core5x5.di.appModule
import com.hulkdx.core5x5.di.iosAppModule
import org.koin.core.context.startKoin

/** Starts the graph and registers its strongly retained delegate before native launch completes. */
fun startIosApplication() {
    val application = startKoin { modules(appModule, iosAppModule) }
    application.koin.get<IosRestAlertNotificationRegistration>().register()
}
