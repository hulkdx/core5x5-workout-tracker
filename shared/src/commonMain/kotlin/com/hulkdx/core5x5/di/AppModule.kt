package com.hulkdx.core5x5.di

import com.hulkdx.core5x5.feature.workout.di.workoutModule
import com.hulkdx.core5x5.shell.ShellViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val appModule = module {
    includes(workoutModule)
    viewModel { ShellViewModel() }
}
