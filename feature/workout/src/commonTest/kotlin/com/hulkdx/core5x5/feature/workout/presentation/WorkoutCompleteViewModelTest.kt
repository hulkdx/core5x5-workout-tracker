package com.hulkdx.core5x5.feature.workout.presentation

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import com.hulkdx.core5x5.feature.workout.di.workoutModule
import com.hulkdx.core5x5.feature.workout.domain.CompletedWorkout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutExercise
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutSet
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
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.parameter.parametersOf
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
internal class WorkoutCompleteViewModelTest {
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
    fun summariesUseTheSavedAAndBSessionsWithTheirActualTotalsAndPartialCompletion() = runTest(dispatcher) {
        for ((workout, prescribed) in listOf(Workout.A to 15, Workout.B to 11)) {
            val saved = completed(workout, id = prescribed.toLong(), completedSets = 7)
            val next = WorkoutPrescription(workout.nextWorkout())
            repository.completed[saved.id] = saved
            repository.prescription = next
            val viewModel = createViewModel(saved.id)
            assertTrue(viewModel.uiState.value.isLoading)
            dispatcher.scheduler.runCurrent()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertSame(saved, state.completedWorkout)
            assertSame(next, state.nextWorkout)
            assertEquals(7, state.completedWorkout?.completedSets)
            assertEquals(prescribed, state.completedWorkout?.prescribedSets)
            assertEquals("42:18", state.completedWorkout?.durationMillis?.formatWorkoutDuration())
            assertEquals(workout.nextWorkout().exercises, state.nextWorkout?.exercises?.map { it.exercise })
        }
        assertEquals(listOf(15L, 11L), repository.readIds)
    }

    @Test
    fun savedPrescriptionsAndNextWeightsPassThroughWithoutProgressionOrDefaultReplacement() = runTest(dispatcher) {
        val original = completed(Workout.B, id = 42L, completedSets = 1)
        val saved = original.copy(exercises = original.exercises.mapIndexed { index, exercise ->
            exercise.copy(weightKg = 22.5 + index * 2.5)
        })
        repository.completed[saved.id] = saved
        val defaults = WorkoutPrescription(Workout.A)
        val next = defaults.copy(exercises = defaults.exercises.map { it.copy(weightKg = 32.5) })
        repository.prescription = next
        val viewModel = createViewModel(saved.id)
        dispatcher.scheduler.runCurrent()

        assertSame(saved, viewModel.uiState.value.completedWorkout)
        assertSame(next, viewModel.uiState.value.nextWorkout)
        assertEquals(listOf(32.5, 32.5, 32.5), viewModel.uiState.value.nextWorkout?.exercises?.map { it.weightKg })
        assertEquals(1, viewModel.uiState.value.completedWorkout?.completedSets)
    }

    @Test
    fun missingOrActiveSessionIdNeverProducesASuccessState() = runTest(dispatcher) {
        val viewModel = createViewModel(404L)
        dispatcher.scheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(WorkoutCompleteError.NOT_FOUND, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.completedWorkout)
        assertNull(viewModel.uiState.value.nextWorkout)
        assertEquals(0, repository.prescriptionReads)
    }

    @Test
    fun summaryLoadFailureCanBeRetriedAndDoesNotCreateOrFinalizeASession() = runTest(dispatcher) {
        val saved = completed(Workout.A)
        repository.completed[saved.id] = saved
        repository.readError = IllegalStateException("Read failed")
        val viewModel = createViewModel(saved.id)
        dispatcher.scheduler.runCurrent()

        assertEquals(WorkoutCompleteError.LOAD, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.completedWorkout)
        repository.readError = null
        viewModel.loadWorkout()
        dispatcher.scheduler.runCurrent()

        assertNull(viewModel.uiState.value.error)
        assertSame(saved, viewModel.uiState.value.completedWorkout)
        assertEquals(listOf(saved.id, saved.id), repository.readIds)
    }

    @Test
    fun nextPrescriptionFailureDoesNotFabricateDefaultWeightsAndCanRetry() = runTest(dispatcher) {
        val saved = completed(Workout.A)
        repository.completed[saved.id] = saved
        repository.prescriptionError = IllegalStateException("Cannot load next prescription")
        val viewModel = createViewModel(saved.id)
        dispatcher.scheduler.runCurrent()

        assertEquals(WorkoutCompleteError.LOAD, viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.nextWorkout)
        repository.prescriptionError = null
        viewModel.loadWorkout()
        dispatcher.scheduler.runCurrent()

        assertNull(viewModel.uiState.value.error)
        assertSame(saved, viewModel.uiState.value.completedWorkout)
        assertEquals(2, repository.prescriptionReads)
    }

    @Test
    fun repeatedLoadRequestsAreIgnoredWhileAReadIsInFlight() = runTest(dispatcher) {
        val saved = completed(Workout.B)
        repository.completed[saved.id] = saved
        repository.readGate = CompletableDeferred()
        val viewModel = createViewModel(saved.id)
        viewModel.loadWorkout()
        dispatcher.scheduler.runCurrent()
        viewModel.loadWorkout()

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(listOf(saved.id), repository.readIds)
        assertNull(viewModel.uiState.value.completedWorkout)
        repository.readGate?.complete(Unit)
        dispatcher.scheduler.runCurrent()
        assertSame(saved, viewModel.uiState.value.completedWorkout)
    }

