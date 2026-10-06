package com.hulkdx.core5x5.feature.workout.presentation

import androidx.lifecycle.ViewModelStore
import com.hulkdx.core5x5.feature.workout.domain.CompletedWorkout
import com.hulkdx.core5x5.feature.workout.domain.Exercise
import com.hulkdx.core5x5.feature.workout.domain.RestTimer
import com.hulkdx.core5x5.feature.workout.domain.RestTimerRules
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutExercise
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.feature.workout.domain.WorkoutPrescription
import com.hulkdx.core5x5.feature.workout.domain.WorkoutRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class ActiveWorkoutViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun loadsTheSavedWorkoutAndPreservesExerciseOrderAndPrescriptions() = runTest(dispatcher) {
        val savedWorkout = UnfinishedWorkout(
            workout = Workout.B,
            startedAtEpochMillis = 123L,
            exercises = listOf(
                UnfinishedWorkoutExercise(Exercise.SQUAT, sets = 5, reps = 5, weightKg = 22.5),
                UnfinishedWorkoutExercise(Exercise.OVERHEAD_PRESS, sets = 5, reps = 5, weightKg = 20.0),
                UnfinishedWorkoutExercise(Exercise.DEADLIFT, sets = 1, reps = 5, weightKg = 25.0),
            ),
            id = 1L,
        )
        val repository = FakeWorkoutRepository(savedWorkout)
        val viewModel = ActiveWorkoutViewModel(repository).also { store.put("active-workout", it) }
        assertTrue(viewModel.uiState.value.isLoading)

        dispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.hasLoadError)
        assertSame(savedWorkout, state.unfinishedWorkout)
        assertEquals(1, repository.readCount)
        assertEquals(
            listOf(
                Exercise.SQUAT to (5 to (5 to 22.5)),
                Exercise.OVERHEAD_PRESS to (5 to (5 to 20.0)),
                Exercise.DEADLIFT to (1 to (5 to 25.0)),
            ),
            state.unfinishedWorkout?.exercises?.map { it.exercise to (it.sets to (it.reps to it.weightKg)) },
        )
    }

    @Test
    fun finishWaitsForThePersistedCompletedReadBeforeRequestingNavigation() = runTest(dispatcher) {
        val repository = FakeWorkoutRepository(session(Workout.B, id = 42L))
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        repository.completedReadGate = CompletableDeferred()

        viewModel.finishWorkout()
        viewModel.finishWorkout()
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.isSaving)
        assertNull(viewModel.uiState.value.requestedCompletedWorkoutId)
        assertEquals(listOf(42L), repository.finalizedIds)
        assertEquals(listOf(42L), repository.completedReadIds)

        repository.completedReadGate?.complete(Unit)
        dispatcher.scheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isSaving)
        assertFalse(viewModel.uiState.value.hasSaveError)
        assertEquals(42L, viewModel.uiState.value.requestedCompletedWorkoutId)
        assertFalse(viewModel.uiState.value.canFinish)
        viewModel.finishWorkout()
        assertEquals(listOf(42L), repository.finalizedIds)
        viewModel.onCompletionRequestHandled()
        assertNull(viewModel.uiState.value.requestedCompletedWorkoutId)
    }

    @Test
    fun failedSaveKeepsTheSessionAndRetriesTheSameId() = runTest(dispatcher) {
        val original = session(Workout.A)
        val repository = FakeWorkoutRepository(original)
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        repository.saveError = IllegalStateException("Disk unavailable")

        viewModel.finishWorkout()
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.hasSaveError)
        assertTrue(viewModel.uiState.value.canFinish)
        assertSame(original, viewModel.uiState.value.unfinishedWorkout)
        assertNull(viewModel.uiState.value.requestedCompletedWorkoutId)

        repository.saveError = null
        viewModel.finishWorkout()
        dispatcher.scheduler.runCurrent()

        assertFalse(viewModel.uiState.value.hasSaveError)
        assertEquals(original.id, viewModel.uiState.value.requestedCompletedWorkoutId)
        assertEquals(listOf(original.id, original.id), repository.finalizedIds)
        assertEquals(1, repository.saveCount)
    }

    @Test
    fun retryAfterACommittedSaveWithLostAcknowledgementDoesNotSaveAgain() = runTest(dispatcher) {
        val original = session(Workout.A)
        val repository = FakeWorkoutRepository(original)
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        repository.afterSaveError = IllegalStateException("Acknowledgement lost")

        viewModel.finishWorkout()
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.hasSaveError)
        assertNull(viewModel.uiState.value.requestedCompletedWorkoutId)
        val persisted = repository.completed
        assertEquals(1, repository.saveCount)

        repository.afterSaveError = null
        viewModel.finishWorkout()
        dispatcher.scheduler.runCurrent()

        assertEquals(original.id, viewModel.uiState.value.requestedCompletedWorkoutId)
        assertSame(persisted, repository.completed)
        assertEquals(1, repository.saveCount)
    }

    @Test
    fun failedVerificationCanRetryWithoutRewritingTheCompletedSession() = runTest(dispatcher) {
        val original = session(Workout.B)
        val repository = FakeWorkoutRepository(original)
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        repository.completedReadError = IllegalStateException("Read failed")

        viewModel.finishWorkout()
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.hasSaveError)
        assertNull(viewModel.uiState.value.requestedCompletedWorkoutId)
        repository.completedReadError = null
        viewModel.finishWorkout()
        dispatcher.scheduler.runCurrent()

        assertEquals(original.id, viewModel.uiState.value.requestedCompletedWorkoutId)
        assertEquals(1, repository.saveCount)
    }

    @Test
    fun missingSavedSessionNeverRequestsSuccessOrFinalizesALaterSession() = runTest(dispatcher) {
        val original = session(Workout.A)
        val later = session(Workout.B, id = 2L)
        val repository = FakeWorkoutRepository(original)
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        repository.savedWorkout = later

        viewModel.finishWorkout()
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.hasSaveError)
        assertNull(viewModel.uiState.value.requestedCompletedWorkoutId)
        assertEquals(listOf(original.id), repository.finalizedIds)
        assertSame(later, repository.savedWorkout)
        assertEquals(0, repository.saveCount)
    }

    @Test
    fun restoredActiveDestinationRecoversTheOriginalCompletedIdWithoutTouchingALaterWorkout() = runTest(dispatcher) {
        val original = session(Workout.A)
        val later = session(Workout.B, id = 2L)
        val repository = FakeWorkoutRepository(later).apply {
            completed = original.completed()
        }
        val viewModel = ActiveWorkoutViewModel(repository, workoutId = original.id)
            .also { store.put("active-workout", it) }
        dispatcher.scheduler.runCurrent()

        assertEquals(original.id, viewModel.uiState.value.requestedCompletedWorkoutId)
        assertNull(viewModel.uiState.value.unfinishedWorkout)
        viewModel.finishWorkout()
        assertEquals(emptyList(), repository.finalizedIds)
        assertSame(later, repository.savedWorkout)
    }

    @Test
    fun missingOrLoadingWorkoutsCannotBeFinishedAndLoadFailureCanRetry() = runTest(dispatcher) {
        val repository = FakeWorkoutRepository(null)
        val viewModel = createViewModel(repository)
        viewModel.finishWorkout()
        dispatcher.scheduler.runCurrent()
        viewModel.finishWorkout()
        assertFalse(viewModel.uiState.value.canFinish)
        assertEquals(emptyList(), repository.finalizedIds)

        repository.readError = IllegalStateException("Load failed")
        viewModel.loadWorkout()
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.hasLoadError)
        repository.readError = null
        repository.savedWorkout = session(Workout.A)
        viewModel.loadWorkout()
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.canFinish)
        assertFalse(viewModel.uiState.value.hasLoadError)
    }

    @Test
    fun clearingTheOwnerCancelsSavingWithoutReportingASaveFailure() = runTest(dispatcher) {
        val repository = FakeWorkoutRepository(session(Workout.A))
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        repository.saveGate = CompletableDeferred()
        viewModel.finishWorkout()
        dispatcher.scheduler.runCurrent()

        store.clear()
        dispatcher.scheduler.runCurrent()

        assertTrue(repository.saveCancelled)
        assertFalse(viewModel.uiState.value.hasSaveError)
        assertNull(viewModel.uiState.value.requestedCompletedWorkoutId)
        assertEquals(0, repository.saveCount)
    }

    @Test
    fun completingASetPublishesSavedProgressAndRestOnlyAfterTheSaveReturns() = runTest(dispatcher) {
        val original = session(Workout.A)
        val repository = FakeWorkoutRepository(original)
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        repository.setSaveGate = CompletableDeferred()

        viewModel.completeNextSet()
        viewModel.completeNextSet()
        viewModel.completeSet(0, 0)
        viewModel.finishWorkout()
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.isCompletingSet)
        assertFalse(viewModel.uiState.value.canFinish)
        assertSame(original, viewModel.uiState.value.unfinishedWorkout)
        assertFalse(viewModel.uiState.value.restTimer.isVisible)
        assertEquals(listOf(Triple(original.id, 0, 0)), repository.completedSetRequests)
        assertTrue(repository.finalizedIds.isEmpty())

        repository.setSaveGate?.complete(Unit)
        dispatcher.scheduler.runCurrent()

        val saved = requireNotNull(viewModel.uiState.value.unfinishedWorkout)
        assertTrue(saved.exercises[0].setStates[0].isCompleted)
        assertFalse(saved.exercises[0].setStates[1].isCompleted)
        assertEquals(RestTimer(181_000), saved.restTimer)
        assertEquals("03:00", viewModel.uiState.value.restTimer.countdown)
        assertEquals(0, viewModel.uiState.value.selectedExercisePosition)
        assertTrue(viewModel.uiState.value.canFinish)

        repository.now += 15_000
        viewModel.completeSet(0, 0)
        dispatcher.scheduler.runCurrent()
        assertEquals(1, repository.completedSetRequests.size)
        assertEquals(saved.restTimer, viewModel.uiState.value.unfinishedWorkout?.restTimer)
    }

    @Test
    fun setSaveFailureLeavesBothProgressAndExistingDeadlineAvailableForRetry() = runTest(dispatcher) {
        val original = session(Workout.B).copy(restTimer = RestTimer(91_000))
        val repository = FakeWorkoutRepository(original)
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        repository.setSaveError = IllegalStateException("Disk unavailable")

        viewModel.completeSet(0, 0)
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.hasSetSaveError)
        assertSame(original, viewModel.uiState.value.unfinishedWorkout)
        assertEquals("01:30", viewModel.uiState.value.restTimer.countdown)
        assertTrue(viewModel.uiState.value.canCompleteSet)

        repository.setSaveError = null
        repository.now = 31_000
        viewModel.completeSet(0, 0)
        dispatcher.scheduler.runCurrent()

        assertFalse(viewModel.uiState.value.hasSetSaveError)
        assertEquals(RestTimer(211_000), viewModel.uiState.value.unfinishedWorkout?.restTimer)
        assertEquals("03:00", viewModel.uiState.value.restTimer.countdown)
    }

    @Test
    fun retryAfterALostSetAcknowledgementRecoversWithoutReplacingRest() = runTest(dispatcher) {
        val repository = FakeWorkoutRepository(session(Workout.A))
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        repository.afterSetSaveError = IllegalStateException("Acknowledgement lost")

        viewModel.completeSet(0, 0)
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.hasSetSaveError)
        assertFalse(viewModel.uiState.value.restTimer.isVisible)
        val deadline = repository.savedWorkout?.restTimer

        repository.afterSetSaveError = null
        repository.now += 20_000
        viewModel.completeSet(0, 0)
        dispatcher.scheduler.runCurrent()

        assertFalse(viewModel.uiState.value.hasSetSaveError)
        assertEquals(deadline, viewModel.uiState.value.unfinishedWorkout?.restTimer)
        assertEquals("02:40", viewModel.uiState.value.restTimer.countdown)
    }

    @Test
    fun resumeAndRecreationRecoverRunningExpiredAndClockAdjustedRest() = runTest(dispatcher) {
        val repository = FakeWorkoutRepository(session(Workout.B).copy(restTimer = RestTimer(181_000)))
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        viewModel.onResume()
        dispatcher.scheduler.runCurrent()

        repository.now = 61_000
        dispatcher.scheduler.advanceTimeBy(1_000)
        dispatcher.scheduler.runCurrent()
        assertEquals("02:00", viewModel.uiState.value.restTimer.countdown)

        viewModel.onPause()
        repository.now = 181_000
        dispatcher.scheduler.advanceTimeBy(10_000)
        dispatcher.scheduler.runCurrent()
        assertEquals("02:00", viewModel.uiState.value.restTimer.countdown)
        viewModel.onResume()
        dispatcher.scheduler.runCurrent()
        assertEquals("00:00", viewModel.uiState.value.restTimer.countdown)
        assertTrue(viewModel.uiState.value.restTimer.isExpired)
        assertTrue(viewModel.uiState.value.canCompleteSet)

        repository.now = 161_000
        dispatcher.scheduler.advanceTimeBy(1_000)
        dispatcher.scheduler.runCurrent()
        assertEquals("00:20", viewModel.uiState.value.restTimer.countdown)
        assertFalse(viewModel.uiState.value.restTimer.isExpired)
        viewModel.onPause()
        store.clear()

        repository.now = 300_000
        val recreated = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        assertTrue(recreated.uiState.value.restTimer.isExpired)
        assertEquals(RestTimer(181_000), recreated.uiState.value.unfinishedWorkout?.restTimer)
        assertTrue(recreated.uiState.value.canFinish)
    }

    @Test
    fun completedExerciseAdvancesAndDeadliftUsesItsSingleSavedSet() = runTest(dispatcher) {
        val original = session(Workout.B).let { workout ->
            workout.copy(exercises = workout.exercises.mapIndexed { position, exercise ->
                if (position < 2) exercise.copy(setStates = exercise.setStates.map { it.copy(isCompleted = true) })
                else exercise
            })
        }
        val repository = FakeWorkoutRepository(original)
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        assertEquals(2, viewModel.uiState.value.selectedExercisePosition)
        assertEquals(1, original.exercises[2].setStates.size)

        viewModel.completeNextSet()
        dispatcher.scheduler.runCurrent()
        assertEquals(listOf(Triple(original.id, 2, 0)), repository.completedSetRequests)
        assertTrue(requireNotNull(viewModel.uiState.value.unfinishedWorkout).exercises.all { exercise ->
            exercise.setStates.all { it.isCompleted }
        })
        viewModel.completeNextSet()
        assertEquals(1, repository.completedSetRequests.size)
        assertTrue(viewModel.uiState.value.canFinish)
    }

    @Test
    fun theLastSetSelectsTheNextIncompleteExerciseAndRowsCanSelectSavedExercises() = runTest(dispatcher) {
        val original = session(Workout.A).let { workout ->
            workout.copy(exercises = workout.exercises.mapIndexed { position, exercise ->
                if (position == 0) exercise.copy(setStates = exercise.setStates.map { it.copy(isCompleted = it.position < 4) })
                else exercise
            })
        }
        val repository = FakeWorkoutRepository(original)
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        viewModel.completeNextSet()
        dispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.uiState.value.selectedExercisePosition)
        viewModel.selectExercise(2)
        assertEquals(2, viewModel.uiState.value.selectedExercisePosition)
        viewModel.selectExercise(-1)
        viewModel.selectExercise(3)
        assertEquals(2, viewModel.uiState.value.selectedExercisePosition)
    }

    @Test
    fun aStaleSessionOrInvalidSetCannotChangeALaterWorkoutOrItsRest() = runTest(dispatcher) {
        val original = session(Workout.A)
        val later = session(Workout.B, id = 2).copy(restTimer = RestTimer(31_000))
        val repository = FakeWorkoutRepository(original)
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        viewModel.completeSet(-1, 0)
        viewModel.completeSet(0, 99)
        assertTrue(repository.completedSetRequests.isEmpty())
        repository.savedWorkout = later

        viewModel.completeSet(0, 0)
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.hasSetSaveError)
        assertSame(later, repository.savedWorkout)
        assertSame(original, viewModel.uiState.value.unfinishedWorkout)
        assertFalse(viewModel.uiState.value.restTimer.isVisible)
    }

    @Test
    fun finishingClearsDisplayedRestOnlyWhenTheSaveCommits() = runTest(dispatcher) {
        val original = session(Workout.A).copy(restTimer = RestTimer(181_000))
        val repository = FakeWorkoutRepository(original)
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        repository.saveError = IllegalStateException("Save failed")

        viewModel.finishWorkout()
        dispatcher.scheduler.runCurrent()
        assertEquals(original.restTimer, viewModel.uiState.value.unfinishedWorkout?.restTimer)
        assertTrue(viewModel.uiState.value.restTimer.isVisible)

        repository.saveError = null
        repository.completedReadGate = CompletableDeferred()
        viewModel.finishWorkout()
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.isSaving)
        assertFalse(viewModel.uiState.value.restTimer.isVisible)
        assertNull(viewModel.uiState.value.unfinishedWorkout?.restTimer)
        assertNull(viewModel.uiState.value.requestedCompletedWorkoutId)

        repository.completedReadGate?.complete(Unit)
        dispatcher.scheduler.runCurrent()
        assertEquals(original.id, viewModel.uiState.value.requestedCompletedWorkoutId)
    }

    @Test
    fun clearingTheOwnerCancelsSetSavingAndCountdownWork() = runTest(dispatcher) {
        val repository = FakeWorkoutRepository(session(Workout.A).copy(restTimer = RestTimer(181_000)))
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        viewModel.onResume()
        dispatcher.scheduler.runCurrent()
        repository.setSaveGate = CompletableDeferred()
        viewModel.completeSet(0, 0)
        dispatcher.scheduler.runCurrent()

        store.clear()
        dispatcher.scheduler.runCurrent()
        assertTrue(repository.setSaveCancelled)
        assertFalse(viewModel.uiState.value.hasSetSaveError)
        val lastCountdown = viewModel.uiState.value.restTimer
        repository.now += 100_000
        dispatcher.scheduler.advanceTimeBy(10_000)
        dispatcher.scheduler.runCurrent()
        assertEquals(lastCountdown, viewModel.uiState.value.restTimer)
        assertFalse(requireNotNull(repository.savedWorkout).exercises[0].setStates[0].isCompleted)
    }

    private fun createViewModel(repository: FakeWorkoutRepository): ActiveWorkoutViewModel =
        ActiveWorkoutViewModel(repository, restTimerRules = RestTimerRules { repository.now })
            .also { store.put("active-workout", it) }

    private class FakeWorkoutRepository(var savedWorkout: UnfinishedWorkout?) : WorkoutRepository {
        var readCount = 0
        var readError: Exception? = null
        var completed: CompletedWorkout? = null
        var completedReadGate: CompletableDeferred<Unit>? = null
        var completedReadError: Exception? = null
        var saveGate: CompletableDeferred<Unit>? = null
        var saveError: Exception? = null
        var afterSaveError: Exception? = null
        var saveCount = 0
        var saveCancelled = false
        var now = 1_000L
        var setSaveGate: CompletableDeferred<Unit>? = null
        var setSaveError: Exception? = null
        var afterSetSaveError: Exception? = null
        var setSaveCancelled = false
        val completedSetRequests = mutableListOf<Triple<Long, Int, Int>>()
        val finalizedIds = mutableListOf<Long>()
        val completedReadIds = mutableListOf<Long>()

        override suspend fun getUnfinishedWorkout(): UnfinishedWorkout? {
            readCount++
            readError?.let { throw it }
            return savedWorkout
        }

        override suspend fun getCompletedWorkout(workoutId: Long): CompletedWorkout? {
            completedReadIds += workoutId
            completedReadGate?.await()
            completedReadError?.let { throw it }
            return completed?.takeIf { it.id == workoutId }
        }

        override suspend fun startWorkout(workout: Workout): UnfinishedWorkout =
            error("Active Workout only loads an existing session")

        override suspend fun getNextWorkout(): Workout =
            error("Active Workout only loads an existing session")

        override suspend fun getNextWorkoutPrescription(): WorkoutPrescription =
            error("Active Workout does not select the next program")

        override suspend fun finalizeWorkout(workoutId: Long): Boolean {
            finalizedIds += workoutId
            try {
                saveGate?.await()
            } catch (cancelled: CancellationException) {
                saveCancelled = true
                throw cancelled
            }
            saveError?.let { throw it }
            val active = savedWorkout?.takeIf { it.id == workoutId } ?: return false
            completed = active.completed()
            savedWorkout = null
            saveCount++
            afterSaveError?.let { throw it }
            return true
        }

        override suspend fun setSetCompleted(
            exercisePosition: Int,
            setPosition: Int,
            isCompleted: Boolean,
        ): Boolean = error("Active Workout only loads an existing session")

        override suspend fun completeSetAndStartRest(
            workoutId: Long,
            exercisePosition: Int,
            setPosition: Int,
            restDurationMillis: Long,
        ): UnfinishedWorkout? {
            completedSetRequests += Triple(workoutId, exercisePosition, setPosition)
            try {
                setSaveGate?.await()
            } catch (cancelled: CancellationException) {
                setSaveCancelled = true
                throw cancelled
            }
            setSaveError?.let { throw it }
            val active = savedWorkout?.takeIf { it.id == workoutId } ?: return null
            val set = active.exercises.getOrNull(exercisePosition)?.setStates?.getOrNull(setPosition) ?: return null
            if (set.isCompleted) return active
            val saved = active.copy(
                exercises = active.exercises.mapIndexed { index, exercise ->
                    if (index != exercisePosition) exercise else exercise.copy(
                        setStates = exercise.setStates.map { if (it.position == setPosition) it.copy(isCompleted = true) else it },
                    )
                },
                restTimer = RestTimerRules { now }.start(restDurationMillis),
            )
            savedWorkout = saved
            afterSetSaveError?.let { throw it }
            return saved
        }
    }

    companion object {
        private fun session(workout: Workout, id: Long = 1L) = UnfinishedWorkout(
            workout = workout,
            startedAtEpochMillis = 1_000L,
            exercises = workout.exercises.map {
                UnfinishedWorkoutExercise(it, it.sets, it.reps, it.startingWeightKg)
            },
            id = id,
        )

        private fun UnfinishedWorkout.completed() = CompletedWorkout(
            id, workout, startedAtEpochMillis, 123_000L, exercises,
        )
    }
}
