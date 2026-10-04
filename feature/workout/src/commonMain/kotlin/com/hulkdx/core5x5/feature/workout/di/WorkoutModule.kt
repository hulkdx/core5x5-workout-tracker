package com.hulkdx.core5x5.feature.workout.di

import com.hulkdx.core5x5.feature.workout.data.WorkoutDatabase
import com.hulkdx.core5x5.feature.workout.data.RoomWorkoutRepository
import com.hulkdx.core5x5.feature.workout.domain.WorkoutRepository
import org.koin.core.module.Module
import org.koin.dsl.module

val workoutModule = module {
    includes(workoutPlatformModule)
    single { get<WorkoutDatabase>().unfinishedWorkoutDao() }
    single<WorkoutRepository> { RoomWorkoutRepository(get()) }
}

internal expect val workoutPlatformModule: Module
