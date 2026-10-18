package com.hulkdx.core5x5.feature.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import com.hulkdx.core5x5.core.training.domain.CompletedExerciseType
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val source: CompletedWorkoutSource,
    private val preferences: TrainingPreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    private var preferencesJob: Job? = null

    init {
        retryPreferences()
        loadHistory()
    }

    fun selectExercise(exercise: CompletedExerciseType) {
        _uiState.value = uiState.value.copy(selectedExercise = exercise)
    }

    /** Subscribe once per owner; only an explicit retry restarts a failed observation. */
    fun retryPreferences() {
        if (preferencesJob?.isActive == true) return
        preferencesJob = viewModelScope.launch {
            try {
                preferences.preferences.collect { preferences ->
                    _uiState.value = uiState.value.copy(weightUnit = preferences.weightUnit, hasPreferencesError = false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = uiState.value.copy(hasPreferencesError = true)
            }
        }
    }

    fun loadHistory() {
        if (loadJob?.isActive == true) return
        _uiState.value = uiState.value.copy(isLoading = true, error = null)
        loadJob = viewModelScope.launch {
            try {
                val workouts = source.getCompletedWorkouts()
                _uiState.value = uiState.value.copy(
                    isLoading = false,
                    completedWorkouts = workouts,
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
