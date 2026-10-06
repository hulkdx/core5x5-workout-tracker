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
    private val workoutId: Long? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()
    private var operationInProgress = false

    init {
        loadWorkout()
    }

    fun loadWorkout() {
        if (operationInProgress) return
        operationInProgress = true
        _uiState.value = ActiveWorkoutUiState()
        viewModelScope.launch {
            try {
                val active = repository.getUnfinishedWorkout()
                val completed = if (workoutId != null && active?.id != workoutId) {
                    repository.getCompletedWorkout(workoutId)
                } else {
                    null
                }
                _uiState.value = ActiveWorkoutUiState(
                    isLoading = false,
                    unfinishedWorkout = active?.takeIf { workoutId == null || it.id == workoutId },
                    requestedCompletedWorkoutId = completed?.id,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = ActiveWorkoutUiState(isLoading = false, hasLoadError = true)
            } finally {
                operationInProgress = false
            }
        }
    }

    fun finishWorkout() {
        if (operationInProgress || !uiState.value.canFinish) return
        val activeId = uiState.value.unfinishedWorkout?.id ?: return
        operationInProgress = true
        _uiState.value = uiState.value.copy(isSaving = true, hasSaveError = false)
        viewModelScope.launch {
            try {
                // A false result can be an already-saved retry; the persisted read is authoritative.
                repository.finalizeWorkout(activeId)
                val completed = repository.getCompletedWorkout(activeId)
                _uiState.value = uiState.value.copy(
                    hasSaveError = completed == null,
                    requestedCompletedWorkoutId = completed?.id,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = uiState.value.copy(hasSaveError = true)
            } finally {
                operationInProgress = false
                _uiState.value = uiState.value.copy(isSaving = false)
            }
        }
    }

    fun onCompletionRequestHandled() {
        _uiState.value = uiState.value.copy(requestedCompletedWorkoutId = null)
    }
}
