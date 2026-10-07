package com.hulkdx.core5x5.feature.workout.presentation

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import com.hulkdx.core5x5.feature.workout.di.workoutModule
import com.hulkdx.core5x5.feature.workout.domain.Exercise
import com.hulkdx.core5x5.feature.workout.domain.CompletedWorkout
import com.hulkdx.core5x5.feature.workout.domain.WorkoutPrescription
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutExercise
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.feature.workout.domain.WorkoutRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.annotation.KoinInternalApi
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.viewmodel.resolveViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class TodayViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeWorkoutRepository()
    private val stores = mutableListOf<ViewModelStore>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        stores.forEach { it.clear() }
        Dispatchers.resetMain()
    }

    @Test
    fun loadingDisablesActionsThenShowsInitialProgram() = runTest(dispatcher) {
        val viewModel = createViewModel()
        assertEquals(TodayUiState(), viewModel.uiState.value)
        viewModel.startWorkout()
        viewModel.requestResume()
        dispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.canStart)
        assertFalse(state.canResume)
        val displayed = requireNotNull(state.nextWorkout)
        assertEquals(Workout.A, displayed)
        assertEquals(listOf(Exercise.SQUAT, Exercise.BENCH_PRESS, Exercise.BARBELL_ROW), displayed.exercises)
        assertTrue(displayed.exercises.all { it.sets == 5 && it.reps == 5 && it.startingWeightKg == 20.0 })
        assertNull(state.unfinishedWorkout)
        assertNull(state.requestedWorkout)
        assertEquals(emptyList(), repository.startedWorkouts)
        assertEquals(1, repository.readCount)
        assertEquals(1, repository.nextWorkoutReadCount)
    }

    @Test
    fun loadAndRecreationUseThePersistedNextWorkout() = runTest(dispatcher) {
        repository.nextWorkout = Workout.B
        val first = createViewModel()
        dispatcher.scheduler.runCurrent()

        assertEquals(Workout.B, first.uiState.value.nextWorkout)
        assertTrue(first.uiState.value.canStart)
        assertNull(first.uiState.value.requestedWorkout)

        stores.single().clear()
        val recreated = createViewModel()
        dispatcher.scheduler.runCurrent()
        assertEquals(Workout.B, recreated.uiState.value.nextWorkout)
        assertTrue(recreated.uiState.value.canStart)
        assertNull(recreated.uiState.value.requestedWorkout)
        assertEquals(emptyList(), repository.startedWorkouts)
    }

    @Test
    fun refreshUsesUpdatedSelectionAfterEachCompletion() = runTest(dispatcher) {
        val viewModel = createViewModel()
        dispatcher.scheduler.runCurrent()
        assertEquals(Workout.A, viewModel.uiState.value.nextWorkout)

        for (nextWorkout in listOf(Workout.B, Workout.A)) {
            repository.nextWorkout = nextWorkout
            viewModel.loadWorkout()
            dispatcher.scheduler.runCurrent()
            assertEquals(nextWorkout, viewModel.uiState.value.nextWorkout)
            assertTrue(viewModel.uiState.value.canStart)
            assertNull(viewModel.uiState.value.requestedWorkout)
        }
        assertEquals(3, repository.nextWorkoutReadCount)
        assertEquals(emptyList(), repository.startedWorkouts)
    }

    @Test
    fun activeWorkoutIsResumableWithoutAutomaticallyRequestingIt() = runTest(dispatcher) {
        val active = session(Workout.B)
        repository.active = active
        repository.nextWorkoutError = IllegalStateException("Active sessions do not need next-workout selection")
        val viewModel = createViewModel()
        dispatcher.scheduler.runCurrent()

        assertSame(active, viewModel.uiState.value.unfinishedWorkout)
        assertTrue(viewModel.uiState.value.canResume)
        assertFalse(viewModel.uiState.value.canStart)
        assertNull(viewModel.uiState.value.nextWorkout)
        assertNull(viewModel.uiState.value.requestedWorkout)
        assertEquals(0, repository.nextWorkoutReadCount)
        viewModel.startWorkout()
        assertEquals(emptyList(), repository.startedWorkouts)
    }

    @Test
    fun startUsesDisplayedWorkoutAndPublishesReturnedSession() = runTest(dispatcher) {
        repository.nextWorkout = Workout.B
        val viewModel = createViewModel()
        dispatcher.scheduler.runCurrent()
        repository.startGate = CompletableDeferred()
        viewModel.startWorkout()
        viewModel.startWorkout()
        viewModel.loadWorkout()
        assertTrue(viewModel.uiState.value.isWorking)
        assertFalse(viewModel.uiState.value.canStart)
        dispatcher.scheduler.runCurrent()
        assertEquals(listOf(Workout.B), repository.startedWorkouts)

        repository.startGate?.complete(Unit)
        dispatcher.scheduler.runCurrent()
        assertEquals(repository.active, viewModel.uiState.value.requestedWorkout)
        assertEquals(repository.active, viewModel.uiState.value.unfinishedWorkout)
        assertNull(viewModel.uiState.value.nextWorkout)
        assertNull(viewModel.uiState.value.error)
        viewModel.onWorkoutRequestHandled()
        assertNull(viewModel.uiState.value.requestedWorkout)
        assertTrue(viewModel.uiState.value.canResume)
    }

    @Test
    fun startHonorsExistingSessionReturnedByRepository() = runTest(dispatcher) {
        val viewModel = createViewModel()
        dispatcher.scheduler.runCurrent()
        val active = session(Workout.B)
        repository.active = active
        viewModel.startWorkout()
        dispatcher.scheduler.runCurrent()
        assertEquals(listOf(Workout.A), repository.startedWorkouts)
        assertSame(active, viewModel.uiState.value.requestedWorkout)
    }

    @Test
    fun resumeRequestsTheDisplayedIdImmediatelyWithoutReadingOrStarting() = runTest(dispatcher) {
        val displayed = session(Workout.B).copy(id = Int.MAX_VALUE.toLong() + 42)
        repository.active = displayed
        val viewModel = createViewModel()
        dispatcher.scheduler.runCurrent()
        repository.active = session(Workout.A).copy(id = displayed.id + 1)
        repository.readGate = CompletableDeferred()
        viewModel.requestResume()
        assertSame(displayed, viewModel.uiState.value.requestedWorkout)
        assertFalse(viewModel.uiState.value.isWorking)
        assertFalse(viewModel.uiState.value.canResume)
        viewModel.requestResume()
        dispatcher.scheduler.runCurrent()
        assertEquals(1, repository.readCount)
        assertSame(displayed, viewModel.uiState.value.requestedWorkout)
        assertSame(displayed, viewModel.uiState.value.unfinishedWorkout)
        assertEquals(emptyList(), repository.startedWorkouts)
    }

    @Test
    fun staleResumeKeepsTheOriginalIdAndTodayRefreshesOnReturn() = runTest(dispatcher) {
        val displayed = session(Workout.A)
        repository.active = displayed
        val viewModel = createViewModel()
        dispatcher.scheduler.runCurrent()
        repository.active = null
        repository.nextWorkout = Workout.B
        viewModel.requestResume()
        assertSame(displayed, viewModel.uiState.value.requestedWorkout)
        assertEquals(1, repository.readCount)

        viewModel.onWorkoutRequestHandled()
        viewModel.loadWorkout()
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.canStart)
        assertEquals(Workout.B, viewModel.uiState.value.nextWorkout)
        assertNull(viewModel.uiState.value.unfinishedWorkout)
        assertNull(viewModel.uiState.value.requestedWorkout)
        assertEquals(emptyList(), repository.startedWorkouts)
    }

    @Test
    fun loadFailureCanBeRetriedWithoutShowingAnUnverifiedProgram() = runTest(dispatcher) {
        repository.readError = IllegalStateException("Cannot load")
        val viewModel = createViewModel()
        dispatcher.scheduler.runCurrent()
        assertEquals(TodayError.LOAD, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.canStart)
        assertFalse(viewModel.uiState.value.canResume)
        repository.readError = null
        viewModel.loadWorkout()
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.canStart)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun nextWorkoutLoadFailureDoesNotFallBackToAAndCanBeRetried() = runTest(dispatcher) {
        repository.nextWorkout = Workout.B
        repository.nextWorkoutError = IllegalStateException("Cannot select next workout")
        val viewModel = createViewModel()
        dispatcher.scheduler.runCurrent()

        assertEquals(TodayError.LOAD, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.nextWorkout)
        assertFalse(viewModel.uiState.value.canStart)
        assertFalse(viewModel.uiState.value.canResume)
        viewModel.startWorkout()
        assertEquals(emptyList(), repository.startedWorkouts)

        repository.nextWorkoutError = null
        viewModel.loadWorkout()
        dispatcher.scheduler.runCurrent()
        assertEquals(Workout.B, viewModel.uiState.value.nextWorkout)
        assertTrue(viewModel.uiState.value.canStart)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun refreshAfterCompletionCanRetryNextWorkoutSelectionFailure() = runTest(dispatcher) {
        repository.active = session(Workout.A)
        val viewModel = createViewModel()
        dispatcher.scheduler.runCurrent()
        val displayed = viewModel.uiState.value.unfinishedWorkout
        repository.active = null
        repository.nextWorkout = Workout.B
        repository.nextWorkoutError = IllegalStateException("Cannot select next workout")

        viewModel.loadWorkout()
        dispatcher.scheduler.runCurrent()
        assertEquals(TodayError.LOAD, viewModel.uiState.value.error)
        assertSame(displayed, viewModel.uiState.value.unfinishedWorkout)
        assertNull(viewModel.uiState.value.nextWorkout)
        assertNull(viewModel.uiState.value.requestedWorkout)

        repository.nextWorkoutError = null
        viewModel.loadWorkout()
        dispatcher.scheduler.runCurrent()
        assertEquals(Workout.B, viewModel.uiState.value.nextWorkout)
        assertTrue(viewModel.uiState.value.canStart)
        assertNull(viewModel.uiState.value.unfinishedWorkout)
        assertNull(viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.requestedWorkout)
        assertEquals(emptyList(), repository.startedWorkouts)
    }

    @Test
    fun startFailurePreservesDisplayedWorkoutAndAllowsRetry() = runTest(dispatcher) {
        val viewModel = createViewModel()
        dispatcher.scheduler.runCurrent()
        repository.startError = IllegalStateException("Cannot save")
        viewModel.startWorkout()
        dispatcher.scheduler.runCurrent()
        assertEquals(TodayError.START, viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.canStart)
        assertEquals(Workout.A, viewModel.uiState.value.nextWorkout)
        assertNull(viewModel.uiState.value.requestedWorkout)
        repository.startError = null
        viewModel.startWorkout()
        dispatcher.scheduler.runCurrent()
        assertEquals(repository.active, viewModel.uiState.value.requestedWorkout)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun refreshCannotClearAPendingResumeAndDatabaseErrorsDoNotDelayIt() = runTest(dispatcher) {
        repository.active = session(Workout.B)
        val viewModel = createViewModel()
        dispatcher.scheduler.runCurrent()
        repository.readError = IllegalStateException("Cannot load")
        viewModel.requestResume()
        viewModel.loadWorkout()
        viewModel.startWorkout()
        dispatcher.scheduler.runCurrent()
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.canResume)
        assertSame(repository.active, viewModel.uiState.value.unfinishedWorkout)
        assertSame(repository.active, viewModel.uiState.value.requestedWorkout)
        assertEquals(1, repository.readCount)
        assertEquals(emptyList(), repository.startedWorkouts)
    }

    @Test
    fun repeatedLoadsAreCoalescedAndOwnerClearingCancelsLoad() = runTest(dispatcher) {
        repository.readGate = CompletableDeferred()
        val viewModel = createViewModel()
        viewModel.loadWorkout()
        dispatcher.scheduler.runCurrent()
        assertEquals(1, repository.readCount)
        stores.single().clear()
        dispatcher.scheduler.runCurrent()
        assertTrue(repository.readCancelled)
        assertNull(viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.requestedWorkout)
    }

    @Test
    fun ownerClearingCancelsNextWorkoutSelection() = runTest(dispatcher) {
        repository.nextWorkoutGate = CompletableDeferred()
        val viewModel = createViewModel()
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(1, repository.nextWorkoutReadCount)

        stores.single().clear()
        dispatcher.scheduler.runCurrent()
        assertTrue(repository.nextWorkoutReadCancelled)
        assertNull(viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.requestedWorkout)
        assertFalse(viewModel.uiState.value.canStart)
    }

    @OptIn(KoinInternalApi::class)
    @Test
    fun featureGraphResolvesOwnerScopedTodayViewModels() = runTest(dispatcher) {
        val application = koinApplication {
            modules(workoutModule, module { single<WorkoutRepository> { repository } })
        }
        fun resolve(store: ViewModelStore): TodayViewModel = resolveViewModel(
            vmClass = TodayViewModel::class,
            viewModelStore = store,
            extras = CreationExtras.Empty,
            scope = application.koin.scopeRegistry.rootScope,
        )
        try {
            val firstStore = newStore()
            val first = resolve(firstStore)
            assertSame(first, resolve(firstStore))
            assertNotSame(first, resolve(newStore()))
            dispatcher.scheduler.runCurrent()
            assertTrue(first.uiState.value.canStart)
        } finally {
            stores.forEach { it.clear() }
            application.close()
        }
    }

    @Test
    fun refreshAfterCompletionShowsTheNextProgramAndItsSavedWeights() = runTest(dispatcher) {
        val active = session(Workout.A)
        repository.active = active
        val viewModel = createViewModel()
        dispatcher.scheduler.runCurrent()
        assertSame(active, viewModel.uiState.value.unfinishedWorkout)

        repository.active = null
        repository.nextWorkout = Workout.B
        val defaults = WorkoutPrescription(Workout.B)
        val next = defaults.copy(exercises = defaults.exercises.map { it.copy(weightKg = 32.5) })
        repository.nextPrescription = next
        viewModel.loadWorkout()
        dispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.canStart)
        assertNull(viewModel.uiState.value.unfinishedWorkout)
        assertEquals(Workout.B, viewModel.uiState.value.nextWorkout)
        assertSame(next, viewModel.uiState.value.nextWorkoutPrescription)
        assertEquals(listOf(32.5, 32.5, 32.5), viewModel.uiState.value.nextWorkoutPrescription?.exercises?.map { it.weightKg })
        assertNull(viewModel.uiState.value.requestedWorkout)
    }

    private fun newStore(): ViewModelStore = ViewModelStore().also(stores::add)

    private fun createViewModel(): TodayViewModel =
        TodayViewModel(repository).also { newStore().put("today", it) }

    private class FakeWorkoutRepository : WorkoutRepository {
        var active: UnfinishedWorkout? = null
        var nextWorkout = Workout.A
        var nextPrescription: WorkoutPrescription? = null
        var readCount = 0
        var nextWorkoutReadCount = 0
        var readError: Exception? = null
        var nextWorkoutError: Exception? = null
        var startError: Exception? = null
        var readGate: CompletableDeferred<Unit>? = null
        var nextWorkoutGate: CompletableDeferred<Unit>? = null
        var startGate: CompletableDeferred<Unit>? = null
        var readCancelled = false
        var nextWorkoutReadCancelled = false
        val startedWorkouts = mutableListOf<Workout>()

        override suspend fun getUnfinishedWorkout(): UnfinishedWorkout? {
            readCount++
            try {
                readGate?.await()
            } catch (cancelled: CancellationException) {
                readCancelled = true
                throw cancelled
            }
            readError?.let { throw it }
            return active
        }

        override suspend fun getNextWorkout(): Workout {
            nextWorkoutReadCount++
            try {
                nextWorkoutGate?.await()
            } catch (cancelled: CancellationException) {
                nextWorkoutReadCancelled = true
                throw cancelled
            }
            nextWorkoutError?.let { throw it }
            return nextWorkout
        }

        override suspend fun getNextWorkoutPrescription(): WorkoutPrescription {
            val selected = getNextWorkout()
            return nextPrescription ?: WorkoutPrescription(selected)
        }

        override suspend fun getCompletedWorkout(workoutId: Long): CompletedWorkout? =
            error("Today does not load a completed-session summary")

        override suspend fun startWorkout(workout: Workout): UnfinishedWorkout {
            startedWorkouts += workout
            startGate?.await()
            startError?.let { throw it }
            return active ?: session(workout).also { active = it }
        }

        override suspend fun setSetCompleted(
            exercisePosition: Int,
            setPosition: Int,
            isCompleted: Boolean,
        ): Boolean = error("Today does not change set completion")

        override suspend fun finalizeWorkout(workoutId: Long): Boolean =
            error("Today does not finalize workouts")

        override suspend fun completeSetAndStartRest(
            workoutId: Long,
            exercisePosition: Int,
            setPosition: Int,
            restDurationMillis: Long,
        ): UnfinishedWorkout? = error("Today does not complete sets or start rest")
    }

    companion object {
        private fun session(workout: Workout) = UnfinishedWorkout(
            workout = workout,
            startedAtEpochMillis = 123L,
            exercises = workout.exercises.map {
                UnfinishedWorkoutExercise(it, it.sets, it.reps, it.startingWeightKg)
            },
            id = 1L,
        )
    }
}
