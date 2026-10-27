package com.hulkdx.core5x5.feature.workout.domain

import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertId
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissionStatus
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertRequest
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertScheduler
import com.hulkdx.core5x5.core.restalerts.domain.RestNotificationPermission
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Makes alert decisions from committed snapshots; never writes workout or preference data. */
class RestAlertCoordinator(
    private val scheduler: RestAlertScheduler,
    private val nowEpochMillis: () -> Long,
) {
    private val mutex = Mutex()

    /** The app-owned observer supplies current saved inputs after changes and on resume. */
    suspend fun updateRestAlert(
        workout: UnfinishedWorkout?,
        preferences: TrainingPreferences,
        permission: RestAlertPermissionStatus,
    ) = mutex.withLock {
        val request = createRequest(workout, preferences, permission.notificationPermission)
        val isExpired = request != null && request.id.deadlineEpochMillis <= nowEpochMillis()

        if (request == null || isExpired) {
            scheduler.cancel()
        } else {
            scheduler.schedule(request)
        }
    }

    /**
     * Native expiry callbacks supply freshly read saved inputs before presenting an alert.
     * The backend must also reject requests its private bookkeeping has canceled or delivered.
     * The backend presents the approved expiry using the returned sound policy.
     */
    fun requestForExpiry(
        id: RestAlertId,
        workout: UnfinishedWorkout?,
        preferences: TrainingPreferences,
        permission: RestNotificationPermission,
    ): RestAlertRequest? {
        val request = createRequest(workout, preferences, permission) ?: return null
        if (request.id != id || id.deadlineEpochMillis > nowEpochMillis()) return null
        return request
    }

    private fun createRequest(
        workout: UnfinishedWorkout?,
        preferences: TrainingPreferences,
        permission: RestNotificationPermission,
    ): RestAlertRequest? {
        if (!preferences.restNotificationsEnabled) return null
        if (permission != RestNotificationPermission.GRANTED) return null
        val timer = workout?.restTimer ?: return null
        return RestAlertRequest(
            id = RestAlertId(workout.id, timer.deadlineEpochMillis),
            backgroundEnabled = preferences.restNotificationsEnabled,
            soundEnabled = preferences.restSoundEnabled,
        )
    }
}
