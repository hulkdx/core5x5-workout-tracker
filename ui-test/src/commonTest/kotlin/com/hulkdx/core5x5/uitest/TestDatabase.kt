package com.hulkdx.core5x5.uitest

import com.hulkdx.core5x5.core.preferences.data.PreferencesStorage
import com.hulkdx.core5x5.feature.workout.data.WorkoutDatabase

class TestDatabase(
    private val openDatabase: () -> WorkoutDatabase,
    val preferencesStorage: PreferencesStorage,
    private val deleteFiles: () -> Unit,
) {
    var database = openDatabase()
        private set

    fun reopen() {
        database.close()
        database = openDatabase()
    }

    fun close() {
        try {
            database.close()
        } finally {
            deleteFiles()
        }
    }
}

expect fun createTestDatabase(): TestDatabase
