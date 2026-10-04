package com.hulkdx.core5x5.feature.workout.presentation

import androidx.lifecycle.ViewModelStore
import com.hulkdx.core5x5.feature.workout.domain.Exercise
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutExercise
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.feature.workout.domain.WorkoutRepository
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

    private class FakeWorkoutRepository(
        private val savedWorkout: UnfinishedWorkout,
    ) : WorkoutRepository {
        var readCount = 0

        override suspend fun getUnfinishedWorkout(): UnfinishedWorkout? {
            readCount++
            return savedWorkout
        }

        override suspend fun startWorkout(workout: Workout): UnfinishedWorkout =
            error("Active Workout only loads an existing session")
    }
}
