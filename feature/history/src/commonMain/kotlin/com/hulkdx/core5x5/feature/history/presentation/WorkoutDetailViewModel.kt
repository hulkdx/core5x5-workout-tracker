package com.hulkdx.core5x5.feature.history.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class WorkoutDetailViewModel(workoutId: Long) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutDetailUiState(workoutId = workoutId))
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState.asStateFlow()
}
