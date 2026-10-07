package com.hulkdx.core5x5.feature.workout.di

import com.hulkdx.core5x5.feature.workout.data.WorkoutDatabase
import com.hulkdx.core5x5.feature.workout.data.RoomWorkoutRepository
import com.hulkdx.core5x5.feature.workout.domain.WorkoutRepository
import com.hulkdx.core5x5.feature.workout.presentation.ActiveWorkoutViewModel
import com.hulkdx.core5x5.feature.workout.presentation.TodayViewModel
import com.hulkdx.core5x5.feature.workout.presentation.WorkoutCompleteViewModel
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutSource
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val workoutModule = module {
    includes(workoutPlatformModule, restTimerModule)
    single { get<WorkoutDatabase>().unfinishedWorkoutDao() }
    single { RoomWorkoutRepository(get()) }
    single<WorkoutRepository> { get<RoomWorkoutRepository>() }
    single<CompletedWorkoutSource> { get<RoomWorkoutRepository>() }
    viewModel { parameters ->
        ActiveWorkoutViewModel(repository = get(), workoutId = parameters.getOrNull(), restTimerRules = get())
    }
    viewModel { TodayViewModel(get()) }
    viewModel { parameters -> WorkoutCompleteViewModel(workoutId = parameters.get(), repository = get()) }
}

internal expect val workoutPlatformModule: Module
