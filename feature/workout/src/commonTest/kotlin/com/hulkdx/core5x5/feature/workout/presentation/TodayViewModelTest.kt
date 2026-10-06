package com.hulkdx.core5x5.feature.workout.presentation

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import com.hulkdx.core5x5.feature.workout.di.workoutModule
import com.hulkdx.core5x5.feature.workout.domain.Exercise
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
    }

    @Test
    fun activeWorkoutIsResumableWithoutAutomaticallyRequestingIt() = runTest(dispatcher) {
        val active = session(Workout.B)
        repository.active = active
        val viewModel = createViewModel()
        dispatcher.scheduler.runCurrent()

        assertSame(active, viewModel.uiState.value.unfinishedWorkout)
        assertTrue(viewModel.uiState.value.canResume)
        assertFalse(viewModel.uiState.value.canStart)
        assertNull(viewModel.uiState.value.nextWorkout)
        assertNull(viewModel.uiState.value.requestedWorkout)
        viewModel.startWorkout()
        assertEquals(emptyList(), repository.startedWorkouts)
    }

    @Test
    fun startUsesDisplayedWorkoutAndPublishesReturnedSession() = runTest(dispatcher) {
        val viewModel = createViewModel(Workout.B)
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
    fun resumeReloadsSavedSessionAndNeverStartsWorkout() = runTest(dispatcher) {
        repository.active = session(Workout.B)
        val viewModel = createViewModel()
        dispatcher.scheduler.runCurrent()
        val updated = session(Workout.B).copy(startedAtEpochMillis = 456L)
        repository.active = updated
        repository.readGate = CompletableDeferred()
        viewModel.requestResume()
        viewModel.requestResume()
        dispatcher.scheduler.runCurrent()
        assertEquals(2, repository.readCount)
        assertTrue(viewModel.uiState.value.isWorking)
        repository.readGate?.complete(Unit)
        dispatcher.scheduler.runCurrent()
        assertSame(updated, viewModel.uiState.value.requestedWorkout)
        assertSame(updated, viewModel.uiState.value.unfinishedWorkout)
        assertEquals(emptyList(), repository.startedWorkouts)
    }

    @Test
    fun resumeWithNoLongerExistingSessionReturnsToReady() = runTest(dispatcher) {
        repository.active = session(Workout.A)
        val viewModel = createViewModel()
        dispatcher.scheduler.runCurrent()
        repository.active = null
        viewModel.requestResume()
        dispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.canStart)
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
    fun resumeFailurePreservesActiveSessionAndAllowsRetry() = runTest(dispatcher) {
        repository.active = session(Workout.B)
        val viewModel = createViewModel()
        dispatcher.scheduler.runCurrent()
        repository.readError = IllegalStateException("Cannot load")
        viewModel.requestResume()
        dispatcher.scheduler.runCurrent()
        assertEquals(TodayError.RESUME, viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.canResume)
        assertSame(repository.active, viewModel.uiState.value.unfinishedWorkout)
        assertNull(viewModel.uiState.value.requestedWorkout)
        repository.readError = null
        viewModel.requestResume()
        dispatcher.scheduler.runCurrent()
        assertSame(repository.active, viewModel.uiState.value.requestedWorkout)
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

    private fun newStore(): ViewModelStore = ViewModelStore().also(stores::add)

    private fun createViewModel(nextWorkout: Workout = Workout.A): TodayViewModel =
        TodayViewModel(repository, nextWorkout).also { newStore().put("today", it) }

    private class FakeWorkoutRepository : WorkoutRepository {
        var active: UnfinishedWorkout? = null
        var readCount = 0
        var readError: Exception? = null
        var startError: Exception? = null
        var readGate: CompletableDeferred<Unit>? = null
        var startGate: CompletableDeferred<Unit>? = null
        var readCancelled = false
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