    @Test
    fun recreatedViewModelReloadsTheSameSavedIdInsteadOfUsingTheLatestSession() = runTest(dispatcher) {
        val original = completed(Workout.A, id = 41L)
        val later = completed(Workout.B, id = 42L)
        repository.completed[original.id] = original
        repository.completed[later.id] = later
        val first = createViewModel(original.id)
        dispatcher.scheduler.runCurrent()
        val recreated = createViewModel(original.id)
        dispatcher.scheduler.runCurrent()

        assertNotSame(first, recreated)
        assertSame(original, first.uiState.value.completedWorkout)
        assertSame(original, recreated.uiState.value.completedWorkout)
        assertEquals(listOf(original.id, original.id), repository.readIds)
    }

    @Test
    fun ownerCleanupCancelsSummaryLoadingWithoutReportingAFailure() = runTest(dispatcher) {
        repository.readGate = CompletableDeferred()
        val viewModel = createViewModel(1L)
        dispatcher.scheduler.runCurrent()
        stores.forEach { it.clear() }
        dispatcher.scheduler.runCurrent()

        assertTrue(repository.readCancelled)
        assertNull(viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.completedWorkout)
    }

    @OptIn(KoinInternalApi::class)
    @Test
    fun featureGraphScopesCompletionViewModelsToOwnersAndInjectsTheDestinationId() = runTest(dispatcher) {
        val saved = completed(Workout.B, id = 42L)
        repository.completed[saved.id] = saved
        val application = koinApplication {
            modules(workoutModule, module { single<WorkoutRepository> { repository } })
        }
        fun resolve(store: ViewModelStore) = resolveViewModel(
            vmClass = WorkoutCompleteViewModel::class,
            viewModelStore = store,
            extras = CreationExtras.Empty,
            scope = application.koin.scopeRegistry.rootScope,
            parameters = { parametersOf(saved.id) },
        )
        try {
            val owner = newStore()
            val first = resolve(owner)
            assertSame(first, resolve(owner))
            assertNotSame(first, resolve(newStore()))
            dispatcher.scheduler.runCurrent()
            assertSame(saved, first.uiState.value.completedWorkout)
            assertEquals(listOf(saved.id, saved.id), repository.readIds)
        } finally {
            stores.forEach { it.clear() }
            application.close()
        }
    }

    private fun newStore() = ViewModelStore().also(stores::add)

    private fun createViewModel(id: Long) =
        WorkoutCompleteViewModel(id, repository).also { newStore().put("completion", it) }

    private class FakeWorkoutRepository : WorkoutRepository {
        val completed = mutableMapOf<Long, CompletedWorkout>()
        var prescription = WorkoutPrescription(Workout.B)
        var readError: Exception? = null
        var prescriptionError: Exception? = null
        var readGate: CompletableDeferred<Unit>? = null
        var readCancelled = false
        var prescriptionReads = 0
        val readIds = mutableListOf<Long>()

        override suspend fun getCompletedWorkout(workoutId: Long): CompletedWorkout? {
            readIds += workoutId
            try {
                readGate?.await()
            } catch (cancelled: CancellationException) {
                readCancelled = true
                throw cancelled
            }
            readError?.let { throw it }
            return completed[workoutId]
        }

        override suspend fun getNextWorkoutPrescription(workoutOverride: Workout?): WorkoutPrescription {
            prescriptionReads++
            prescriptionError?.let { throw it }
            return prescription
        }

        override suspend fun getUnfinishedWorkout(): UnfinishedWorkout? = error("Summary reads by completed ID")
        override suspend fun getNextWorkout(): Workout = error("Summary loads saved next-workout weights")
        override suspend fun startWorkout(workout: Workout): UnfinishedWorkout = error("Summary cannot start a session")
        override suspend fun finalizeWorkout(workoutId: Long): Boolean = error("Summary must never save again")
        override suspend fun setSetCompleted(exercisePosition: Int, setPosition: Int, isCompleted: Boolean): Boolean =
            error("Summary cannot edit logged sets")

        override suspend fun editExercise(
            workoutId: Long, exercisePosition: Int,
            edit: com.hulkdx.core5x5.feature.workout.domain.ExerciseEdit,
        ): UnfinishedWorkout? = error("Editing is not used by this fixture")

        override suspend fun completeSetAndStartRest(
            workoutId: Long,
            exercisePosition: Int,
            setPosition: Int,
            restDurationMillis: Long,
        ): UnfinishedWorkout? = error("Summary cannot edit sets or start rest")
    }

    companion object {
        private fun completed(workout: Workout, id: Long = 1L, completedSets: Int = 0): CompletedWorkout {
            var remaining = completedSets
            return CompletedWorkout(
                id = id,
                workout = workout,
                startedAtEpochMillis = 1_000L,
                completedAtEpochMillis = 2_539_000L,
                exercises = workout.exercises.map { exercise ->
                    UnfinishedWorkoutExercise(
                        exercise, exercise.sets, exercise.reps, exercise.startingWeightKg,
                        List(exercise.sets) { UnfinishedWorkoutSet(it, isCompleted = remaining-- > 0) },
                    )
                },
            )
        }
    }
}
