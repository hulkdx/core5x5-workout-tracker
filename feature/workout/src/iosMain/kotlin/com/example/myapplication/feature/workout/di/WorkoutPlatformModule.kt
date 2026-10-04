package com.example.myapplication.feature.workout.di

import com.example.myapplication.feature.workout.data.buildWorkoutDatabase
import com.example.myapplication.feature.workout.data.workoutDatabaseBuilder
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

internal actual val workoutPlatformModule: Module = module {
    single { workoutDatabaseBuilder().buildWorkoutDatabase() } onClose { it?.close() }
}
