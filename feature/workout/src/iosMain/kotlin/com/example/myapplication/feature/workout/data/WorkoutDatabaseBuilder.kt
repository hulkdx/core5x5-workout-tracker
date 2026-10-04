package com.example.myapplication.feature.workout.data

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
internal fun workoutDatabaseBuilder(): RoomDatabase.Builder<WorkoutDatabase> {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    val path = requireNotNull(directory?.path)
    return Room.databaseBuilder<WorkoutDatabase>(name = "$path/$WORKOUT_DATABASE_NAME")
}
