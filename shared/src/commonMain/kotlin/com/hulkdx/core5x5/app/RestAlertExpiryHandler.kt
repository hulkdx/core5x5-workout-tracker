package com.hulkdx.core5x5.app

import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import com.hulkdx.core5x5.core.restalerts.data.PersistedRestAlertScheduler
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertId
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissions
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertRequest
import com.hulkdx.core5x5.feature.workout.domain.RestAlertCoordinator
import com.hulkdx.core5x5.feature.workout.domain.WorkoutRepository

class RestAlertExpiryHandler(
    private val scheduler: PersistedRestAlertScheduler,
    private val coordinator: RestAlertCoordinator,
    private val workouts: WorkoutRepository,
    private val preferences: TrainingPreferencesRepository,
    private val permissions: RestAlertPermissions,
) {
    suspend fun handle(id: RestAlertId, present: suspend (RestAlertRequest) -> Unit) {
        scheduler.handleExpiry(
            id = id,
            validate = {
                permissions.refreshStatus()
                coordinator.requestForExpiry(
                    id = id,
                    workout = workouts.getUnfinishedWorkout(),
                    preferences = preferences.getPreferences(),
                    permission = permissions.status.value.notificationPermission,
                )
            },
            present = present,
        )
    }
}
