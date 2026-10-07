package com.hulkdx.core5x5.feature.workout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import com.hulkdx.core5x5.feature.workout.domain.RestTimerRules
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout
import com.hulkdx.core5x5.feature.workout.domain.WorkoutRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock

internal class ActiveWorkoutViewModel(
    private val repository: WorkoutRepository,
    private val preferences: TrainingPreferencesRepository,
    private val workoutId: Long? = null,
    private val restTimerRules: RestTimerRules = RestTimerRules { Clock.System.now().toEpochMilliseconds() },
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()
    private var operationInProgress = false
    private var countdownActive = false
    private var countdownJob: Job? = null
    private var reloadOnResume = false

    init {
        loadWorkout()
    }

    fun loadWorkout() {
        if (operationInProgress) return
        operationInProgress = true
        val previous = uiState.value
        _uiState.value = previous.copy(isLoading = true, hasLoadError = false)
        viewModelScope.launch {
            try {
                val active = repository.getUnfinishedWorkout()
                val completed = if (workoutId != null && active?.id != workoutId) {
                    repository.getCompletedWorkout(workoutId)
                } else {
                    null
                }
                val identified = active?.takeIf { workoutId == null || it.id == workoutId }
                _uiState.value = ActiveWorkoutUiState(
                    isLoading = false,
                    unfinishedWorkout = identified,
                    selectedExercisePosition = if (identified != null && identified.id == previous.unfinishedWorkout?.id) {
                        previous.selectedExercisePosition.coerceIn(0, identified.exercises.lastIndex.coerceAtLeast(0))
                    } else {
                        identified?.nextExercisePosition() ?: 0
                    },
                    requestedCompletedWorkoutId = completed?.id,
                )
                updateCountdown()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = uiState.value.copy(isLoading = false, hasLoadError = true)
            } finally {
                operationInProgress = false
            }
        }
    }

    /** Construction loads once; a later return reloads durable progress and elapsed rest. */
    fun onResume() {
        countdownActive = true
        updateCountdown()
        if (reloadOnResume) {
            reloadOnResume = false
            loadWorkout()
        }
    }

    fun onPause() {
        reloadOnResume = true
        countdownActive = false
        countdownJob?.cancel()
        countdownJob = null
    }

    fun selectExercise(position: Int) {
        if (operationInProgress || !uiState.value.canCompleteSet) return
        val workout = uiState.value.unfinishedWorkout ?: return
        if (position !in workout.exercises.indices) return
        _uiState.value = uiState.value.copy(selectedExercisePosition = position)
    }

    fun completeNextSet() {
        val state = uiState.value
        val exercise = state.unfinishedWorkout?.exercises?.getOrNull(state.selectedExercisePosition) ?: return
        val nextSet = exercise.setStates.firstOrNull { !it.isCompleted } ?: return
        completeSet(state.selectedExercisePosition, nextSet.position)
    }

    fun completeSet(exercisePosition: Int, setPosition: Int) {
        val state = uiState.value
        if (operationInProgress || !state.canCompleteSet) return
        val workout = state.unfinishedWorkout ?: return
        val set = workout.exercises.getOrNull(exercisePosition)?.setStates
            ?.firstOrNull { it.position == setPosition } ?: return
        if (set.isCompleted) return
        operationInProgress = true
        _uiState.value = state.copy(isCompletingSet = true, hasSetSaveError = false)
        viewModelScope.launch {
            try {
                val restDurationMillis = preferences.getPreferences().restDurationMillis
                val saved = repository.completeSetAndStartRest(
                    workout.id, exercisePosition, setPosition, restDurationMillis,
                )
                if (saved == null || saved.id != workout.id) {
                    _uiState.value = uiState.value.copy(hasSetSaveError = true)
                } else {
                    val selected = if (saved.exercises.getOrNull(exercisePosition)?.setStates?.any { !it.isCompleted } == true) {
                        exercisePosition
                    } else {
                        saved.nextExercisePosition()
                    }
                    _uiState.value = uiState.value.copy(
                        unfinishedWorkout = saved,
                        selectedExercisePosition = selected,
                    )
                    updateCountdown()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = uiState.value.copy(hasSetSaveError = true)
            } finally {
                operationInProgress = false
                _uiState.value = uiState.value.copy(isCompletingSet = false)
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
                if (repository.finalizeWorkout(activeId)) clearDisplayedRest()
                val completed = repository.getCompletedWorkout(activeId)
                if (completed != null) clearDisplayedRest()
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

    private fun clearDisplayedRest() {
        _uiState.value = uiState.value.copy(
            unfinishedWorkout = uiState.value.unfinishedWorkout?.copy(restTimer = null),
        )
        updateCountdown()
    }

    private fun updateCountdown() {
        val timer = uiState.value.unfinishedWorkout?.restTimer
        _uiState.value = uiState.value.copy(restTimer = restTimerRules.state(timer).toUiState())
        if (timer == null) {
            countdownJob?.cancel()
            countdownJob = null
        } else if (countdownActive && countdownJob?.isActive != true) {
            countdownJob = viewModelScope.launch {
                // No decrementing counter: sleep/background/clock changes are reflected on each read.
                while (isActive) {
                    delay(1_000)
                    _uiState.value = uiState.value.copy(
                        restTimer = restTimerRules.state(uiState.value.unfinishedWorkout?.restTimer).toUiState(),
                    )
                }
            }
        }
    }
}

private fun UnfinishedWorkout.nextExercisePosition(): Int =
    exercises.indexOfFirst { exercise -> exercise.setStates.any { !it.isCompleted } }
        .takeIf { it >= 0 } ?: exercises.lastIndex.coerceAtLeast(0)
