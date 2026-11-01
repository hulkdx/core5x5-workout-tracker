package com.hulkdx.core5x5.feature.workout.domain

import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertId
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissionStatus
import com.hulkdx.core5x5.core.restalerts.domain.RestNotificationPermission
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame

class RestAlertCoordinatorTest {
    private val scheduler = RecordingRestAlertScheduler()
    private var now = 1_000L
    private val coordinator = RestAlertCoordinator(scheduler) { now }
    private val workout = restAlertWorkout()
    private val permission = RestAlertPermissionStatus(RestNotificationPermission.GRANTED, true)

    @Test
    fun futureCommittedRestUsesItsIdentityAndIndependentSoundChoice() = runTest {
        coordinator.updateRestAlert(workout, TrainingPreferences(restSoundEnabled = false), permission)
        val request = scheduler.requests.single()
        assertEquals(RestAlertId(7, 2_000), request.id)
        assertEquals(true, request.backgroundEnabled)
        assertFalse(request.soundEnabled)
        assertEquals(0, scheduler.cancellations)
    }

    @Test
    fun deniedUnknownOrMasterOffAlwaysCancelEvenWithSoundOn() = runTest {
        for (status in listOf(RestNotificationPermission.UNKNOWN, RestNotificationPermission.DENIED)) {
            coordinator.updateRestAlert(workout, TrainingPreferences(), permission.copy(notificationPermission = status))
        }
        coordinator.updateRestAlert(workout, TrainingPreferences(restNotificationsEnabled = false), permission)
        assertEquals(3, scheduler.cancellations)
        assertEquals(emptyList(), scheduler.requests)
    }

    @Test
    fun absentRestFinishAndProgramSwitchCancelTheOldAlert() = runTest {
        for (saved in listOf(null, workout.copy(restTimer = null), restAlertWorkout(id = 8, deadline = null))) {
            coordinator.updateRestAlert(saved, TrainingPreferences(), permission)
        }
        assertEquals(3, scheduler.cancellations)
        assertEquals(emptyList(), scheduler.requests)
    }

    @Test
    fun everyExpiredUpdateCancelsWithoutChangingTheSavedDeadline() = runTest {
        for (time in listOf(2_000L, 2_001L, Long.MAX_VALUE)) {
            now = time
            coordinator.updateRestAlert(workout, TrainingPreferences(), permission)
        }
        assertEquals(3, scheduler.cancellations)
        assertEquals(2_000L, workout.restTimer?.deadlineEpochMillis)
        assertEquals(emptyList(), scheduler.requests)
    }

    @Test
    fun repeatedFutureObservationsReachTheBackendIncludingTimingAccessChanges() = runTest {
        coordinator.updateRestAlert(workout, TrainingPreferences(), permission)
        coordinator.updateRestAlert(workout, TrainingPreferences(), permission.copy(preciseTimingAllowed = false))
        assertEquals(2, scheduler.requests.size)
        assertEquals(scheduler.requests[0], scheduler.requests[1])
    }

    @Test
    fun replacementUsesTheNewSessionOrDeadlineWithoutChangingSavedInputs() = runTest {
        val replacement = restAlertWorkout(id = 8, deadline = 3_000)
        coordinator.updateRestAlert(workout, TrainingPreferences(), permission)
        coordinator.updateRestAlert(replacement, TrainingPreferences(), permission)
        assertEquals(listOf(RestAlertId(7, 2_000), RestAlertId(8, 3_000)), scheduler.requests.map { it.id })
    }

    @Test
    fun expiryRequiresReachedTimeMatchingSavedIdentityPermissionAndMasterSwitch() {
        val id = RestAlertId(7, 2_000)
        fun validate(
            candidate: RestAlertId = id,
            saved: UnfinishedWorkout? = workout,
            preferences: TrainingPreferences = TrainingPreferences(),
            status: RestNotificationPermission = RestNotificationPermission.GRANTED,
        ) = coordinator.requestForExpiry(candidate, saved, preferences, status)

        assertNull(validate()) // Callback before deadline.
        now = 2_000
        assertEquals(id, validate()?.id)
        assertFalse(requireNotNull(validate(preferences = TrainingPreferences(restSoundEnabled = false))).soundEnabled)
        assertNull(validate(candidate = id.copy(sessionId = 8)))
        assertNull(validate(candidate = id.copy(deadlineEpochMillis = 1_999)))
        assertNull(validate(saved = null))
        assertNull(validate(saved = workout.copy(restTimer = null)))
        assertNull(validate(preferences = TrainingPreferences(restNotificationsEnabled = false)))
        assertNull(validate(status = RestNotificationPermission.DENIED))
        assertNull(validate(status = RestNotificationPermission.UNKNOWN))
        assertEquals(emptyList(), scheduler.requests)
        assertEquals(0, scheduler.cancellations)
    }

    @Test
    fun backendFailuresAndCancellationPropagateWithoutMutatingTheWorkout() = runTest {
        val failure = IllegalStateException("schedule failed")
        scheduler.failure = failure
        assertSame(failure, assertFailsWith<IllegalStateException> {
            coordinator.updateRestAlert(workout, TrainingPreferences(), permission)
        })
        scheduler.failure = CancellationException("owner closed")
        assertFailsWith<CancellationException> {
            coordinator.updateRestAlert(workout, TrainingPreferences(), permission)
        }
        assertEquals(restAlertWorkout(), workout)
        scheduler.failure = null
        coordinator.updateRestAlert(workout, TrainingPreferences(), permission)
        assertEquals(RestAlertId(7, 2_000), scheduler.requests.single().id)
    }
}
