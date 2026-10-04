package com.hulkdx.core5x5.feature.workout.data

import com.hulkdx.core5x5.feature.workout.domain.Exercise
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutExercise
import com.hulkdx.core5x5.feature.workout.domain.Workout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

/** Shared contract exercised by a platform fixture with a real on-disk Room database. */
internal abstract class UnfinishedWorkoutPersistenceTest {
    abstract fun databasePath(name: String): String
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

    @Test
    fun startCreatesAndPersistsSelectedProgramInOrder() = runTest {
        for (workout in Workout.entries) {
            withDatabase("start-${workout.name}.db") { database ->
                val repository = RoomWorkoutRepository(database.unfinishedWorkoutDao()) { 1234L }
                val session = repository.startWorkout(workout)
                assertEquals(workout, session.workout)
                assertEquals(1234L, session.startedAtEpochMillis)
                assertProgram(workout, session)
                val stored = requireNotNull(database.unfinishedWorkoutDao().getSession())
                assertEquals(workout, stored.session.workout)
                assertEquals(1234L, stored.session.startedAtEpochMillis)
                assertEquals(listOf(0, 1, 2), stored.exercises.map { it.position })
                assertEquals(session.exercises.map { it.exercise }, stored.exercises.map { it.exercise })
                assertEquals(session.exercises.map { it.sets }, stored.exercises.map { it.sets })
                assertEquals(listOf(5, 5, 5), stored.exercises.map { it.reps })
                assertEquals(listOf(20.0, 20.0, 20.0), stored.exercises.map { it.weightKg })
            }
        }
    }

    @Test
    fun repeatedStartPreservesOriginalSessionEvenAfterReopen() = runTest {
        val name = "repeat-start.db"
        deleteDatabase(name)
        try {
            val database = openDatabase(name)
            val original = try {
                val dao = database.unfinishedWorkoutDao()
                val session = RoomWorkoutRepository(dao) { 100L }.startWorkout(Workout.A)
                assertEquals(session, RoomWorkoutRepository(dao) { 200L }.startWorkout(Workout.B))
                session
            } finally {
                database.close()
            }
            val reopened = openDatabase(name)
            try {
                val dao = reopened.unfinishedWorkoutDao()
                assertEquals(original, RoomWorkoutRepository(dao) { 300L }.startWorkout(Workout.B))
                assertEquals(3, dao.getExercises().size)
            } finally {
                reopened.close()
            }
        } finally {
            deleteDatabase(name)
        }
    }

    @Test
    fun concurrentStartsAcrossDatabaseInstancesReturnOneSession() = runTest {
        withDatabase("concurrent-start.db") { database ->
            val second = openDatabase("concurrent-start.db")
            try {
                val results = listOf(
                    async(Dispatchers.Default) {
                        RoomWorkoutRepository(database.unfinishedWorkoutDao()) { 100L }
                            .startWorkout(Workout.A)
                    },
                    async(Dispatchers.Default) {
                        RoomWorkoutRepository(second.unfinishedWorkoutDao()) { 200L }
                            .startWorkout(Workout.B)
                    },
                ).awaitAll()
                assertEquals(results.first(), results.last())
                assertProgram(results.first().workout, results.first())
                assertEquals(3, database.unfinishedWorkoutDao().getExercises().size)
            } finally {
                second.close()
            }
        }
    }

    @Test
    fun failedExerciseInsertRollsBackSession() = runTest {
        withDatabase("failed-start.db") { database ->
            val dao = database.unfinishedWorkoutDao()
            assertFails {
                dao.insertSessionIfAbsent(
                    UnfinishedWorkoutEntity(Workout.A, 100L),
                    listOf(UnfinishedWorkoutExerciseEntity(2, 0, Exercise.SQUAT, 5, 5, 20.0)),
                )
            }
            assertNull(dao.getSession())
            assertEquals(emptyList(), dao.getExercises())
        }
    }

