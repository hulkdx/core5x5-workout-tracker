package com.hulkdx.core5x5.di

import com.hulkdx.core5x5.app.IosRestAlertNotificationRegistration
import com.hulkdx.core5x5.app.RestAlertExpiryHandler
import com.hulkdx.core5x5.core.restalerts.data.IosRestAlertPermissions
import com.hulkdx.core5x5.core.restalerts.data.IosRestAlertPlatform
import com.hulkdx.core5x5.core.restalerts.data.IosRestAlertStateStore
import com.hulkdx.core5x5.core.restalerts.data.PersistedRestAlertScheduler
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissions
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertScheduler
import com.hulkdx.core5x5.feature.workout.di.restAlertObservationModule
import org.koin.dsl.module
import kotlin.time.Clock

val iosAppModule = module {
    includes(restAlertObservationModule)
    single<RestAlertPermissions> { IosRestAlertPermissions() }
    single {
        PersistedRestAlertScheduler(
            stateStore = IosRestAlertStateStore(),
            platform = IosRestAlertPlatform(),
            nowEpochMillis = { Clock.System.now().toEpochMilliseconds() },
        )
    }
    single<RestAlertScheduler> { get<PersistedRestAlertScheduler>() }
    single {
        RestAlertExpiryHandler(
            scheduler = get(),
            coordinator = get(),
            workouts = get(),
            preferences = get(),
            permissions = get(),
        )
    }
    single {
        IosRestAlertNotificationRegistration(
            applicationScope = get(),
            expiryHandler = lazy { get<RestAlertExpiryHandler>() },
        )
    }
}
