package com.hulkdx.core5x5.feature.workout.di

import com.hulkdx.core5x5.feature.workout.presentation.RestTimerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val restTimerModule = module {
    viewModel { RestTimerViewModel() }
}