    @Test
    fun loadReturnsNullWithoutCreatingSession() = runTest {
        withDatabase("load-empty.db") { database ->
            val dao = database.unfinishedWorkoutDao()
            val repository = RoomWorkoutRepository(dao) { error("Loading must not read the clock") }
            assertNull(repository.getUnfinishedWorkout())
            assertNull(repository.getUnfinishedWorkout())
            assertNull(dao.getUnfinishedWorkout())
            assertEquals(emptyList(), dao.getExercises())
        }
    }

    @Test
    fun loadReturnsCreatedSessionWithAllPrescriptionsAfterReopen() = runTest {
        for (workout in Workout.entries) {
            val name = "load-${workout.name}.db"
            deleteDatabase(name)
            try {
                val database = openDatabase(name)
                val created = try {
                    RoomWorkoutRepository(database.unfinishedWorkoutDao()) { 1234L }
                        .startWorkout(workout)
                } finally {
                    database.close()
                }
                val reopened = openDatabase(name)
                try {
                    val repository = RoomWorkoutRepository(reopened.unfinishedWorkoutDao()) {
                        error("Loading must not read the clock")
                    }
                    val loaded = requireNotNull(repository.getUnfinishedWorkout())
                    assertEquals(created, loaded)
                    assertEquals(workout, loaded.workout)
                    assertEquals(1234L, loaded.startedAtEpochMillis)
                    assertProgram(workout, loaded)
                    assertEquals(loaded, repository.getUnfinishedWorkout())
                } finally {
                    reopened.close()
                }
            } finally {
                deleteDatabase(name)
            }
        }
    }

    @Test
    fun loadUsesStoredPrescriptionsAndPositionRatherThanProgramDefaultsOrInsertOrder() = runTest {
        withDatabase("load-snapshot.db") { database ->
            val dao = database.unfinishedWorkoutDao()
            // Distinct persisted values prove loading maps the snapshot, not today's program defaults.
            dao.insertSessionIfAbsent(
                UnfinishedWorkoutEntity(Workout.B, 5678L),
                listOf(
                    UnfinishedWorkoutExerciseEntity(1, 2, Exercise.DEADLIFT, 1, 4, 60.0),
                    UnfinishedWorkoutExerciseEntity(1, 0, Exercise.SQUAT, 3, 5, 40.0),
                    UnfinishedWorkoutExerciseEntity(1, 1, Exercise.OVERHEAD_PRESS, 5, 3, 25.0),
                ),
            )
            val before = dao.getSession()
            val loaded = RoomWorkoutRepository(dao).getUnfinishedWorkout()
            assertEquals(
                UnfinishedWorkout(
                    Workout.B,
                    5678L,
                    listOf(
                        UnfinishedWorkoutExercise(Exercise.SQUAT, 3, 5, 40.0),
                        UnfinishedWorkoutExercise(Exercise.OVERHEAD_PRESS, 5, 3, 25.0),
                        UnfinishedWorkoutExercise(Exercise.DEADLIFT, 1, 4, 60.0),
                    ),
                ),
                loaded,
            )
            assertEquals(before, dao.getSession())
        }
    }

    private suspend fun withDatabase(name: String, block: suspend (WorkoutDatabase) -> Unit) {
        deleteDatabase(name)
        try {
            val database = openDatabase(name)
            try {
                block(database)
            } finally {
                database.close()
            }
        } finally {
            deleteDatabase(name)
        }
    }

    private fun assertProgram(workout: Workout, session: UnfinishedWorkout) {
        val expected = when (workout) {
            Workout.A -> listOf(Exercise.SQUAT, Exercise.BENCH_PRESS, Exercise.BARBELL_ROW)
            Workout.B -> listOf(Exercise.SQUAT, Exercise.OVERHEAD_PRESS, Exercise.DEADLIFT)
        }
        assertEquals(expected, session.exercises.map { it.exercise })
        assertEquals(if (workout == Workout.A) listOf(5, 5, 5) else listOf(5, 5, 1), session.exercises.map { it.sets })
        assertEquals(listOf(5, 5, 5), session.exercises.map { it.reps })
        assertEquals(listOf(20.0, 20.0, 20.0), session.exercises.map { it.weightKg })
    }

}
