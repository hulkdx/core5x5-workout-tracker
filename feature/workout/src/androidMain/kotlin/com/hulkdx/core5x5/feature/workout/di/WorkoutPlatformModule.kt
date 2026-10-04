package com.hulkdx.core5x5.feature.workout.di

import com.hulkdx.core5x5.feature.workout.data.buildWorkoutDatabase
import com.hulkdx.core5x5.feature.workout.data.workoutDatabaseBuilder
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

internal actual val workoutPlatformModule: Module = module {
    single { workoutDatabaseBuilder(get()).buildWorkoutDatabase() } onClose { it?.close() }
}
