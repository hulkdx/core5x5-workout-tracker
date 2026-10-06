package com.hulkdx.core5x5.feature.history.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class HistoryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
}
