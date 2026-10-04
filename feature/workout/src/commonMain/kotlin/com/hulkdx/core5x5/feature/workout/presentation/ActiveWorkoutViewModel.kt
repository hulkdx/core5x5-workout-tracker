package com.hulkdx.core5x5.feature.workout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hulkdx.core5x5.feature.workout.domain.WorkoutRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class ActiveWorkoutViewModel(
    private val repository: WorkoutRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _uiState.value = ActiveWorkoutUiState(
                    isLoading = false,
                    unfinishedWorkout = repository.getUnfinishedWorkout(),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = ActiveWorkoutUiState(isLoading = false, hasLoadError = true)
            }
        }
    }
}
