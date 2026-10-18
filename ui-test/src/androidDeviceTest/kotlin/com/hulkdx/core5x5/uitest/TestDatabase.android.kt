package com.hulkdx.core5x5.uitest

import androidx.room3.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.hulkdx.core5x5.core.preferences.data.AndroidPreferencesStorage
import com.hulkdx.core5x5.feature.workout.data.WorkoutDatabase
import com.hulkdx.core5x5.feature.workout.data.buildWorkoutDatabase
import java.util.UUID

actual fun createTestDatabase(): TestDatabase {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val name = "journey-${UUID.randomUUID()}.db"
    return TestDatabase(
        openDatabase = { Room.databaseBuilder<WorkoutDatabase>(context, name).buildWorkoutDatabase() },
        preferencesStorage = AndroidPreferencesStorage(context, name),
        deleteFiles = {
            context.deleteDatabase(name)
            context.deleteSharedPreferences(name)
        },
    )
}
