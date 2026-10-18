package com.hulkdx.core5x5.uitest

import androidx.room3.Room
import com.hulkdx.core5x5.feature.workout.data.WorkoutDatabase
import com.hulkdx.core5x5.feature.workout.data.buildWorkoutDatabase
import com.hulkdx.core5x5.core.preferences.data.IosPreferencesStorage
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults

@OptIn(ExperimentalForeignApi::class)
actual fun createTestDatabase(): TestDatabase {
    val name = "journey-${NSUUID().UUIDString}"
    val path = NSTemporaryDirectory() + name + ".db"
    val defaults = NSUserDefaults(suiteName = name)
    return TestDatabase(
        openDatabase = { Room.databaseBuilder<WorkoutDatabase>(name = path).buildWorkoutDatabase() },
        preferencesStorage = IosPreferencesStorage(defaults),
        deleteFiles = {
            for (suffix in listOf("", "-wal", "-shm")) {
                NSFileManager.defaultManager.removeItemAtPath(path + suffix, null)
            }
            defaults.removePersistentDomainForName(name)
        },
    )
}
