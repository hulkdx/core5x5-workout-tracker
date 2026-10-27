package com.hulkdx.core5x5.feature.workout.domain

import com.hulkdx.core5x5.core.common.Logger
import com.hulkdx.core5x5.core.common.runCatchingSuspend
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class RestAlertObserver(
    private val workouts: WorkoutRepository,
    private val preferences: TrainingPreferencesRepository,
    private val permissions: RestAlertPermissions,
    private val coordinator: RestAlertCoordinator,
    private val applicationScope: CoroutineScope,
) {
    // Keep the latest resume if it arrives before the application-scope collector starts.
    private val refreshSignals = MutableSharedFlow<Unit>(replay = 1)
    private var observationJob: Job? = null

    /** Called by the app's main-thread lifecycle handler after refreshing permissions. */
    suspend fun onResume() {
        if (observationJob?.isActive != true) {
            observationJob = applicationScope.launch { observe() }
        }
        // Equal permission snapshots do not emit, but resume must still retry and clean up expiry.
        refreshSignals.emit(Unit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun observe() {
        refreshSignals.onStart { emit(Unit) }
            .flatMapLatest {
                // Resume signals also restart sources that ended with a read error.
                merge(
                    workouts.observeWorkoutChanges().asChanges(),
                    preferences.preferences.asChanges(),
                    permissions.status.asChanges(),
                ).onStart { emit(Unit) }
            }
            .conflate()
            .collect { updateFromStorage() }
    }

    private fun Flow<*>.asChanges(): Flow<Unit> = map { Unit }.catch { error ->
        if (error is CancellationException || error !is Exception) throw error
        Logger.warn("RestAlerts", "Could not observe rest alert changes", error)
        // Keep other sources running. Resume signals resubscribe this source.
    }

    private suspend fun updateFromStorage() {
        runCatchingSuspend {
            // Read when processing a signal, rather than retaining an older emitted snapshot.
            val workout = workouts.getUnfinishedWorkout()
            val savedPreferences = preferences.getPreferences()
            val permission = permissions.status.value
            coordinator.updateRestAlert(workout, savedPreferences, permission)
        }.onFailure { error ->
            Logger.warn("RestAlerts", "Could not update rest alerts", error)
        }
    }
}
