package com.hulkdx.core5x5.feature.workout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
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
                    exerciseEdit = previous.exerciseEdit?.takeIf { identified?.id == previous.unfinishedWorkout?.id },
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
        saveSet(exercisePosition, setPosition, isCompleted = true)
    }

    fun toggleSet(exercisePosition: Int, setPosition: Int) {
        val set = uiState.value.unfinishedWorkout?.exercises?.getOrNull(exercisePosition)?.setStates
            ?.firstOrNull { it.position == setPosition } ?: return
        saveSet(exercisePosition, setPosition, isCompleted = !set.isCompleted)
    }

    private fun saveSet(exercisePosition: Int, setPosition: Int, isCompleted: Boolean) {
        val state = uiState.value
        if (operationInProgress || !state.canCompleteSet) return
        val workout = state.unfinishedWorkout ?: return
        val set = workout.exercises.getOrNull(exercisePosition)?.setStates
            ?.firstOrNull { it.position == setPosition } ?: return
        if (set.isCompleted == isCompleted) return
        operationInProgress = true
        _uiState.value = state.copy(isCompletingSet = true, hasSetSaveError = false)
        viewModelScope.launch {
            try {
                val saved = if (isCompleted) {
                    val restOverride = workout.exercises[exercisePosition].restDurationMillis
                    val restDurationMillis = restOverride?.coerceAtLeast(1L) ?: preferences.getPreferences().restDurationMillis
                    repository.completeSetAndStartRest(
                        workout.id, exercisePosition, setPosition, restDurationMillis,
                    )
                } else {
                    repository.undoSetCompletion(workout.id, exercisePosition, setPosition)
                }
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

    fun openExerciseEdit(position: Int, unit: WeightUnit) {
        val state = uiState.value
        if (operationInProgress || !state.canCompleteSet) return
        val exercise = state.unfinishedWorkout?.exercises?.getOrNull(position) ?: return
        operationInProgress = true
        viewModelScope.launch {
            try {
                val duration = exercise.restDurationMillis ?: preferences.getPreferences().restDurationMillis
                _uiState.value = uiState.value.copy(hasEditLoadError = false, exerciseEdit = ExerciseEditUiState(
                    position = position, originalName = exercise.displayName(), name = exercise.displayName(),
                    weightKg = exercise.weightKg, unit = unit, sets = exercise.sets, reps = exercise.reps,
                    minimumSets = (exercise.setStates.filter { it.isCompleted }.maxOfOrNull { it.position + 1 } ?: 1),
                    restDurationMillis = exercise.restDurationMillis, effectiveRestDurationMillis = duration,
                    customRestSeconds = (duration / 1_000).takeIf { it > 0 } ?: 180,
                ))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = uiState.value.copy(hasEditLoadError = true)
            } finally {
                operationInProgress = false
            }
        }
    }

    fun dismissExerciseEdit() {
        val edit = uiState.value.exerciseEdit ?: return
        if (operationInProgress || edit.isSaving) return
        _uiState.value = uiState.value.copy(exerciseEdit = if (edit.showCustomRest) edit.copy(showCustomRest = false) else null)
    }

    private fun updateEdit(transform: (ExerciseEditUiState) -> ExerciseEditUiState) {
        val edit = uiState.value.exerciseEdit ?: return
        if (operationInProgress || edit.isSaving) return
        _uiState.value = uiState.value.copy(exerciseEdit = transform(edit).copy(hasSaveError = false))
    }

    fun editName(name: String) = updateEdit { it.copy(name = name.take(80)) }
    fun adjustWeight(direction: Int) = updateEdit {
        val step = if (it.unit == WeightUnit.KG) 2.5 else 5.0
        // Keep full kg precision until an explicit adjustment; opening/saving in lb never rounds storage.
        val value = it.unit.fromKilograms(it.weightKg) + direction.coerceIn(-1, 1) * step
        it.copy(weightKg = it.unit.toKilograms(value.coerceAtLeast(0.0)).coerceAtMost(1_000_000.0))
    }
    fun adjustSets(direction: Int) = updateEdit { it.copy(sets = (it.sets + direction.coerceIn(-1, 1)).coerceIn(it.minimumSets, 100)) }
    fun adjustReps(direction: Int) = updateEdit { it.copy(reps = (it.reps + direction.coerceIn(-1, 1)).coerceIn(1, 100)) }
    fun selectRest(durationMillis: Long) {
        if (durationMillis != 0L && durationMillis != 180_000L) return
        updateEdit { it.copy(restDurationMillis = durationMillis, effectiveRestDurationMillis = durationMillis) }
    }
    fun openCustomRest() = updateEdit { it.copy(showCustomRest = true,
        customRestSeconds = (it.effectiveRestDurationMillis / 1_000).takeIf { seconds -> seconds > 0 } ?: 180) }
    fun editCustomRest(seconds: Long) {
        if (seconds !in 0..Long.MAX_VALUE / 1_000) return
        updateEdit { if (it.showCustomRest) it.copy(customRestSeconds = seconds) else it }
    }
    fun applyCustomRest() = updateEdit {
        if (!it.showCustomRest || it.customRestSeconds <= 0) it else it.copy(showCustomRest = false,
            restDurationMillis = it.customRestSeconds * 1_000, effectiveRestDurationMillis = it.customRestSeconds * 1_000)
    }

    fun saveExerciseEdit() {
        val state = uiState.value
        val edit = state.exerciseEdit ?: return
        val session = state.unfinishedWorkout ?: return
        if (operationInProgress || !edit.canSave) return
        operationInProgress = true
        _uiState.value = state.copy(exerciseEdit = edit.copy(isSaving = true, hasSaveError = false))
        viewModelScope.launch {
            try {
                val saved = repository.editExercise(session.id, edit.position, edit.toEdit())
                if (saved == null || saved.id != session.id) {
                    _uiState.value = uiState.value.copy(exerciseEdit = edit.copy(hasSaveError = true))
                } else {
                    _uiState.value = uiState.value.copy(unfinishedWorkout = saved, exerciseEdit = null,
                        selectedExercisePosition = saved.nextExercisePosition())
                    updateCountdown()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = uiState.value.copy(exerciseEdit = edit.copy(hasSaveError = true))
            } finally {
                operationInProgress = false
                _uiState.value = uiState.value.copy(exerciseEdit = uiState.value.exerciseEdit?.copy(isSaving = false))
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
