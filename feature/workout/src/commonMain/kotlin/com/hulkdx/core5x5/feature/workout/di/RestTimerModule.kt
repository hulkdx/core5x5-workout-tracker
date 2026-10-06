package com.hulkdx.core5x5.feature.workout.di

import com.hulkdx.core5x5.feature.workout.domain.RestTimerRules
import org.koin.dsl.module
import kotlin.time.Clock

val restTimerModule = module {
    factory { RestTimerRules { Clock.System.now().toEpochMilliseconds() } }
}
