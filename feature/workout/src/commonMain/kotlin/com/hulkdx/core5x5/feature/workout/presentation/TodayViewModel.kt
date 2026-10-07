package com.hulkdx.core5x5.feature.workout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout
import com.hulkdx.core5x5.feature.workout.domain.WorkoutRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class TodayViewModel(
    private val repository: WorkoutRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()
    private var operationInProgress = false

    init {
        loadWorkout()
    }

    fun loadWorkout() {
        runOperation(TodayError.LOAD, loading = true) {
            showWorkout(repository.getUnfinishedWorkout())
        }
    }

    fun startWorkout() {
        if (!uiState.value.canStart) return
        val displayed = uiState.value.nextWorkout ?: return
        runOperation(TodayError.START) {
            val workout = repository.startWorkout(displayed)
            showWorkout(workout, requested = true)
        }
    }

    fun requestResume() {
        val state = uiState.value
        if (!state.canResume) return
        // Active Workout validates this identity and reloads durable progress after navigation.
        _uiState.value = state.copy(requestedWorkout = state.unfinishedWorkout, error = null)
    }

    fun onWorkoutRequestHandled() {
        _uiState.value = uiState.value.copy(requestedWorkout = null)
    }

    private suspend fun showWorkout(workout: UnfinishedWorkout?, requested: Boolean = false) {
        val nextWorkout = if (workout == null) repository.getNextWorkoutPrescription() else null
        _uiState.value = uiState.value.copy(
            nextWorkout = nextWorkout?.workout,
            nextWorkoutPrescription = nextWorkout,
            unfinishedWorkout = workout,
            requestedWorkout = if (requested) workout else null,
            error = null,
        )
    }

    private fun runOperation(
        error: TodayError,
        loading: Boolean = false,
        block: suspend () -> Unit,
    ) {
        if (operationInProgress || uiState.value.requestedWorkout != null) return
        operationInProgress = true
        _uiState.value = uiState.value.copy(
            isLoading = loading,
            isWorking = !loading,
            error = null,
            requestedWorkout = null,
        )
        viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = uiState.value.copy(error = error)
            } finally {
                operationInProgress = false
                _uiState.value = uiState.value.copy(isLoading = false, isWorking = false)
            }
        }
    }
}
