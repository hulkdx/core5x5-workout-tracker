package com.hulkdx.core5x5.feature.workout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hulkdx.core5x5.feature.workout.domain.WorkoutRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class WorkoutCompleteViewModel(
    private val workoutId: Long,
    private val repository: WorkoutRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutCompleteUiState())
    val uiState: StateFlow<WorkoutCompleteUiState> = _uiState.asStateFlow()
    private var isLoading = false

    init {
        loadWorkout()
    }

    fun loadWorkout() {
        if (isLoading) return
        isLoading = true
        _uiState.value = WorkoutCompleteUiState()
        viewModelScope.launch {
            try {
                val completedWorkout = repository.getCompletedWorkout(workoutId)
                _uiState.value = if (completedWorkout == null) {
                    WorkoutCompleteUiState(isLoading = false, error = WorkoutCompleteError.NOT_FOUND)
                } else {
                    WorkoutCompleteUiState(
                        isLoading = false,
                        completedWorkout = completedWorkout,
                        nextWorkout = repository.getNextWorkoutPrescription(),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = WorkoutCompleteUiState(isLoading = false, error = WorkoutCompleteError.LOAD)
            } finally {
                isLoading = false
            }
        }
    }
}
