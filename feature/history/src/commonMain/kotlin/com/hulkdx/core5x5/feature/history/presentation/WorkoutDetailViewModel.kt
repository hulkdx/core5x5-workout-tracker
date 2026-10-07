package com.hulkdx.core5x5.feature.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class WorkoutDetailViewModel(
    private val source: CompletedWorkoutSource,
    workoutId: Long,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutDetailUiState(workoutId = workoutId))
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadWorkout()
    }

    fun loadWorkout() {
        if (loadJob?.isActive == true) return
        _uiState.value = uiState.value.copy(isLoading = true, error = null)
        loadJob = viewModelScope.launch {
            try {
                val completedWorkout = source.getCompletedWorkoutById(uiState.value.workoutId)
                _uiState.value = uiState.value.copy(
                    isLoading = false,
                    completedWorkout = completedWorkout,
                    error = if (completedWorkout == null) WorkoutDetailError.NOT_FOUND else null,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = uiState.value.copy(isLoading = false, error = WorkoutDetailError.LOAD)
            }
        }
    }
}
