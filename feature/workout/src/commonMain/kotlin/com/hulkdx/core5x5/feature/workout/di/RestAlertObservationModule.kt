package com.hulkdx.core5x5.feature.workout.di

import com.hulkdx.core5x5.feature.workout.domain.RestAlertCoordinator
import com.hulkdx.core5x5.feature.workout.domain.RestAlertObserver
import org.koin.dsl.module
import kotlin.time.Clock

/** Included by native startup once its permission and scheduler implementations are available. */
val restAlertObservationModule = module {
    single {
        RestAlertCoordinator(
            scheduler = get(),
            nowEpochMillis = { Clock.System.now().toEpochMilliseconds() },
        )
    }
    single {
        RestAlertObserver(
            workouts = get(),
            preferences = get(),
            permissions = get(),
            coordinator = get(),
            applicationScope = get(),
        )
    }
}
