package com.hulkdx.core5x5.feature.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutDeletion
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
    private val deletion: CompletedWorkoutDeletion,
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

    fun deleteWorkout(workoutId: Long) {
        if (uiState.value.isLoading || uiState.value.isDeleting) return
        if (uiState.value.completedWorkouts.none { it.id == workoutId }) return
        _uiState.value = uiState.value.copy(deletingWorkoutId = workoutId, failedDeletionId = null)
        viewModelScope.launch {
            try {
                deletion.deleteCompletedWorkout(workoutId)
                // A missing/reopened ID is also absent from completed History. Remove it before
                // refreshing so a secondary read failure cannot restore a successfully deleted row.
                _uiState.value = uiState.value.copy(
                    completedWorkouts = uiState.value.completedWorkouts.filterNot { it.id == workoutId },
                    deletingWorkoutId = null,
                )
                loadHistory()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Keep the row for an explicit, idempotent retry after a failure or lost reply.
                _uiState.value = uiState.value.copy(deletingWorkoutId = null, failedDeletionId = workoutId)
            }
        }
    }

    fun loadHistory() {
        // Reads and deletes cannot overlap and republish stale snapshots.
        if (loadJob?.isActive == true || uiState.value.isDeleting) return
        _uiState.value = uiState.value.copy(isLoading = true, error = null)
        loadJob = viewModelScope.launch {
            try {
                val workouts = source.getCompletedWorkouts()
                _uiState.value = uiState.value.copy(
                    isLoading = false,
                    completedWorkouts = workouts,
                    failedDeletionId = uiState.value.failedDeletionId?.takeIf { id -> workouts.any { it.id == id } },
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
