package com.hulkdx.core5x5.feature.history.presentation

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import com.hulkdx.core5x5.core.training.domain.CompletedExerciseType
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutExercise
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutRecord
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutSet
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutSource
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutType
import com.hulkdx.core5x5.feature.history.di.historyModule
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
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class HistoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val source = FakeCompletedWorkoutSource()
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
    fun initialLoadShowsNewestCompletedSessions() = runTest(dispatcher) {
        source.history = listOf(record(2L, CompletedWorkoutType.B), record(1L, CompletedWorkoutType.A))
        val viewModel = createHistoryViewModel()
        dispatcher.scheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(source.history, viewModel.uiState.value.completedWorkouts)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun emptyHistoryIsAReadyState() = runTest(dispatcher) {
        val viewModel = createHistoryViewModel()
        dispatcher.scheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.completedWorkouts.isEmpty())
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun loadFailurePreservesDataAndRetryRecovers() = runTest(dispatcher) {
        source.history = listOf(record(1L, CompletedWorkoutType.A))
        val viewModel = createHistoryViewModel()
        dispatcher.scheduler.runCurrent()
        source.failure = IllegalStateException("read failed")

        viewModel.loadHistory()
        dispatcher.scheduler.runCurrent()
        assertEquals(HistoryError.LOAD, viewModel.uiState.value.error)
        assertEquals(source.history, viewModel.uiState.value.completedWorkouts)

        source.failure = null
        source.history = listOf(record(2L, CompletedWorkoutType.B))
        viewModel.loadHistory()
        dispatcher.scheduler.runCurrent()
        assertEquals(source.history, viewModel.uiState.value.completedWorkouts)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun overlappingLoadsAreCoalescedAndOwnerCleanupCancelsRead() = runTest(dispatcher) {
        source.readGate = CompletableDeferred()
        val viewModel = createHistoryViewModel()
        viewModel.loadHistory()
        dispatcher.scheduler.runCurrent()

        assertEquals(1, source.historyReadCount)
        stores.single().clear()
        dispatcher.scheduler.runCurrent()
        assertTrue(source.readCancelled)
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun detailLoadsSavedDataAndRejectsMissingIds() = runTest(dispatcher) {
        val saved = record(42L, CompletedWorkoutType.B)
        source.records[saved.id] = saved
        val detail = createDetailViewModel(saved.id)
        dispatcher.scheduler.runCurrent()

        assertFalse(detail.uiState.value.isLoading)
        assertSame(saved, detail.uiState.value.completedWorkout)
        assertNull(detail.uiState.value.error)

        stores.single().clear()
        val missing = createDetailViewModel(99L)
        dispatcher.scheduler.runCurrent()
        assertNull(missing.uiState.value.completedWorkout)
        assertEquals(WorkoutDetailError.NOT_FOUND, missing.uiState.value.error)
    }

    @Test
    fun detailReadFailureCanBeRetriedWithoutInventingData() = runTest(dispatcher) {
        val saved = record(42L, CompletedWorkoutType.A)
        source.records[saved.id] = saved
        source.failure = IllegalStateException("read failed")
        val detail = createDetailViewModel(saved.id)
        dispatcher.scheduler.runCurrent()

        assertEquals(WorkoutDetailError.LOAD, detail.uiState.value.error)
        assertNull(detail.uiState.value.completedWorkout)

        source.failure = null
        detail.loadWorkout()
        dispatcher.scheduler.runCurrent()
        assertSame(saved, detail.uiState.value.completedWorkout)
        assertNull(detail.uiState.value.error)
    }

    @Test
    fun detailOwnerCleanupCancelsRead() = runTest(dispatcher) {
        source.detailGate = CompletableDeferred()
        val detail = createDetailViewModel(1L)
        dispatcher.scheduler.runCurrent()
        stores.single().clear()
        dispatcher.scheduler.runCurrent()

        assertTrue(source.detailReadCancelled)
        assertTrue(detail.uiState.value.isLoading)
    }

    @OptIn(KoinInternalApi::class)
    @Test
    fun featureGraphScopesHistoryViewModelsToTheirDestinationOwners() = runTest(dispatcher) {
        val application = koinApplication {
            modules(historyModule, module { single<CompletedWorkoutSource> { source } })
        }
        try {
            val firstStore = newStore()
            val secondStore = newStore()
            fun resolve(store: ViewModelStore): HistoryViewModel = resolveViewModel(
                vmClass = HistoryViewModel::class,
                viewModelStore = store,
                extras = CreationExtras.Empty,
                scope = application.koin.scopeRegistry.rootScope,
            )

            val first = resolve(firstStore)
            assertSame(first, resolve(firstStore))
            assertTrue(first !== resolve(secondStore))
            dispatcher.scheduler.runCurrent()
            assertFalse(first.uiState.value.isLoading)
        } finally {
            application.close()
        }
    }

    private fun createHistoryViewModel(): HistoryViewModel =
        HistoryViewModel(source).also { newStore().put("history", it) }

    private fun createDetailViewModel(workoutId: Long): WorkoutDetailViewModel =
        WorkoutDetailViewModel(source, workoutId).also { newStore().put("detail:$workoutId", it) }

    private fun newStore(): ViewModelStore = ViewModelStore().also(stores::add)

    private class FakeCompletedWorkoutSource : CompletedWorkoutSource {
        var history = emptyList<CompletedWorkoutRecord>()
        val records = mutableMapOf<Long, CompletedWorkoutRecord>()
        var failure: Exception? = null
        var readGate: CompletableDeferred<Unit>? = null
        var detailGate: CompletableDeferred<Unit>? = null
        var historyReadCount = 0
        var readCancelled = false
        var detailReadCancelled = false

        override suspend fun getCompletedWorkouts(): List<CompletedWorkoutRecord> {
            historyReadCount++
            try {
                readGate?.await()
            } catch (cancelled: CancellationException) {
                readCancelled = true
                throw cancelled
            }
            failure?.let { throw it }
            return history
        }

        override suspend fun getCompletedWorkoutById(workoutId: Long): CompletedWorkoutRecord? {
            try {
                detailGate?.await()
            } catch (cancelled: CancellationException) {
                detailReadCancelled = true
                throw cancelled
            }
            failure?.let { throw it }
            return records[workoutId]
        }
    }

    companion object {
        private fun record(id: Long, workout: CompletedWorkoutType) = CompletedWorkoutRecord(
            id = id,
            workout = workout,
            startedAtEpochMillis = 1_000L,
            completedAtEpochMillis = 61_000L,
            exercises = listOf(
                CompletedWorkoutExercise(
                    exercise = CompletedExerciseType.SQUAT,
                    sets = 5,
                    reps = 5,
                    weightKg = 20.0,
                    setStates = List(5) { CompletedWorkoutSet(it, it == 0) },
                ),
            ),
        )
    }
}
