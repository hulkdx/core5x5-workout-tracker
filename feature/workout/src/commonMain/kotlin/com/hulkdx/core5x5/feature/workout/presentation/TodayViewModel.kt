package com.hulkdx.core5x5.feature.workout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.feature.workout.domain.WorkoutRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class TodayViewModel(
    private val repository: WorkoutRepository,
    private val preferences: TrainingPreferencesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()
    private var operationInProgress = false
    private var selectedWorkout: Workout? = null
    private var isVisible = true
    private var refreshPending = false
    private var workoutChangesJob: Job? = null

    private var preferencesJob: Job? = null

    init {
        retryPreferences()
        observeWorkoutChanges()
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

    fun loadWorkout() {
        if (operationInProgress || uiState.value.isNavigating) return
        if (workoutChangesJob?.isActive != true) {
            observeWorkoutChanges()
        } else {
            refreshPending = true
            refreshIfPossible()
        }
    }

    private fun observeWorkoutChanges() {
        workoutChangesJob = viewModelScope.launch {
            try {
                repository.observeWorkoutChanges().collect {
                    refreshPending = true
                    refreshIfPossible()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = uiState.value.copy(isLoading = false, error = TodayError.LOAD)
            }
        }
    }

    private fun refreshIfPossible() {
        if (!refreshPending || operationInProgress || (isVisible && uiState.value.isNavigating)) return
        refreshPending = false
        runOperation(TodayError.LOAD, loading = true) {
            showWorkout(repository.getUnfinishedWorkout())
        }
    }

    /** STARTED includes the incoming animation, so refreshed data is ready before RESUMED. */
    fun onVisible() {
        isVisible = true
        val wasNavigating = uiState.value.isNavigating
        _uiState.value = uiState.value.copy(isNavigating = false, requestedWorkout = null)
        if (wasNavigating) refreshPending = true
        refreshIfPossible()
    }

    /** The outgoing content is no longer drawn; it can now reconcile saved changes. */
    fun onHidden() {
        isVisible = false
        _uiState.value = uiState.value.copy(isNavigating = false, requestedWorkout = null)
        refreshPending = true
        refreshIfPossible()
    }

    fun switchWorkout() {
        val state = uiState.value
        if (!isVisible || !state.canStart || state.unfinishedWorkout != null) return
        val selected = state.nextWorkout?.nextWorkout() ?: return
        runOperation(TodayError.SWITCH) {
            val prescription = repository.getNextWorkoutPrescription(selected)
            selectedWorkout = prescription.workout
            _uiState.value = uiState.value.copy(
                nextWorkout = prescription.workout,
                nextWorkoutPrescription = prescription,
                error = null,
            )
        }
    }

    fun startWorkout() {
        if (!isVisible || !uiState.value.canStart) return
        val displayed = uiState.value.nextWorkout ?: return
        runOperation(TodayError.START) {
            val workout = repository.startWorkout(displayed)
            selectedWorkout = null
            // Keep the outgoing prescription/card intact. The hidden refresh publishes Resume.
            _uiState.value = uiState.value.copy(
                requestedWorkout = workout.takeIf { isVisible },
                isNavigating = isVisible,
                error = null,
            )
            refreshPending = true
        }
    }

    fun requestResume() {
        val state = uiState.value
        if (!isVisible || !state.canResume) return
        // Active Workout validates this identity and reloads durable progress after navigation.
        _uiState.value = state.copy(requestedWorkout = state.unfinishedWorkout, isNavigating = true, error = null)
    }

    fun resumePreviousWorkout() {
        val state = uiState.value
        if (!isVisible || !state.canResumePrevious) return
        val previous = state.previousWorkout ?: return
        runOperation(TodayError.REOPEN) {
            val workout = try {
                repository.reopenWorkout(previous.id, previous.completedAtEpochMillis)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                // Recover a committed reopen whose acknowledgement was lost, by this ID only.
                repository.getUnfinishedWorkout()?.takeIf { it.id == previous.id } ?: throw failure
            }
            if (workout == null) {
                // Eligibility may have expired or another session may have started since loading.
                showWorkout(repository.getUnfinishedWorkout())
            } else {
                selectedWorkout = null
                _uiState.value = uiState.value.copy(
                    requestedWorkout = workout.takeIf { isVisible },
                    isNavigating = isVisible,
                    error = null,
                )
                refreshPending = true
            }
        }
    }

    fun onWorkoutRequestHandled(accepted: Boolean = true) {
        _uiState.value = uiState.value.copy(
            requestedWorkout = null,
            isNavigating = accepted && isVisible,
        )
        refreshIfPossible()
    }

    private suspend fun showWorkout(workout: UnfinishedWorkout?) {
        if (workout != null || uiState.value.unfinishedWorkout != null) selectedWorkout = null
        val nextWorkout = if (workout == null) repository.getNextWorkoutPrescription(selectedWorkout) else null
        val previousWorkout = if (workout == null) repository.getResumablePreviousWorkout() else null
        _uiState.value = uiState.value.copy(
            nextWorkout = nextWorkout?.workout,
            nextWorkoutPrescription = nextWorkout,
            unfinishedWorkout = workout,
            previousWorkout = previousWorkout,
            error = null,
        )
    }

    private fun runOperation(
        error: TodayError,
        loading: Boolean = false,
        block: suspend () -> Unit,
    ) {
        if (operationInProgress || (isVisible && uiState.value.isNavigating)) return
        operationInProgress = true
        _uiState.value = uiState.value.copy(
            isLoading = loading,
            isWorking = !loading,
            isReopening = error == TodayError.REOPEN,
            error = null,
            requestedWorkout = null,
            // Never reveal an invalidated hidden snapshot if its replacement read is slow.
            nextWorkout = uiState.value.nextWorkout.takeUnless { loading && !isVisible },
            nextWorkoutPrescription = uiState.value.nextWorkoutPrescription.takeUnless { loading && !isVisible },
            unfinishedWorkout = uiState.value.unfinishedWorkout.takeUnless { loading && !isVisible },
            previousWorkout = uiState.value.previousWorkout.takeUnless { loading && !isVisible },
        )
        viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = uiState.value.copy(error = error)
            } finally {
                operationInProgress = false
                _uiState.value = uiState.value.copy(isLoading = false, isWorking = false, isReopening = false)
                if (viewModelScope.isActive) refreshIfPossible()
            }
        }
    }
}
