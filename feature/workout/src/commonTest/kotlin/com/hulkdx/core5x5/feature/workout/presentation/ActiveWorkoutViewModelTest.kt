package com.hulkdx.core5x5.feature.workout.presentation

import androidx.lifecycle.ViewModelStore
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
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
import kotlinx.coroutines.flow.MutableStateFlow
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
        val viewModel = ActiveWorkoutViewModel(
            repository = repository,
            preferences = FakeTrainingPreferencesRepository(),
        ).also { store.put("active-workout", it) }
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
    fun firstResumeDoesNotRepeatAnAlreadyFinishedConstructorLoad() = runTest(dispatcher) {
        val repository = FakeWorkoutRepository(session(Workout.A))
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()

        viewModel.onResume()
        dispatcher.scheduler.runCurrent()

        assertEquals(1, repository.readCount)
        assertSame(repository.savedWorkout, viewModel.uiState.value.unfinishedWorkout)
    }

    @Test
    fun firstResumeDoesNotRepeatAnInFlightConstructorLoad() = runTest(dispatcher) {
        val repository = FakeWorkoutRepository(session(Workout.B)).apply {
            readGate = CompletableDeferred()
        }
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        viewModel.onResume()
        dispatcher.scheduler.runCurrent()

        assertEquals(1, repository.readCount)
        assertTrue(viewModel.uiState.value.isLoading)
        repository.readGate?.complete(Unit)
        dispatcher.scheduler.runCurrent()
        assertEquals(1, repository.readCount)
        assertSame(repository.savedWorkout, viewModel.uiState.value.unfinishedWorkout)
    }

    @Test
    fun pauseThenResumeReloadsSavedProgressAndRecoversExpiredRest() = runTest(dispatcher) {
        val repository = FakeWorkoutRepository(session(Workout.A))
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        viewModel.onResume()
        viewModel.onPause()
        val updated = requireNotNull(repository.savedWorkout).copy(
            restTimer = RestTimer(2_000),
            exercises = requireNotNull(repository.savedWorkout).exercises.map { it.copy(weightKg = 32.5) },
        )
        repository.savedWorkout = updated
        repository.now = 3_000

        viewModel.onResume()
        dispatcher.scheduler.runCurrent()

        assertEquals(2, repository.readCount)
        assertSame(updated, viewModel.uiState.value.unfinishedWorkout)
        assertTrue(viewModel.uiState.value.restTimer.isExpired)
        assertEquals("00:00", viewModel.uiState.value.restTimer.countdown)
        assertTrue(viewModel.uiState.value.canCompleteSet)
        // Cancel the recurring ticker before runTest drains the scheduler.
        viewModel.onPause()
    }

    @Test
    fun requestedMissingIdNeverLoadsOrModifiesANewerActiveSession() = runTest(dispatcher) {
        val later = session(Workout.B, id = 2L)
        val repository = FakeWorkoutRepository(later)
        val viewModel = ActiveWorkoutViewModel(
            repository = repository,
            preferences = FakeTrainingPreferencesRepository(),
            workoutId = 1L,
        ).also { store.put("active-workout", it) }
        dispatcher.scheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.unfinishedWorkout)
        assertNull(viewModel.uiState.value.requestedCompletedWorkoutId)
        assertFalse(viewModel.uiState.value.canCompleteSet)
        viewModel.completeNextSet()
        viewModel.finishWorkout()
        assertEquals(emptyList(), repository.completedSetRequests)
        assertEquals(emptyList(), repository.finalizedIds)
        assertSame(later, repository.savedWorkout)
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
        val viewModel = ActiveWorkoutViewModel(
            repository = repository,
            preferences = FakeTrainingPreferencesRepository(),
            workoutId = original.id,
        )
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
    fun completingASetUsesTheCurrentSavedRestDuration() = runTest(dispatcher) {
        val repository = FakeWorkoutRepository(session(Workout.A))
        val preferences = FakeTrainingPreferencesRepository(TrainingPreferences(restDurationSeconds = 45))
        val viewModel = createViewModel(repository, preferences)
        dispatcher.scheduler.runCurrent()

        viewModel.completeSet(0, 0)
        dispatcher.scheduler.runCurrent()

        assertEquals(RestTimer(46_000), viewModel.uiState.value.unfinishedWorkout?.restTimer)
        preferences.value = TrainingPreferences(restDurationSeconds = 91)
        viewModel.completeSet(0, 1)
        dispatcher.scheduler.runCurrent()

        assertEquals(RestTimer(92_000), viewModel.uiState.value.unfinishedWorkout?.restTimer)
    }

    @Test
    fun restDurationReadFailureLeavesSetAndExistingRestUnchanged() = runTest(dispatcher) {
        val original = session(Workout.B).copy(restTimer = RestTimer(91_000))
        val repository = FakeWorkoutRepository(original)
        val preferences = FakeTrainingPreferencesRepository().apply {
            readError = IllegalStateException("Preferences unavailable")
        }
        val viewModel = createViewModel(repository, preferences)
        dispatcher.scheduler.runCurrent()

        viewModel.completeSet(0, 0)
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.hasSetSaveError)
        assertTrue(repository.completedSetRequests.isEmpty())
        assertSame(original, viewModel.uiState.value.unfinishedWorkout)
        assertEquals("01:30", viewModel.uiState.value.restTimer.countdown)
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

    @Test
    fun tappingACompletedSetUndoesOnlyThatSetAndRecompletionStartsFreshRest() = runTest(dispatcher) {
        for (workout in Workout.entries) {
            val repository = FakeWorkoutRepository(session(workout))
            val preferences = FakeTrainingPreferencesRepository()
            val viewModel = createViewModel(repository, preferences)
            dispatcher.scheduler.runCurrent()
            viewModel.toggleSet(2, 0)
            dispatcher.scheduler.runCurrent()
            val completed = requireNotNull(viewModel.uiState.value.unfinishedWorkout)
            assertTrue(completed.exercises[2].setStates[0].isCompleted)

            preferences.readError = IllegalStateException("Undo needs no duration preference")
            repository.now += 20_000
            viewModel.toggleSet(2, 0)
            dispatcher.scheduler.runCurrent()
            val undone = requireNotNull(viewModel.uiState.value.unfinishedWorkout)
            assertFalse(undone.exercises[2].setStates[0].isCompleted)
            assertEquals(completed.restTimer, undone.restTimer)
            assertEquals("02:40", viewModel.uiState.value.restTimer.countdown)
            assertEquals(completed.exercises.take(2), undone.exercises.take(2))
            assertEquals(completed.exercises[2].setStates.drop(1), undone.exercises[2].setStates.drop(1))
            assertEquals(2, viewModel.uiState.value.selectedExercisePosition)
            assertFalse(viewModel.uiState.value.hasSetSaveError)

            preferences.readError = null
            viewModel.toggleSet(2, 0)
            dispatcher.scheduler.runCurrent()
            assertTrue(requireNotNull(viewModel.uiState.value.unfinishedWorkout).exercises[2].setStates[0].isCompleted)
            assertEquals(RestTimer(201_000), viewModel.uiState.value.unfinishedWorkout?.restTimer)
        }
    }

    @Test
    fun undoPreservesExistingRestAndRecompletionUsesTheExerciseOverride() = runTest(dispatcher) {
        for (restOverride in listOf(0L, 150_000L)) {
            val workout = session(Workout.A).copy(
                restTimer = RestTimer(181_000L),
                exercises = session(Workout.A).exercises.mapIndexed { index, exercise ->
                    if (index != 0) exercise else exercise.copy(
                        restDurationMillis = restOverride,
                        setStates = exercise.setStates.map {
                            if (it.position == 0) it.copy(isCompleted = true) else it
                        },
                    )
                },
            )
            val repository = FakeWorkoutRepository(workout)
            val preferences = FakeTrainingPreferencesRepository().apply {
                readError = IllegalStateException("Exercise overrides need no preference read")
            }
            val viewModel = createViewModel(repository, preferences)
            dispatcher.scheduler.runCurrent()

            repository.now += 20_000L
            viewModel.toggleSet(0, 0)
            dispatcher.scheduler.runCurrent()
            val undone = requireNotNull(viewModel.uiState.value.unfinishedWorkout)
            assertFalse(undone.exercises[0].setStates[0].isCompleted)
            assertEquals(workout.restTimer, undone.restTimer)
            assertFalse(viewModel.uiState.value.hasSetSaveError)

            viewModel.toggleSet(0, 0)
            dispatcher.scheduler.runCurrent()
            val recompleted = requireNotNull(viewModel.uiState.value.unfinishedWorkout)
            assertTrue(recompleted.exercises[0].setStates[0].isCompleted)
            assertEquals(restOverride.takeIf { it > 0 }?.let { RestTimer(repository.now + it) }, recompleted.restTimer)
            assertFalse(viewModel.uiState.value.hasSetSaveError)
        }
    }

    @Test
    fun undoFailurePreservesProgressAndRestAndCanRetry() = runTest(dispatcher) {
        val repository = FakeWorkoutRepository(session(Workout.A))
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        viewModel.toggleSet(0, 0)
        dispatcher.scheduler.runCurrent()
        val completed = viewModel.uiState.value.unfinishedWorkout
        repository.setSaveError = IllegalStateException("Disk unavailable")
        viewModel.toggleSet(0, 0)
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.hasSetSaveError)
        assertSame(completed, viewModel.uiState.value.unfinishedWorkout)
        assertTrue(viewModel.uiState.value.canCompleteSet)

        repository.setSaveError = null
        viewModel.toggleSet(0, 0)
        dispatcher.scheduler.runCurrent()
        assertFalse(viewModel.uiState.value.hasSetSaveError)
        assertFalse(requireNotNull(viewModel.uiState.value.unfinishedWorkout).exercises[0].setStates[0].isCompleted)
        assertEquals(completed?.restTimer, viewModel.uiState.value.unfinishedWorkout?.restTimer)
    }

    @Test
    fun retryAfterLostUndoAcknowledgementDoesNotRecompleteTheSet() = runTest(dispatcher) {
        val repository = FakeWorkoutRepository(session(Workout.B))
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        viewModel.toggleSet(2, 0)
        dispatcher.scheduler.runCurrent()
        val timer = repository.savedWorkout?.restTimer
        repository.afterSetSaveError = IllegalStateException("Acknowledgement lost")
        viewModel.toggleSet(2, 0)
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.hasSetSaveError)
        assertTrue(requireNotNull(viewModel.uiState.value.unfinishedWorkout).exercises[2].setStates[0].isCompleted)
        assertFalse(requireNotNull(repository.savedWorkout).exercises[2].setStates[0].isCompleted)

        repository.afterSetSaveError = null
        viewModel.toggleSet(2, 0)
        dispatcher.scheduler.runCurrent()
        assertFalse(viewModel.uiState.value.hasSetSaveError)
        assertFalse(requireNotNull(viewModel.uiState.value.unfinishedWorkout).exercises[2].setStates[0].isCompleted)
        assertEquals(timer, viewModel.uiState.value.unfinishedWorkout?.restTimer)
        assertEquals(1, repository.completedSetRequests.size)
        assertEquals(2, repository.undoneSetRequests.size)
    }

    @Test
    fun undoGuardsOverlappingActionsAndPublishesOnlySavedProgress() = runTest(dispatcher) {
        val repository = FakeWorkoutRepository(session(Workout.A))
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        viewModel.toggleSet(0, 0)
        dispatcher.scheduler.runCurrent()
        val completed = viewModel.uiState.value.unfinishedWorkout
        viewModel.toggleSet(-1, 0)
        viewModel.toggleSet(0, 5)
        assertTrue(repository.undoneSetRequests.isEmpty())

        repository.setSaveGate = CompletableDeferred()
        viewModel.toggleSet(0, 0)
        viewModel.toggleSet(0, 0)
        viewModel.toggleSet(1, 0)
        viewModel.finishWorkout()
        viewModel.loadWorkout()
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.isCompletingSet)
        assertFalse(viewModel.uiState.value.canFinish)
        assertSame(completed, viewModel.uiState.value.unfinishedWorkout)
        assertEquals(1, repository.undoneSetRequests.size)
        assertEquals(1, repository.completedSetRequests.size)
        assertTrue(repository.finalizedIds.isEmpty())
        assertEquals(1, repository.readCount)

        repository.setSaveGate?.complete(Unit)
        dispatcher.scheduler.runCurrent()
        assertFalse(viewModel.uiState.value.isCompletingSet)
        assertTrue(viewModel.uiState.value.canFinish)
        assertFalse(requireNotNull(viewModel.uiState.value.unfinishedWorkout).exercises[0].setStates[0].isCompleted)
    }

    @Test
    fun undoNeverModifiesALaterActiveSession() = runTest(dispatcher) {
        val repository = FakeWorkoutRepository(session(Workout.A, id = 42L))
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        viewModel.toggleSet(0, 0)
        dispatcher.scheduler.runCurrent()
        val original = viewModel.uiState.value.unfinishedWorkout
        val later = session(Workout.B, id = 43L)
        repository.savedWorkout = later
        viewModel.toggleSet(0, 0)
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.hasSetSaveError)
        assertSame(original, viewModel.uiState.value.unfinishedWorkout)
        assertSame(later, repository.savedWorkout)
        assertEquals(listOf(Triple(42L, 0, 0)), repository.undoneSetRequests)
    }

    private fun createViewModel(
        repository: FakeWorkoutRepository,
        preferences: FakeTrainingPreferencesRepository = FakeTrainingPreferencesRepository(),
    ): ActiveWorkoutViewModel = ActiveWorkoutViewModel(
        repository = repository,
        preferences = preferences,
        restTimerRules = RestTimerRules { repository.now },
    )
            .also { store.put("active-workout", it) }

    private class FakeTrainingPreferencesRepository(
        var value: TrainingPreferences = TrainingPreferences(),
    ) : TrainingPreferencesRepository {
        val values = MutableStateFlow(value)
        var readError: Exception? = null

        override val preferences = values

        override suspend fun getPreferences(): TrainingPreferences {
            readError?.let { throw it }
            return value
        }

        override suspend fun setWeightUnit(unit: WeightUnit) {
            value = value.copy(weightUnit = unit)
            values.value = value
        }

        override suspend fun setRestDurationSeconds(seconds: Long) {
            value = value.copy(restDurationSeconds = seconds)
            values.value = value
        }
    }

    @Test
    fun editCancelAndCustomRestCancelNeverPersistDrafts() = runTest(dispatcher) {
        val original = session(Workout.A)
        val repository = FakeWorkoutRepository(original)
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        viewModel.openExerciseEdit(0, WeightUnit.LB)
        dispatcher.scheduler.runCurrent()
        viewModel.editName("Front Squat")
        viewModel.adjustWeight(1)
        viewModel.openCustomRest()
        viewModel.editCustomRest(150)
        viewModel.dismissExerciseEdit()
        assertEquals(180_000L, viewModel.uiState.value.exerciseEdit?.effectiveRestDurationMillis)
        viewModel.dismissExerciseEdit()
        assertNull(viewModel.uiState.value.exerciseEdit)
        assertEquals(original, repository.savedWorkout)
        assertEquals(0, repository.editCount)
    }

    @Test
    fun editSaveIsAtomicGuardsOtherActionsAndPreservesLbPrecisionAndRunningRest() = runTest(dispatcher) {
        val original = session(Workout.A).copy(restTimer = RestTimer(123_000))
        val repository = FakeWorkoutRepository(original)
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        viewModel.openExerciseEdit(0, WeightUnit.LB)
        dispatcher.scheduler.runCurrent()
        viewModel.editName("  Front Squat  ")
        viewModel.adjustSets(1)
        viewModel.adjustReps(-1)
        viewModel.openCustomRest()
        viewModel.editCustomRest(150)
        viewModel.applyCustomRest()
        repository.editGate = CompletableDeferred()
        viewModel.saveExerciseEdit()
        viewModel.saveExerciseEdit()
        viewModel.completeSet(0, 0)
        viewModel.finishWorkout()
        viewModel.dismissExerciseEdit()
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.exerciseEdit?.isSaving == true)
        assertEquals(original, viewModel.uiState.value.unfinishedWorkout)
        assertTrue(repository.completedSetRequests.isEmpty())
        assertTrue(repository.finalizedIds.isEmpty())
        repository.editGate?.complete(Unit)
        dispatcher.scheduler.runCurrent()
        val saved = requireNotNull(viewModel.uiState.value.unfinishedWorkout)
        assertNull(viewModel.uiState.value.exerciseEdit)
        assertEquals(1, repository.editCount)
        assertEquals("Front Squat", saved.exercises[0].customName)
        assertEquals(20.0, saved.exercises[0].weightKg)
        assertEquals(6, saved.exercises[0].sets)
        assertEquals(4, saved.exercises[0].reps)
        assertEquals(150_000L, saved.exercises[0].restDurationMillis)
        assertEquals(original.restTimer, saved.restTimer)
    }

    @Test
    fun editFailurePreservesDraftAndCanRetryAndLoggedSetsCannotBeRemoved() = runTest(dispatcher) {
        val original = session(Workout.A).let { workout -> workout.copy(exercises = workout.exercises.mapIndexed { index, exercise ->
            if (index != 0) exercise else exercise.copy(setStates = exercise.setStates.map { it.copy(isCompleted = it.position == 3) })
        }) }
        val repository = FakeWorkoutRepository(original)
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        viewModel.openExerciseEdit(0, WeightUnit.KG)
        dispatcher.scheduler.runCurrent()
        repeat(10) { viewModel.adjustSets(-1) }
        assertEquals(4, viewModel.uiState.value.exerciseEdit?.sets)
        viewModel.editName("Tempo Squat")
        repository.editError = IllegalStateException("Write failed")
        viewModel.saveExerciseEdit()
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.exerciseEdit?.hasSaveError == true)
        assertEquals("Tempo Squat", viewModel.uiState.value.exerciseEdit?.name)
        assertEquals(original, viewModel.uiState.value.unfinishedWorkout)
        repository.editError = null
        viewModel.saveExerciseEdit()
        dispatcher.scheduler.runCurrent()
        assertNull(viewModel.uiState.value.exerciseEdit)
        assertTrue(repository.savedWorkout!!.exercises[0].setStates[3].isCompleted)
    }

    @Test
    fun offAndCustomRestOverridesBypassPreferenceReadAndApplyOnlyToNextSet() = runTest(dispatcher) {
        val repository = FakeWorkoutRepository(session(Workout.A))
        val preferences = FakeTrainingPreferencesRepository()
        val viewModel = createViewModel(repository, preferences = preferences)
        dispatcher.scheduler.runCurrent()
        viewModel.openExerciseEdit(0, WeightUnit.KG)
        dispatcher.scheduler.runCurrent()
        viewModel.selectRest(0)
        viewModel.saveExerciseEdit()
        dispatcher.scheduler.runCurrent()
        preferences.readError = IllegalStateException("Unavailable")
        viewModel.completeSet(0, 0)
        dispatcher.scheduler.runCurrent()
        assertTrue(repository.savedWorkout!!.exercises[0].setStates[0].isCompleted)
        assertNull(repository.savedWorkout!!.restTimer)
        viewModel.openExerciseEdit(0, WeightUnit.KG)
        dispatcher.scheduler.runCurrent()
        viewModel.openCustomRest()
        viewModel.editCustomRest(150)
        viewModel.applyCustomRest()
        viewModel.saveExerciseEdit()
        dispatcher.scheduler.runCurrent()
        viewModel.completeSet(0, 1)
        dispatcher.scheduler.runCurrent()
        assertEquals(RestTimer(repository.now + 150_000), repository.savedWorkout!!.restTimer)
    }

    @Test
    fun blankNameCannotSaveAndPauseResumePreservesDraft() = runTest(dispatcher) {
        val repository = FakeWorkoutRepository(session(Workout.A))
        val viewModel = createViewModel(repository)
        dispatcher.scheduler.runCurrent()
        viewModel.openExerciseEdit(0, WeightUnit.KG)
        dispatcher.scheduler.runCurrent()
        viewModel.editName(" ")
        viewModel.saveExerciseEdit()
        assertEquals(0, repository.editCount)
        viewModel.editName("Paused Squat")
        viewModel.onPause()
        viewModel.onResume()
        dispatcher.scheduler.runCurrent()
        assertEquals("Paused Squat", viewModel.uiState.value.exerciseEdit?.name)
    }

    private class FakeWorkoutRepository(var savedWorkout: UnfinishedWorkout?) : WorkoutRepository {
        var editCount = 0
        var editGate: CompletableDeferred<Unit>? = null
        var editError: Exception? = null
        var readCount = 0
        var readGate: CompletableDeferred<Unit>? = null
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
        val undoneSetRequests = mutableListOf<Triple<Long, Int, Int>>()
        val completedSetRequests = mutableListOf<Triple<Long, Int, Int>>()
        val finalizedIds = mutableListOf<Long>()
        val completedReadIds = mutableListOf<Long>()

        override suspend fun getUnfinishedWorkout(): UnfinishedWorkout? {
            readCount++
            readGate?.await()
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

        override suspend fun getNextWorkoutPrescription(workoutOverride: Workout?): WorkoutPrescription =
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

        override suspend fun editExercise(
            workoutId: Long, exercisePosition: Int,
            edit: com.hulkdx.core5x5.feature.workout.domain.ExerciseEdit,
        ): UnfinishedWorkout? {
            editCount++
            editGate?.await()
            editError?.let { throw it }
            val active = savedWorkout?.takeIf { it.id == workoutId } ?: return null
            val exercise = active.exercises.getOrNull(exercisePosition) ?: return null
            if (exercise.setStates.any { it.isCompleted && it.position >= edit.sets }) return null
            val saved = active.copy(exercises = active.exercises.mapIndexed { index, item ->
                if (index != exercisePosition) item else item.copy(customName = edit.name.trim(), weightKg = edit.weightKg,
                    sets = edit.sets, reps = edit.reps, restDurationMillis = edit.restDurationMillis,
                    setStates = List(edit.sets) { position -> item.setStates.getOrNull(position)
                        ?: com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutSet(position) })
            })
            savedWorkout = saved
            return saved
        }

        override suspend fun undoSetCompletion(
            workoutId: Long,
            exercisePosition: Int,
            setPosition: Int,
        ): UnfinishedWorkout? {
            undoneSetRequests += Triple(workoutId, exercisePosition, setPosition)
            try {
                setSaveGate?.await()
            } catch (cancelled: CancellationException) {
                setSaveCancelled = true
                throw cancelled
            }
            setSaveError?.let { throw it }
            val active = savedWorkout?.takeIf { it.id == workoutId } ?: return null
            val set = active.exercises.getOrNull(exercisePosition)?.setStates
                ?.firstOrNull { it.position == setPosition } ?: return null
            if (!set.isCompleted) return active
            val saved = active.copy(exercises = active.exercises.mapIndexed { index, exercise ->
                if (index != exercisePosition) exercise else exercise.copy(
                    setStates = exercise.setStates.map { if (it.position == setPosition) it.copy(isCompleted = false) else it },
                )
            })
            savedWorkout = saved
            afterSetSaveError?.let { throw it }
            return saved
        }

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
                restTimer = (active.exercises[exercisePosition].restDurationMillis ?: restDurationMillis)
                    .takeIf { it > 0 }?.let { RestTimerRules { now }.start(it) },
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
