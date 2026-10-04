package com.hulkdx.core5x5.feature.workout.di

import com.hulkdx.core5x5.feature.workout.data.WorkoutDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val workoutModule = module {
    includes(workoutPlatformModule)
    single { get<WorkoutDatabase>().unfinishedWorkoutDao() }
}

internal expect val workoutPlatformModule: Module
