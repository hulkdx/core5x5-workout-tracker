package com.hulkdx.core5x5.di

import com.hulkdx.core5x5.feature.history.di.historyModule
import com.hulkdx.core5x5.feature.settings.di.settingsModule
import com.hulkdx.core5x5.feature.workout.di.restTimerModule
import com.hulkdx.core5x5.feature.workout.di.workoutModule
import com.hulkdx.core5x5.shell.ShellViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val appModule = module {
    includes(workoutModule, restTimerModule, historyModule, settingsModule)
    viewModel { ShellViewModel() }
}
