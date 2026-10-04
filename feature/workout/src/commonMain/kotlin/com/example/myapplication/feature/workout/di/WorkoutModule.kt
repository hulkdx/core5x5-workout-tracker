package com.example.myapplication.feature.workout.di

import com.example.myapplication.feature.workout.data.WorkoutDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val workoutModule = module {
    includes(workoutPlatformModule)
    single { get<WorkoutDatabase>().unfinishedWorkoutDao() }
}

internal expect val workoutPlatformModule: Module
