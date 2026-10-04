package com.hulkdx.core5x5.feature.workout.data

import androidx.room3.Room
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory

@OptIn(ExperimentalForeignApi::class)
internal class IosUnfinishedWorkoutPersistenceTest : UnfinishedWorkoutPersistenceTest() {
    override fun openDatabase(name: String): WorkoutDatabase =
        Room.databaseBuilder<WorkoutDatabase>(name = NSTemporaryDirectory() + name)
            .buildWorkoutDatabase()

    override fun deleteDatabase(name: String) {
        for (suffix in listOf("", "-wal", "-shm")) {
            NSFileManager.defaultManager.removeItemAtPath(NSTemporaryDirectory() + name + suffix, null)
        }
    }
}
