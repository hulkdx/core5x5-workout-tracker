package com.hulkdx.core5x5.di

import com.hulkdx.core5x5.core.preferences.di.preferencesModule
import com.hulkdx.core5x5.feature.history.di.historyModule
import com.hulkdx.core5x5.feature.settings.di.settingsModule
import com.hulkdx.core5x5.feature.workout.di.restTimerModule
import com.hulkdx.core5x5.feature.workout.di.workoutModule
import org.koin.dsl.module

val appModule = module {
    includes(preferencesModule, workoutModule, restTimerModule, historyModule, settingsModule)
}
