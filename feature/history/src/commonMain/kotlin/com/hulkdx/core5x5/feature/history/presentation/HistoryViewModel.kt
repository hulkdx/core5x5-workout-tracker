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

internal class HistoryViewModel(
    private val source: CompletedWorkoutSource,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadHistory()
    }

    fun loadHistory() {
        if (loadJob?.isActive == true) return
        _uiState.value = uiState.value.copy(isLoading = true, error = null)
        loadJob = viewModelScope.launch {
            try {
                _uiState.value = uiState.value.copy(
                    isLoading = false,
                    completedWorkouts = source.getCompletedWorkouts(),
                    error = null,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = uiState.value.copy(isLoading = false, error = HistoryError.LOAD)
            }
        }
    }
}
