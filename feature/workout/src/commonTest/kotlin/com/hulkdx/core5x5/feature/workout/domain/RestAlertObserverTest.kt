package com.hulkdx.core5x5.feature.workout.domain

import com.hulkdx.core5x5.core.restalerts.domain.RestNotificationPermission
import com.hulkdx.core5x5.feature.workout.presentation.FakeTrainingPreferencesRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RestAlertObserverTest {
    private val workouts = RestAlertTestWorkouts()
    private val preferences = FakeTrainingPreferencesRepository()
    private val permissions = RestAlertTestPermissions()
    private val scheduler = RecordingRestAlertScheduler()
    private var now = 1_000L
    private val coordinator = RestAlertCoordinator(scheduler) { now }

    @Test
    fun observationStartsOnlyOnResumeAndRepeatedResumesKeepOneSubscription() = runTest {
        val observer = RestAlertObserver(workouts, preferences, permissions, coordinator, backgroundScope)
        runCurrent()
        assertEquals(0, workouts.observations)
        repeat(3) { launch { observer.onResume() } }
        runCurrent()
        assertEquals(1, workouts.activeObservers)
        assertEquals(1, preferences.values.subscriptionCount.value)
        assertEquals(1, permissions.status.subscriptionCount.value)
        assertTrue(scheduler.requests.isNotEmpty())
        val count = workouts.reads
        observer.onResume()
        runCurrent()
        assertTrue(workouts.reads > count) // Equal permission values must still force a fresh read.
        assertEquals(1, workouts.activeObservers)
        assertEquals(0, permissions.refreshes) // Lifecycle, not observation, queries the OS.
    }

    @Test
    fun savedWorkoutPreferencesAndPermissionChangesEachUpdateTheCurrentAlert() = runTest {
        val observer = RestAlertObserver(workouts, preferences, permissions, coordinator, backgroundScope)
        observer.onResume()
        runCurrent()
        workouts.saved = restAlertWorkout(id = 8, deadline = 3_000)
        workouts.changes.emit(Unit)
        runCurrent()
        assertEquals(8L, scheduler.requests.last().id.sessionId)
        preferences.setRestSoundEnabled(false)
        runCurrent()
        assertFalse(scheduler.requests.last().soundEnabled)
        preferences.setRestNotificationsEnabled(false)
        runCurrent()
        val cancelled = scheduler.cancellations
        assertTrue(cancelled > 0)
        assertFalse(preferences.value.restNotificationsEnabled)
        assertFalse(preferences.value.restSoundEnabled)
        preferences.setRestNotificationsEnabled(true)
        runCurrent()
        assertFalse(scheduler.requests.last().soundEnabled)
        permissions.status.value = permissions.status.value.copy(notificationPermission = RestNotificationPermission.DENIED)
        runCurrent()
        assertTrue(scheduler.cancellations > cancelled)
    }

    @Test
    fun resumeCleansExpiredRestEvenWhenPermissionsAreUnchanged() = runTest {
        val observer = RestAlertObserver(workouts, preferences, permissions, coordinator, backgroundScope)
        observer.onResume()
        runCurrent()
        scheduler.requests.clear()
        now = 2_000
        repeat(2) { observer.onResume(); runCurrent() }
        assertTrue(scheduler.requests.isEmpty())
        assertTrue(scheduler.cancellations >= 2)
        assertEquals(2_000L, workouts.saved?.restTimer?.deadlineEpochMillis)
    }

    @Test
    fun coalescedSignalsReadTheLatestSavedDataRatherThanAnOlderEmittedSnapshot() = runTest {
        workouts.readGate = CompletableDeferred()
        val observer = RestAlertObserver(workouts, preferences, permissions, coordinator, backgroundScope)
        observer.onResume()
        runCurrent()
        workouts.saved = restAlertWorkout(id = 8, deadline = 4_000)
        preferences.setRestSoundEnabled(false)
        workouts.changes.emit(Unit)
        workouts.readGate?.complete(Unit)
        runCurrent()
        assertTrue(scheduler.requests.isNotEmpty())
        assertTrue(scheduler.requests.all { it.id.sessionId == 8L && !it.soundEnabled })
    }

    @Test
    fun failedSourcesDoNotStopHealthySourcesAndResumeResubscribesThem() = runTest {
        workouts.observationFailure = IllegalStateException("workout source failed")
        preferences.loadError = IllegalStateException("preference source failed")
        val observer = RestAlertObserver(workouts, preferences, permissions, coordinator, backgroundScope)
        observer.onResume()
        runCurrent()
        assertEquals(0, workouts.activeObservers)
        permissions.status.value = permissions.status.value.copy(notificationPermission = RestNotificationPermission.DENIED)
        runCurrent()
        assertTrue(scheduler.cancellations > 0)
        workouts.observationFailure = null
        preferences.loadError = null
        observer.onResume()
        runCurrent()
        assertEquals(1, workouts.activeObservers)
        assertTrue(preferences.observations >= 2)
        preferences.setRestNotificationsEnabled(false)
        runCurrent()
        assertEquals(1, preferences.values.subscriptionCount.value)
    }

    @Test
    fun savedReadAndSchedulingFailuresAreIsolatedAndTheNextSignalRetries() = runTest {
        workouts.readFailure = IllegalStateException("storage read failed")
        val observer = RestAlertObserver(workouts, preferences, permissions, coordinator, backgroundScope)
        observer.onResume()
        runCurrent()
        assertTrue(scheduler.requests.isEmpty())
        workouts.readFailure = null
        scheduler.failure = IllegalStateException("native schedule failed")
        preferences.setRestSoundEnabled(false)
        runCurrent()
        assertTrue(scheduler.requests.isEmpty())
        scheduler.failure = null
        observer.onResume()
        runCurrent()
        assertFalse(scheduler.requests.last().soundEnabled)
        assertEquals(restAlertWorkout(), workouts.saved)
    }

    @Test
    fun preferenceReadFailuresDoNotScheduleWithDefaultsAndTheNextChangeRetriesSavedValues() = runTest {
        preferences.readError = IllegalStateException("preference read failed")
        val observer = RestAlertObserver(workouts, preferences, permissions, coordinator, backgroundScope)
        observer.onResume()
        runCurrent()
        assertTrue(scheduler.requests.isEmpty())
        assertEquals(0, scheduler.cancellations)
        preferences.readError = null
        preferences.setRestSoundEnabled(false)
        runCurrent()
        assertFalse(scheduler.requests.last().soundEnabled)
    }

    @Test
    fun closingTheApplicationScopeStopsSourcesAndPendingUpdates() = runTest {
        val observer = RestAlertObserver(workouts, preferences, permissions, coordinator, backgroundScope)
        observer.onResume()
        runCurrent()
        assertEquals(1, workouts.activeObservers)
        val count = workouts.reads
        backgroundScope.cancel()
        runCurrent()
        assertEquals(0, workouts.activeObservers)
        assertEquals(0, preferences.values.subscriptionCount.value)
        assertEquals(0, permissions.status.subscriptionCount.value)
        preferences.setRestNotificationsEnabled(false)
        runCurrent()
        assertEquals(count, workouts.reads)
    }
}
