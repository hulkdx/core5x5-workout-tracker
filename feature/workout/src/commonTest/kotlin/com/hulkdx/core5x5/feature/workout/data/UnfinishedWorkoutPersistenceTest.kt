package com.hulkdx.core5x5.feature.workout.data

import com.hulkdx.core5x5.feature.workout.domain.Exercise
import com.hulkdx.core5x5.feature.workout.domain.CompletedWorkout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutExercise
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutSet
import com.hulkdx.core5x5.feature.workout.domain.Workout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
                    id = 1L,
                )
                val database = openDatabase(name)
                try {
                    val dao = database.unfinishedWorkoutDao()
                    assertNull(dao.getUnfinishedWorkout())
                    dao.insert(expected)
                    assertEquals(expected, dao.getUnfinishedWorkout())
                    assertFails {
                        dao.insert(expected.copy(id = 2L, startedAtEpochMillis = 42L))
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
                assertEquals(3, dao.getExercises(original.id).size)
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
                assertEquals(3, database.unfinishedWorkoutDao().getExercises(results.first().id).size)
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
                    listOf(
                        UnfinishedWorkoutExerciseEntity(0, 0, Exercise.SQUAT, 5, 5, 20.0),
                        UnfinishedWorkoutExerciseEntity(0, 0, Exercise.BENCH_PRESS, 5, 5, 20.0),
                    ),
                )
            }
            assertNull(dao.getSession())
            assertEquals(emptyList(), dao.getExercises(1L))
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
            assertEquals(emptyList(), dao.getExercises(1L))
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
                    id = 1L,
                ),
                loaded,
            )
            assertEquals(before, dao.getSession())
        }
    }

    @Test
    fun setCanBeMarkedCompletedWithoutChangingOtherSetsOrPrescriptions() = runTest {
        withDatabase("complete-set.db") { database ->
            val repository = RoomWorkoutRepository(database.unfinishedWorkoutDao()) { 1234L }
            val original = repository.startWorkout(Workout.A)

            assertTrue(repository.setSetCompleted(1, 2, true))

            assertEquals(
                original.copy(exercises = listOf(
                    original.exercises[0],
                    original.exercises[1].copy(setStates = List(5) { UnfinishedWorkoutSet(it, it == 2) }),
                    original.exercises[2],
                )),
                repository.getUnfinishedWorkout(),
            )
        }
    }

    @Test
    fun completedSetCanBeChangedBackToIncompleteWithoutResettingOtherCompletedSets() = runTest {
        withDatabase("uncomplete-set.db") { database ->
            val repository = RoomWorkoutRepository(database.unfinishedWorkoutDao()) { 1234L }
            val original = repository.startWorkout(Workout.A)
            assertTrue(repository.setSetCompleted(0, 2, true))
            assertTrue(repository.setSetCompleted(1, 0, true))

            assertTrue(repository.setSetCompleted(0, 2, false))

            assertEquals(
                original.copy(exercises = listOf(
                    original.exercises[0],
                    original.exercises[1].copy(setStates = List(5) { UnfinishedWorkoutSet(it, it == 0) }),
                    original.exercises[2],
                )),
                repository.getUnfinishedWorkout(),
            )
        }
    }

    @Test
    fun completionStateSurvivesRepositoryAndDatabaseRecreationAndRepeatedStart() = runTest {
        for (workout in Workout.entries) {
            val name = "set-reopen-${workout.name}.db"
            deleteDatabase(name)
            try {
                val database = openDatabase(name)
                val saved = try {
                    val dao = database.unfinishedWorkoutDao()
                    val repository = RoomWorkoutRepository(dao) { 1234L }
                    repository.startWorkout(workout)
                    assertTrue(repository.setSetCompleted(0, 4, true))
                    assertTrue(repository.setSetCompleted(1, 1, true))
                    assertTrue(repository.setSetCompleted(1, 1, false))
                    assertTrue(repository.setSetCompleted(2, 0, true))
                    val session = requireNotNull(repository.getUnfinishedWorkout())
                    assertEquals(listOf(false, false, false, false, true), session.exercises[0].setStates.map { it.isCompleted })
                    assertEquals(List(5) { false }, session.exercises[1].setStates.map { it.isCompleted })
                    assertTrue(session.exercises[2].setStates[0].isCompleted)
                    assertEquals(session, RoomWorkoutRepository(dao).getUnfinishedWorkout())
                    session
                } finally {
                    database.close()
                }
                val reopened = openDatabase(name)
                try {
                    val repository = RoomWorkoutRepository(reopened.unfinishedWorkoutDao()) { 5678L }
                    assertEquals(saved, repository.getUnfinishedWorkout())
                    val otherWorkout = if (workout == Workout.A) Workout.B else Workout.A
                    assertEquals(saved, repository.startWorkout(otherWorkout))
                    assertEquals(saved.exercises.sumOf { it.sets }, reopened.unfinishedWorkoutDao().getSets(saved.id).size)
                } finally {
                    reopened.close()
                }
            } finally {
                deleteDatabase(name)
            }
        }
    }

    @Test
    fun repeatedCompletionWritesKeepTheRequestedState() = runTest {
        withDatabase("repeat-set.db") { database ->
            val repository = RoomWorkoutRepository(database.unfinishedWorkoutDao()) { 1234L }
            val original = repository.startWorkout(Workout.B)
            assertTrue(repository.setSetCompleted(2, 0, true))
            val completed = repository.getUnfinishedWorkout()
            assertTrue(repository.setSetCompleted(2, 0, true))
            assertEquals(completed, repository.getUnfinishedWorkout())
            assertTrue(repository.setSetCompleted(2, 0, false))
            assertTrue(repository.setSetCompleted(2, 0, false))
            assertEquals(original, repository.getUnfinishedWorkout())
        }
    }

    @Test
    fun missingSetsCannotBeCompletedAndDoNotCreateOrChangeASession() = runTest {
        withDatabase("missing-set.db") { database ->
            val repository = RoomWorkoutRepository(database.unfinishedWorkoutDao()) { 1234L }
            assertFalse(repository.setSetCompleted(0, 0, true))
            assertNull(repository.getUnfinishedWorkout())
            val original = repository.startWorkout(Workout.B)
            for ((exercisePosition, setPosition) in listOf(-1 to 0, 3 to 0, 0 to -1, 0 to 5, 2 to 1)) {
                assertFalse(repository.setSetCompleted(exercisePosition, setPosition, true))
                assertFalse(repository.setSetCompleted(exercisePosition, setPosition, false))
            }
            assertEquals(original, repository.getUnfinishedWorkout())
        }
    }

    @Test
    fun concurrentUpdatesAcrossDatabaseInstancesPreserveBothSets() = runTest {
        withDatabase("concurrent-sets.db") { database ->
            val repository = RoomWorkoutRepository(database.unfinishedWorkoutDao()) { 1234L }
            val original = repository.startWorkout(Workout.A)
            val second = openDatabase("concurrent-sets.db")
            try {
                val secondRepository = RoomWorkoutRepository(second.unfinishedWorkoutDao())
                val results = listOf(
                    async(Dispatchers.Default) { repository.setSetCompleted(0, 0, true) },
                    async(Dispatchers.Default) { secondRepository.setSetCompleted(0, 1, true) },
                ).awaitAll()
                assertEquals(listOf(true, true), results)
                assertEquals(
                    original.copy(exercises = listOf(
                        original.exercises[0].copy(setStates = List(5) { UnfinishedWorkoutSet(it, it < 2) }),
                        original.exercises[1],
                        original.exercises[2],
                    )),
                    repository.getUnfinishedWorkout(),
                )
            } finally {
                second.close()
            }
        }
    }

    @Test
    fun finalizingActiveWorkoutPersistsItsCompletionTimestamp() = runTest {
        for (workout in Workout.entries) {
            withDatabase("finalize-${workout.name}.db") { database ->
                val dao = database.unfinishedWorkoutDao()
                val active = RoomWorkoutRepository(dao) { 1234L }.startWorkout(workout)
                val before = requireNotNull(dao.getWorkout(active.id))
                assertNull(before.completedAtEpochMillis)
                assertEquals(1, before.unfinishedSlot)

                assertTrue(RoomWorkoutRepository(dao) { 5678L }.finalizeWorkout(active.id))

                assertEquals(
                    before.copy(completedAtEpochMillis = 5678L, unfinishedSlot = null),
                    dao.getWorkout(active.id),
                )
            }
        }
    }

    @Test
    fun completedReadRejectsMissingAndUnfinishedIdsWithoutWritingOrReadingTheClock() = runTest {
        withDatabase("completed-read-empty.db") { database ->
            val dao = database.unfinishedWorkoutDao()
            val reader = RoomWorkoutRepository(dao) { error("Completed reads must not consult the clock") }
            assertNull(reader.getCompletedWorkout(1L))
            val active = RoomWorkoutRepository(dao) { 1234L }.startWorkout(Workout.B)
            assertNull(reader.getCompletedWorkout(active.id))
            assertNull(reader.getCompletedWorkout(Long.MAX_VALUE))
            assertEquals(active, reader.getUnfinishedWorkout())
        }
    }

    @Test
    fun completedSummaryReloadsItsOriginalSessionAndLoggedSetsAfterReopening() = runTest {
        for (workout in Workout.entries) {
            val name = "completed-summary-${workout.name}.db"
            deleteDatabase(name)
            try {
                val database = openDatabase(name)
                val expected = try {
                    val dao = database.unfinishedWorkoutDao()
                    val active = RoomWorkoutRepository(dao) { 1000L }.startWorkout(workout)
                    val repository = RoomWorkoutRepository(dao) { 2_539_000L }
                    assertTrue(repository.setSetCompleted(0, 0, true))
                    assertTrue(repository.setSetCompleted(2, 0, true))
                    val logged = requireNotNull(repository.getUnfinishedWorkout())
                    assertTrue(repository.finalizeWorkout(active.id))
                    val saved = requireNotNull(repository.getCompletedWorkout(active.id))
                    assertEquals(
                        CompletedWorkout(active.id, workout, 1000L, 2_539_000L, logged.exercises),
                        saved,
                    )
                    assertEquals(2, saved.completedSets)
                    assertEquals(if (workout == Workout.A) 15 else 11, saved.prescribedSets)
                    assertEquals(2_538_000L, saved.durationMillis)
                    saved
                } finally {
                    database.close()
                }
                val reopened = openDatabase(name)
                try {
                    val dao = reopened.unfinishedWorkoutDao()
                    val reader = RoomWorkoutRepository(dao) { error("Summary reads do not consult the clock") }
                    assertEquals(expected, reader.getCompletedWorkout(expected.id))
                    val later = RoomWorkoutRepository(dao) { 3_000_000L }.startWorkout(workout.nextWorkout())
                    assertEquals(expected, reader.getCompletedWorkout(expected.id))
                    assertNull(reader.getCompletedWorkout(later.id))
                    assertEquals(later, reader.getUnfinishedWorkout())
                } finally {
                    reopened.close()
                }
            } finally {
                deleteDatabase(name)
            }
        }
    }

    @Test
    fun completedSummaryUsesSavedNonDefaultPrescriptionsInPersistedOrder() = runTest {
        withDatabase("completed-summary-snapshots.db") { database ->
            val dao = database.unfinishedWorkoutDao()
            val stored = dao.insertSessionIfAbsent(
                UnfinishedWorkoutEntity(Workout.B, 1234L),
                listOf(
                    UnfinishedWorkoutExerciseEntity(0, 2, Exercise.DEADLIFT, 1, 4, 60.0),
                    UnfinishedWorkoutExerciseEntity(0, 0, Exercise.SQUAT, 3, 5, 40.0),
                    UnfinishedWorkoutExerciseEntity(0, 1, Exercise.OVERHEAD_PRESS, 5, 3, 25.0),
                ),
            )
            val repository = RoomWorkoutRepository(dao) { 5678L }
            assertTrue(repository.setSetCompleted(0, 1, true))
            val logged = requireNotNull(repository.getUnfinishedWorkout())
            assertTrue(repository.finalizeWorkout(stored.session.id))

            val saved = requireNotNull(repository.getCompletedWorkout(stored.session.id))
            assertEquals(logged.exercises, saved.exercises)
            assertEquals(listOf(Exercise.SQUAT, Exercise.OVERHEAD_PRESS, Exercise.DEADLIFT), saved.exercises.map { it.exercise })
            assertEquals(listOf(40.0, 25.0, 60.0), saved.exercises.map { it.weightKg })
            assertEquals(9, saved.prescribedSets)
            assertEquals(1, saved.completedSets)
        }
    }

    @Test
    fun nextPrescriptionBeforeAnyCompletionUsesCanonicalDefaultsAndDoesNotCreateASession() = runTest {
        withDatabase("next-prescription-empty.db") { database ->
            val repository = RoomWorkoutRepository(database.unfinishedWorkoutDao()) {
                error("Prescription reads do not consult the clock")
            }
            val prescription = repository.getNextWorkoutPrescription()
            assertEquals(Workout.A, prescription.workout)
            assertEquals(Workout.A.exercises, prescription.exercises.map { it.exercise })
            assertEquals(listOf(5, 5, 5), prescription.exercises.map { it.sets })
            assertEquals(listOf(5, 5, 5), prescription.exercises.map { it.reps })
            assertEquals(listOf(20.0, 20.0, 20.0), prescription.exercises.map { it.weightKg })
            assertNull(repository.getUnfinishedWorkout())
        }
    }

    @Test
    fun nextPrescriptionCarriesEachLiftsLatestSavedWeightAcrossAAndBAndIntoTheNextStart() = runTest {
        val name = "next-prescription-weights.db"
        deleteDatabase(name)
        try {
            val database = openDatabase(name)
            val expected = try {
                val dao = database.unfinishedWorkoutDao()
                val first = dao.insertSessionIfAbsent(
                    UnfinishedWorkoutEntity(Workout.A, 100L),
                    listOf(
                        UnfinishedWorkoutExerciseEntity(0, 0, Exercise.SQUAT, 5, 5, 40.0),
                        UnfinishedWorkoutExerciseEntity(0, 1, Exercise.BENCH_PRESS, 5, 5, 27.5),
                        UnfinishedWorkoutExerciseEntity(0, 2, Exercise.BARBELL_ROW, 5, 5, 35.0),
                    ),
                )
                assertTrue(RoomWorkoutRepository(dao) { 500L }.finalizeWorkout(first.session.id))
                val nextB = RoomWorkoutRepository(dao).getNextWorkoutPrescription()
                assertEquals(Workout.B, nextB.workout)
                assertEquals(listOf(40.0, 20.0, 20.0), nextB.exercises.map { it.weightKg })
                assertEquals(listOf(5, 5, 1), nextB.exercises.map { it.sets })

                val second = dao.insertSessionIfAbsent(
                    UnfinishedWorkoutEntity(Workout.B, 200L),
                    listOf(
                        UnfinishedWorkoutExerciseEntity(0, 0, Exercise.SQUAT, 5, 5, 45.0),
                        UnfinishedWorkoutExerciseEntity(0, 1, Exercise.OVERHEAD_PRESS, 5, 5, 32.5),
                        UnfinishedWorkoutExerciseEntity(0, 2, Exercise.DEADLIFT, 1, 5, 60.0),
                    ),
                )
                // A backward clock and partial completion must not choose older weights or increase them.
                val repository = RoomWorkoutRepository(dao) { 400L }
                assertTrue(repository.setSetCompleted(0, 0, true))
                assertTrue(repository.finalizeWorkout(second.session.id))
                val nextA = repository.getNextWorkoutPrescription()
                assertEquals(Workout.A, nextA.workout)
                assertEquals(listOf(45.0, 27.5, 35.0), nextA.exercises.map { it.weightKg })
                val started = repository.startWorkout(nextA.workout)
                assertEquals(nextA.exercises.map { it.exercise }, started.exercises.map { it.exercise })
                assertEquals(nextA.exercises.map { it.weightKg }, started.exercises.map { it.weightKg })
                assertTrue(started.exercises.all { exercise -> exercise.setStates.none { it.isCompleted } })
                assertEquals(nextA, repository.getNextWorkoutPrescription())
                nextA
            } finally {
                database.close()
            }
            val reopened = openDatabase(name)
            try {
                val reader = RoomWorkoutRepository(reopened.unfinishedWorkoutDao()) {
                    error("Restored prescription reads do not consult the clock")
                }
                assertEquals(expected, reader.getNextWorkoutPrescription())
            } finally {
                reopened.close()
            }
        } finally {
            deleteDatabase(name)
        }
    }

    @Test
    fun nextPrescriptionIgnoresWeightsFromAnUnfinishedSession() = runTest {
        withDatabase("next-prescription-unfinished.db") { database ->
            val dao = database.unfinishedWorkoutDao()
            val active = dao.insertSessionIfAbsent(
                UnfinishedWorkoutEntity(Workout.B, 1234L),
                Workout.B.exercises.mapIndexed { position, exercise ->
                    UnfinishedWorkoutExerciseEntity(0, position, exercise, exercise.sets, exercise.reps, 100.0)
                },
            )
            val repository = RoomWorkoutRepository(dao) { error("Prescription reads do not consult the clock") }
            val before = dao.getSession(active.session.id)
            assertEquals(Workout.A, repository.getNextWorkoutPrescription().workout)
            assertEquals(listOf(20.0, 20.0, 20.0), repository.getNextWorkoutPrescription().exercises.map { it.weightKg })
            assertEquals(before, dao.getSession(active.session.id))
        }
    }

    @Test
    fun completedWorkoutIsNoLongerActiveEvenAfterDatabaseReopen() = runTest {
        val name = "finalize-reopen.db"
        deleteDatabase(name)
        try {
            val database = openDatabase(name)
            val workoutId = try {
                val repository = RoomWorkoutRepository(database.unfinishedWorkoutDao()) { 1234L }
                val active = repository.startWorkout(Workout.A)
                assertTrue(repository.finalizeWorkout(active.id))
                assertNull(repository.getUnfinishedWorkout())
                assertNull(database.unfinishedWorkoutDao().getSession())
                active.id
            } finally {
                database.close()
            }
            val reopened = openDatabase(name)
            try {
                val dao = reopened.unfinishedWorkoutDao()
                val repository = RoomWorkoutRepository(dao) { error("Loading must not read the clock") }
                assertNull(repository.getUnfinishedWorkout())
                assertNull(dao.getUnfinishedWorkout())
                assertEquals(1234L, requireNotNull(dao.getWorkout(workoutId)).completedAtEpochMillis)
            } finally {
                reopened.close()
            }
        } finally {
            deleteDatabase(name)
        }
    }

    @Test
    fun finalizationPreservesLoggedSetsAndSavedPrescriptionsAfterReopen() = runTest {
        val name = "finalize-sets.db"
        deleteDatabase(name)
        try {
            val database = openDatabase(name)
            val expected = try {
                val dao = database.unfinishedWorkoutDao()
                dao.insertSessionIfAbsent(
                    UnfinishedWorkoutEntity(Workout.B, 1234L),
                    listOf(
                        UnfinishedWorkoutExerciseEntity(0, 2, Exercise.DEADLIFT, 1, 4, 60.0),
                        UnfinishedWorkoutExerciseEntity(0, 0, Exercise.SQUAT, 3, 5, 40.0),
                        UnfinishedWorkoutExerciseEntity(0, 1, Exercise.OVERHEAD_PRESS, 5, 3, 25.0),
                    ),
                )
                val repository = RoomWorkoutRepository(dao) { 5678L }
                assertTrue(repository.setSetCompleted(0, 0, true))
                assertTrue(repository.setSetCompleted(1, 2, true))
                assertTrue(repository.setSetCompleted(1, 2, false))
                assertTrue(repository.setSetCompleted(2, 0, true))
                val before = requireNotNull(dao.getSession())
                assertEquals(listOf(true, false, false), before.sets.filter { it.exercisePosition == 0 }.map { it.isCompleted })
                assertEquals(List(5) { false }, before.sets.filter { it.exercisePosition == 1 }.map { it.isCompleted })
                assertTrue(before.sets.last().isCompleted)

                assertTrue(repository.finalizeWorkout(before.session.id))

                val completed = before.copy(
                    session = before.session.copy(completedAtEpochMillis = 5678L, unfinishedSlot = null),
                )
                assertEquals(completed, dao.getSession(before.session.id))
                assertFalse(repository.setSetCompleted(0, 0, false))
                assertEquals(completed, dao.getSession(before.session.id))
                completed
            } finally {
                database.close()
            }
            val reopened = openDatabase(name)
            try {
                assertEquals(expected, reopened.unfinishedWorkoutDao().getSession(expected.session.id))
            } finally {
                reopened.close()
            }
        } finally {
            deleteDatabase(name)
        }
    }

    @Test
    fun repeatedFinalizationDoesNotChangeCompletionOrFinalizeALaterWorkout() = runTest {
        withDatabase("repeat-finalize.db") { database ->
            val dao = database.unfinishedWorkoutDao()
            val repository = RoomWorkoutRepository(dao) { 1234L }
            val original = repository.startWorkout(Workout.A)
            assertTrue(repository.setSetCompleted(0, 0, true))
            assertTrue(RoomWorkoutRepository(dao) { 5678L }.finalizeWorkout(original.id))
            val completed = requireNotNull(dao.getSession(original.id))

            assertFalse(RoomWorkoutRepository(dao) { 9000L }.finalizeWorkout(original.id))
            assertEquals(completed, dao.getSession(original.id))
            assertNull(repository.getUnfinishedWorkout())

            val second = openDatabase("repeat-finalize.db")
            try {
                val secondRepository = RoomWorkoutRepository(second.unfinishedWorkoutDao()) { 10000L }
                assertFalse(secondRepository.finalizeWorkout(original.id))
                assertEquals(completed, second.unfinishedWorkoutDao().getSession(original.id))
                val next = secondRepository.startWorkout(Workout.A)
                assertTrue(next.id != original.id)
                assertProgram(Workout.A, next)

                assertFalse(secondRepository.finalizeWorkout(original.id))
                assertEquals(next, secondRepository.getUnfinishedWorkout())
                assertTrue(secondRepository.setSetCompleted(1, 0, true))
                assertEquals(completed, dao.getSession(original.id))
                assertTrue(secondRepository.finalizeWorkout(next.id))
                assertEquals(completed, dao.getSession(original.id))
                assertEquals(10000L, requireNotNull(dao.getWorkout(next.id)).completedAtEpochMillis)
                assertNull(secondRepository.getUnfinishedWorkout())
            } finally {
                second.close()
            }
        }
    }

    @Test
    fun concurrentFinalizationAcrossDatabaseInstancesCompletesOnlyOnce() = runTest {
        withDatabase("concurrent-finalize.db") { database ->
            val dao = database.unfinishedWorkoutDao()
            val repository = RoomWorkoutRepository(dao) { 1234L }
            val active = repository.startWorkout(Workout.B)
            assertTrue(repository.setSetCompleted(2, 0, true))
            val before = requireNotNull(dao.getSession())
            val second = openDatabase("concurrent-finalize.db")
            try {
                val results = listOf(
                    async(Dispatchers.Default) { RoomWorkoutRepository(dao) { 5678L }.finalizeWorkout(active.id) },
                    async(Dispatchers.Default) {
                        RoomWorkoutRepository(second.unfinishedWorkoutDao()) { 9000L }.finalizeWorkout(active.id)
                    },
                ).awaitAll()

                assertEquals(1, results.count { it })
                val timestamp = if (results.first()) 5678L else 9000L
                assertEquals(
                    before.copy(session = before.session.copy(completedAtEpochMillis = timestamp, unfinishedSlot = null)),
                    dao.getSession(active.id),
                )
                assertNull(repository.getUnfinishedWorkout())
                assertEquals(Workout.A, repository.getNextWorkout())
                assertEquals(Workout.A, RoomWorkoutRepository(second.unfinishedWorkoutDao()).getNextWorkout())
            } finally {
                second.close()
            }
        }
    }

    @Test
    fun missingWorkoutCannotBeFinalizedAndLeavesTheActiveSessionUnchanged() = runTest {
        withDatabase("missing-finalize.db") { database ->
            val repository = RoomWorkoutRepository(database.unfinishedWorkoutDao()) { 1234L }
            assertFalse(repository.finalizeWorkout(1L))
            assertNull(repository.getUnfinishedWorkout())
            val active = repository.startWorkout(Workout.A)
            assertFalse(repository.finalizeWorkout(Long.MAX_VALUE))
            assertEquals(active, repository.getUnfinishedWorkout())
        }
    }

    @Test
    fun nextWorkoutStartsWithAWithoutWritingOrReadingTheClock() = runTest {
        withDatabase("next-empty.db") { database ->
            val dao = database.unfinishedWorkoutDao()
            val repository = RoomWorkoutRepository(dao) { error("Selection must not read the clock") }
            assertEquals(Workout.A, repository.getNextWorkout())
            assertEquals(Workout.A, repository.getNextWorkout())
            assertNull(dao.getLastCompletedWorkout())
            assertNull(dao.getUnfinishedWorkout())
            assertNull(dao.getWorkout(1L))
        }
    }

    @Test
    fun completingSuccessiveSessionsAlternatesTheNextProgram() = runTest {
        withDatabase("alternating-sessions.db") { database ->
            val repository = RoomWorkoutRepository(database.unfinishedWorkoutDao()) { 1234L }
            for (expected in listOf(Workout.A, Workout.B, Workout.A, Workout.B)) {
                val selected = repository.getNextWorkout()
                assertEquals(expected, selected)
                val active = repository.startWorkout(selected)
                assertProgram(expected, active)
                assertEquals(expected, repository.getNextWorkout())
                assertTrue(repository.finalizeWorkout(active.id))
                assertEquals(expected.nextWorkout(), repository.getNextWorkout())
                assertEquals(expected.nextWorkout(), repository.getNextWorkout())
                assertNull(repository.getUnfinishedWorkout())
            }
        }
    }

    @Test
    fun startingAndCompletingSetsDoNotAdvanceNextWorkout() = runTest {
        withDatabase("next-unfinished.db") { database ->
            val repository = RoomWorkoutRepository(database.unfinishedWorkoutDao()) { 1234L }
            // Selection follows completed history, not the chosen program or the number of starts.
            val active = repository.startWorkout(Workout.B)
            assertEquals(Workout.A, repository.getNextWorkout())
            assertEquals(active, repository.startWorkout(Workout.A))
            for ((exercisePosition, exercise) in active.exercises.withIndex()) {
                for (setPosition in 0 until exercise.sets) {
                    assertTrue(repository.setSetCompleted(exercisePosition, setPosition, true))
                }
            }
            assertEquals(Workout.A, repository.getNextWorkout())
            assertEquals(Workout.B, repository.getUnfinishedWorkout()?.workout)
        }
    }

    @Test
    fun nextWorkoutAfterEitherCompletedProgramSurvivesDatabaseReopen() = runTest {
        for (completed in Workout.entries) {
            val name = "next-reopen-${completed.name}.db"
            deleteDatabase(name)
            try {
                val database = openDatabase(name)
                try {
                    val repository = RoomWorkoutRepository(database.unfinishedWorkoutDao()) { 1234L }
                    val active = repository.startWorkout(completed)
                    assertTrue(repository.setSetCompleted(0, 0, true))
                    assertTrue(repository.finalizeWorkout(active.id))
                    assertEquals(completed.nextWorkout(), repository.getNextWorkout())
                } finally {
                    database.close()
                }
                val reopened = openDatabase(name)
                try {
                    val repository = RoomWorkoutRepository(reopened.unfinishedWorkoutDao()) {
                        error("Selection must not read the clock")
                    }
                    assertEquals(completed.nextWorkout(), repository.getNextWorkout())
                    assertNull(repository.getUnfinishedWorkout())
                } finally {
                    reopened.close()
                }
            } finally {
                deleteDatabase(name)
            }
        }
    }

    @Test
    fun selectionUsesSessionOrderWhenCompletionTimesRepeatOrMoveBackwards() = runTest {
        withDatabase("next-clock-change.db") { database ->
            var now = 5000L
            val repository = RoomWorkoutRepository(database.unfinishedWorkoutDao()) { now }
            val first = repository.startWorkout(Workout.A)
            assertTrue(repository.finalizeWorkout(first.id))
            assertEquals(Workout.B, repository.getNextWorkout())

            val second = repository.startWorkout(Workout.B)
            assertTrue(repository.finalizeWorkout(second.id))
            assertEquals(Workout.A, repository.getNextWorkout())

            now = 1000L
            val third = repository.startWorkout(Workout.A)
            assertTrue(repository.finalizeWorkout(third.id))
            assertEquals(Workout.B, repository.getNextWorkout())
        }
    }

    @Test
    fun repeatedAndMissingFinalizationDoNotAdvanceSelectionAgain() = runTest {
        withDatabase("next-finalize-retries.db") { database ->
            val repository = RoomWorkoutRepository(database.unfinishedWorkoutDao()) { 1234L }
            assertFalse(repository.finalizeWorkout(Long.MAX_VALUE))
            assertEquals(Workout.A, repository.getNextWorkout())
            val first = repository.startWorkout(Workout.A)
            assertFalse(repository.finalizeWorkout(Long.MAX_VALUE))
            assertEquals(Workout.A, repository.getNextWorkout())
            assertTrue(repository.finalizeWorkout(first.id))
            assertEquals(Workout.B, repository.getNextWorkout())
            assertFalse(repository.finalizeWorkout(first.id))
            assertEquals(Workout.B, repository.getNextWorkout())

            val second = repository.startWorkout(repository.getNextWorkout())
            assertFalse(repository.finalizeWorkout(first.id))
            assertEquals(second, repository.getUnfinishedWorkout())
            assertEquals(Workout.B, repository.getNextWorkout())
            assertTrue(repository.finalizeWorkout(second.id))
            assertEquals(Workout.A, repository.getNextWorkout())
            assertFalse(repository.finalizeWorkout(first.id))
            assertFalse(repository.finalizeWorkout(second.id))
            assertEquals(Workout.A, repository.getNextWorkout())
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
        session.exercises.forEach { exercise ->
            assertEquals(List(exercise.sets) { UnfinishedWorkoutSet(it) }, exercise.setStates)
        }
    }

}
