package com.hulkdx.core5x5.feature.history.di

import com.hulkdx.core5x5.feature.history.presentation.HistoryViewModel
import com.hulkdx.core5x5.feature.history.presentation.WorkoutDetailViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val historyModule = module {
    viewModel { HistoryViewModel(source = get()) }
    viewModel { parameters -> WorkoutDetailViewModel(source = get(), workoutId = parameters.get()) }
}
