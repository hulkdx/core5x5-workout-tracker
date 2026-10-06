package com.hulkdx.core5x5.feature.workout.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class RestTimerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RestTimerUiState)
    val uiState: StateFlow<RestTimerUiState> = _uiState.asStateFlow()
}
