package com.hulkdx.core5x5.di

import com.hulkdx.core5x5.app.AndroidRestAlertReceiver
import com.hulkdx.core5x5.app.RestAlertExpiryHandler
import com.hulkdx.core5x5.core.restalerts.data.AndroidRestAlertPlatform
import com.hulkdx.core5x5.core.restalerts.data.AndroidRestAlertStateStore
import com.hulkdx.core5x5.core.restalerts.data.PersistedRestAlertScheduler
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertScheduler
import com.hulkdx.core5x5.feature.workout.di.restAlertObservationModule
import org.koin.dsl.module
import kotlin.time.Clock

val androidRestAlertDeliveryModule = module {
    includes(restAlertObservationModule)
    single {
        PersistedRestAlertScheduler(
            stateStore = AndroidRestAlertStateStore(context = get()),
            platform = AndroidRestAlertPlatform(
                context = get(),
                receiverClass = AndroidRestAlertReceiver::class.java,
            ),
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
}
