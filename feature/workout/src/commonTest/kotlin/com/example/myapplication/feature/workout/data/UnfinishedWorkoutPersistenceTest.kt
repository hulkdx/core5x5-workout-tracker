package com.example.myapplication.feature.workout.data

import com.example.myapplication.feature.workout.domain.Workout
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

/** Shared contract exercised by a platform fixture with a real on-disk Room database. */
internal abstract class UnfinishedWorkoutPersistenceTest {
    abstract fun openDatabase(name: String): WorkoutDatabase
    abstract fun deleteDatabase(name: String)

    @Test
    fun unfinishedWorkoutSurvivesDatabaseReopen() = runTest {
        for (workout in Workout.entries) {
            val name = "unfinished-${workout.name}.db"
            deleteDatabase(name)
            try {
                val expected = UnfinishedWorkoutEntity(
                    workout = workout,
                    startedAtEpochMillis = 1_790_000_000_123L,
                )
                val database = openDatabase(name)
                try {
                    val dao = database.unfinishedWorkoutDao()
                    assertNull(dao.getUnfinishedWorkout())
                    dao.insert(expected)
                    assertEquals(expected, dao.getUnfinishedWorkout())
                    assertFails {
                        dao.insert(expected.copy(startedAtEpochMillis = 42L))
                    }
                    assertEquals(expected, dao.getUnfinishedWorkout())
                } finally {
                    database.close()
                }
                val reopened = openDatabase(name)
                try {
                    assertEquals(expected, reopened.unfinishedWorkoutDao().getUnfinishedWorkout())
                } finally {
                    reopened.close()
                }
            } finally {
                deleteDatabase(name)
            }
        }
    }
}
