@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.hulkdx.core5x5.uitest.screen.workout

import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissionStatus
import com.hulkdx.core5x5.core.restalerts.domain.RestNotificationPermission
import com.hulkdx.core5x5.feature.workout.domain.Exercise.SQUAT
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.uitest.runRestAlertUiTest
import com.hulkdx.core5x5.uitest.screen.settings.launchSettingsScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RestAlertJourneyTest {
    @Test
    fun savedSetSchedulesOneRestAndRepChangesDoNotReplaceItsIdentity() = runRestAlertUiTest { alerts ->
        val workout = launchWorkoutScreen(this) { tapSet(SQUAT, 1) }
        ui.waitUntil(timeoutMillis = 5_000) { alerts.pending.value != null }
        val request = requireNotNull(alerts.pending.value)
        assertEquals(nowEpochMillis + 180_000, request.id.deadlineEpochMillis)
        workout.tapSet(SQUAT, 1)
        resumeRestAlertObservation()
        ui.waitForIdle()
        assertEquals(request, alerts.pending.value)
        assertEquals(request.id.deadlineEpochMillis, unfinishedWorkout()?.restTimer?.deadlineEpochMillis)
        assertTrue(alerts.delivered.value.isEmpty())
    }

    @Test
    fun settingsSwitchesCancelReenableAndSilenceTheRunningRest() = runRestAlertUiTest { alerts ->
        launchWorkoutScreen(this) { tapSet(SQUAT, 1); leaveWorkout() }
        ui.waitUntil(timeoutMillis = 5_000) { alerts.pending.value != null }
        val id = requireNotNull(alerts.pending.value).id
        val settings = launchSettingsScreen(this) {}
        settings.toggleNotifications()
        ui.waitUntil(timeoutMillis = 5_000) { alerts.pending.value == null }
        settings.verify { notificationSwitches(enable = false, sound = true) }
        settings.toggleNotifications()
        ui.waitUntil(timeoutMillis = 5_000) { alerts.pending.value?.id == id }
        settings.toggleSound()
        ui.waitUntil(timeoutMillis = 5_000) { alerts.pending.value?.soundEnabled == false }
        assertEquals(id, alerts.pending.value?.id)
        assertEquals(id.deadlineEpochMillis, unfinishedWorkout()?.restTimer?.deadlineEpochMillis)
    }

    @Test
    fun permissionDenialCancelsWithoutChangingProgressAndGrantRestoresTheFutureRest() = runRestAlertUiTest { alerts ->
        val workout = launchWorkoutScreen(this) { tapSet(SQUAT, 1) }
        ui.waitUntil(timeoutMillis = 5_000) { alerts.pending.value != null }
        val saved = requireNotNull(unfinishedWorkout())
        setRestAlertPermissionStatus(RestAlertPermissionStatus(RestNotificationPermission.DENIED))
        ui.waitUntil(timeoutMillis = 5_000) { alerts.pending.value == null }
        workout.verify { setCompleted(SQUAT, 1, 5); restTimerShown("03:00") }
        assertEquals(saved, unfinishedWorkout())
        setRestAlertPermissionStatus(RestAlertPermissionStatus(RestNotificationPermission.GRANTED))
        ui.waitUntil(timeoutMillis = 5_000) { alerts.pending.value?.id?.deadlineEpochMillis == saved.restTimer?.deadlineEpochMillis }
        assertTrue(alerts.delivered.value.isEmpty())
    }

    @Test
    fun replacementFinishAndProgramSwitchSuppressStaleExpiryCallbacks() = runRestAlertUiTest { alerts ->
        val workout = launchWorkoutScreen(this) { tapSet(SQUAT, 1) }
        ui.waitUntil(timeoutMillis = 5_000) { alerts.pending.value != null }
        val obsolete = requireNotNull(alerts.pending.value).id
        advanceTimeBy(10_000)
        workout.tapSet(SQUAT, 2)
        ui.waitUntil(timeoutMillis = 5_000) {
            alerts.pending.value?.let { it.id != obsolete } == true
        }
        deliverRestAlert(obsolete)
        assertTrue(alerts.delivered.value.isEmpty())
        workout.switchWorkout(Workout.B)
        ui.waitUntil(timeoutMillis = 5_000) { alerts.pending.value == null }
        workout.tapSet(SQUAT, 1)
        ui.waitUntil(timeoutMillis = 5_000) { alerts.pending.value != null }
        val beforeFinish = requireNotNull(alerts.pending.value).id
        workout.pressFinish()
        ui.waitUntil(timeoutMillis = 5_000) { alerts.pending.value == null }
        advanceTimeBy(180_000)
        deliverRestAlert(beforeFinish)
        assertTrue(alerts.delivered.value.isEmpty())
        workout.verify { workoutCompleteScreenShown(completedSets = 1, prescribedSets = 11) }
    }

    @Test
    fun duplicateExpiryWhileSettingsIsVisiblePresentsOnePermittedSilentAlert() = runRestAlertUiTest { alerts ->
        launchWorkoutScreen(this) { tapSet(SQUAT, 1); leaveWorkout() }
        val settings = launchSettingsScreen(this) { toggleSound() }
        ui.waitUntil(timeoutMillis = 5_000) { alerts.pending.value?.soundEnabled == false }
        val id = requireNotNull(alerts.pending.value).id
        advanceTimeBy(180_000)
        repeat(2) { deliverRestAlert(id) }
        assertEquals(1, alerts.delivered.value.size)
        assertFalse(alerts.delivered.value.single().soundEnabled)
        settings.verify { notificationSwitches(enable = true, sound = false) }
    }

    @Test
    fun reopeningExpiredRestShowsZeroWithoutCatchUpDelivery() = runRestAlertUiTest { alerts ->
        val workout = launchWorkoutScreen(this) { tapSet(SQUAT, 1) }
        ui.waitUntil(timeoutMillis = 5_000) { alerts.pending.value != null }
        val id = requireNotNull(alerts.pending.value).id
        restart()
        ui.waitUntil(timeoutMillis = 5_000) { alerts.pending.value?.id == id }
        advanceTimeBy(180_000)
        restart()
        ui.waitUntil(timeoutMillis = 5_000) { alerts.pending.value == null }
        workout.resumeWorkout()
        workout.verify { restTimerShown("00:00", expired = true); setCompleted(SQUAT, 1, 5) }
        deliverRestAlert(id)
        assertTrue(alerts.delivered.value.isEmpty())
    }

    @Test
    fun failedNativeSubmissionDoesNotBlockLoggingAndResumeRetries() = runRestAlertUiTest { alerts ->
        alerts.rejectSubmissions = true
        val attempts = alerts.submissionAttempts.value
        val workout = launchWorkoutScreen(this) { tapSet(SQUAT, 1) }
        ui.waitUntil(timeoutMillis = 5_000) { alerts.submissionAttempts.value > attempts }
        workout.verify { setCompleted(SQUAT, 1, 5); restTimerShown("03:00") }
        val saved = unfinishedWorkout()
        alerts.rejectSubmissions = false
        resumeRestAlertObservation()
        ui.waitUntil(timeoutMillis = 5_000) { alerts.pending.value != null }
        assertEquals(saved, unfinishedWorkout())
        assertTrue(alerts.delivered.value.isEmpty())
    }
}
