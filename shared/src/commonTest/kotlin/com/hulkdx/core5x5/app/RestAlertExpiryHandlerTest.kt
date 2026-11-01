package com.hulkdx.core5x5.app

import com.hulkdx.core5x5.core.preferences.data.PersistedTrainingPreferencesRepository
import com.hulkdx.core5x5.core.preferences.data.PreferencesStorage
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import com.hulkdx.core5x5.core.restalerts.data.PersistedRestAlertScheduler
import com.hulkdx.core5x5.core.restalerts.data.RestAlertPlatform
import com.hulkdx.core5x5.core.restalerts.data.RestAlertState
import com.hulkdx.core5x5.core.restalerts.data.RestAlertStateStore
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertId
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissionStatus
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissions
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertRequest
import com.hulkdx.core5x5.core.restalerts.domain.RestNotificationPermission
import com.hulkdx.core5x5.feature.workout.domain.CompletedWorkout
import com.hulkdx.core5x5.feature.workout.domain.ExerciseEdit
import com.hulkdx.core5x5.feature.workout.domain.RestAlertCoordinator
import com.hulkdx.core5x5.feature.workout.domain.RestAlertObserver
import com.hulkdx.core5x5.feature.workout.domain.RestTimer
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.feature.workout.domain.WorkoutPrescription
import com.hulkdx.core5x5.feature.workout.domain.WorkoutRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RestAlertExpiryHandlerTest {
    @Test
    fun expiryReadsCurrentSavedInputsAndClaimsBeforePresentingTheCurrentSoundPolicy() = runTest {
        val fixture = ExpiryFixture()
        fixture.preferences.setRestSoundEnabled(false)
        var presented: RestAlertRequest? = null
        fixture.handler.handle(fixture.id) {
            assertEquals(RestAlertState.Status.HANDLED, fixture.state?.status)
            presented = it
        }
        assertEquals(RestAlertRequest(fixture.id, true, false), presented)
        assertEquals(listOf("permissions", "workout", "preferences"), fixture.reads)
        assertEquals(fixture.workout, fixture.workouts.getUnfinishedWorkout())
        fixture.handler.handle(fixture.id) { error("duplicate delivery") }
        assertEquals(1, fixture.refreshes)
    }

    @Test
    fun freshlyDeniedPermissionMasterOffAndChangedSavedIdentitiesSuppressDelivery() = runTest {
        val fixtures = listOf(
            ExpiryFixture().apply { nextPermission = RestNotificationPermission.DENIED },
            ExpiryFixture().apply { preferences.setRestNotificationsEnabled(false) },
            ExpiryFixture().apply { workouts.saved = workout.copy(id = 8) },
            ExpiryFixture().apply { workouts.saved = workout.copy(restTimer = RestTimer(3_000)) },
            ExpiryFixture().apply { workouts.saved = null },
        )
        for (fixture in fixtures) {
            fixture.handler.handle(fixture.id) { error("ineligible delivery") }
            assertEquals(RestAlertState.Status.CANCELLED, fixture.state?.status)
            assertEquals(1, fixture.notificationCancellations)
        }
    }

    @Test
    fun earlyCallbacksAreRejectedByTheCoordinatorWithoutPresenting() = runTest {
        val fixture = ExpiryFixture().apply { now = 1_999 }
        fixture.handler.handle(fixture.id) { error("early delivery") }
        assertEquals(RestAlertState.Status.CANCELLED, fixture.state?.status)
    }

    @Test
    fun staleCallbackDoesNotRefreshPermissionsOrReadSavedInputs() = runTest {
        val fixture = ExpiryFixture()
        fixture.handler.handle(fixture.id.copy(sessionId = 8)) { error("stale delivery") }
        assertTrue(fixture.reads.isEmpty())
        assertEquals(RestAlertState.Status.ARMED, fixture.state?.status)
    }

    @Test
    fun permissionAndSavedInputFailuresPropagateWithoutClaimingOrPresenting() = runTest {
        for (source in listOf("permissions", "workout", "preferences")) {
            val fixture = ExpiryFixture().apply { failingRead = source }
            assertFailsWith<IllegalStateException> {
                fixture.handler.handle(fixture.id) { error("unvalidated delivery") }
            }
            assertEquals(RestAlertState.Status.ARMED, fixture.state?.status)
            fixture.failingRead = null
            var delivered = false
            fixture.handler.handle(fixture.id) { delivered = true }
            assertTrue(delivered)
        }
    }

    @Test
    fun cancellationDuringPermissionRefreshDoesNotBecomeSuccessfulDelivery() = runTest {
        val fixture = ExpiryFixture()
        val cancelled = CancellationException("application closed")
        fixture.refreshFailure = cancelled
        assertSame(cancelled, assertFailsWith<CancellationException> {
            fixture.handler.handle(fixture.id) { error("cancelled delivery") }
        })
        assertEquals(RestAlertState.Status.ARMED, fixture.state?.status)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppLifecycleHandlerTest {
    @Test
    fun refreshesPermissionsBeforeStartingObservationAndForcesEveryResumeToUpdate() = runTest {
        val fixture = ExpiryFixture().apply { now = 1_000 }
        // This scheduler is deliberately only used after expiry; start without an active rest.
        fixture.workouts.saved = null
        val observer = RestAlertObserver(
            fixture.workouts, fixture.preferences, fixture.permissions,
            RestAlertCoordinator(fixture.scheduler) { fixture.now }, backgroundScope,
        )
        val lifecycle = AppLifecycleHandler(fixture.permissions, observer)
        lifecycle.onResume()
        assertEquals(listOf("permissions"), fixture.reads)
        runCurrent()
        assertTrue(fixture.reads.indexOf("workout") > fixture.reads.indexOf("permissions"))
        val reads = fixture.reads.count { it == "workout" }
        lifecycle.onResume()
        runCurrent()
        assertEquals(2, fixture.refreshes)
        assertTrue(fixture.reads.count { it == "workout" } > reads)
    }

    @Test
    fun failedPermissionRefreshStillAllowsCleanupUsingTheLastCheckedSnapshot() = runTest {
        val fixture = ExpiryFixture().apply {
            refreshFailure = IllegalStateException("OS query failed")
            nextPermission = RestNotificationPermission.DENIED
        }
        val observer = RestAlertObserver(
            fixture.workouts, fixture.preferences, fixture.permissions,
            RestAlertCoordinator(fixture.scheduler) { fixture.now }, backgroundScope,
        )
        AppLifecycleHandler(fixture.permissions, observer).onResume()
        runCurrent()
        assertTrue("workout" in fixture.reads)
        assertTrue(fixture.notificationCancellations > 0)
    }

    @Test
    fun cancelledPermissionRefreshDoesNotStartObservation() = runTest {
        val fixture = ExpiryFixture().apply { refreshFailure = CancellationException("owner closed") }
        val observer = RestAlertObserver(
            fixture.workouts, fixture.preferences, fixture.permissions,
            RestAlertCoordinator(fixture.scheduler) { fixture.now }, backgroundScope,
        )
        assertFailsWith<CancellationException> { AppLifecycleHandler(fixture.permissions, observer).onResume() }
        runCurrent()
        assertEquals(listOf("permissions"), fixture.reads)
    }
}

private class ExpiryFixture {
    val id = RestAlertId(7, 2_000)
    val workout = UnfinishedWorkout(Workout.A, 100, emptyList(), id.sessionId, RestTimer(id.deadlineEpochMillis))
    val reads = mutableListOf<String>()
    var now = 2_000L
    var state: RestAlertState? = RestAlertState(id, RestAlertState.Status.ARMED)
    var nextPermission = RestNotificationPermission.GRANTED
    var refreshFailure: Throwable? = null
    var failingRead: String? = null
    var refreshes = 0
    var notificationCancellations = 0
    val workouts = ExpiryWorkouts(workout) {
        reads += "workout"
        check(failingRead != "workout")
    }
    private var encodedPreferences: String? = null
    private val persistedPreferences = PersistedTrainingPreferencesRepository(object : PreferencesStorage {
        override suspend fun read(): String? = encodedPreferences
        override suspend fun write(value: String) { encodedPreferences = value }
    })
    val preferences = object : TrainingPreferencesRepository by persistedPreferences {
        override suspend fun getPreferences(): TrainingPreferences {
            reads += "preferences"
            check(failingRead != "preferences")
            return persistedPreferences.getPreferences()
        }
    }
    val permissions = object : RestAlertPermissions {
        override val status = MutableStateFlow(RestAlertPermissionStatus())
        override suspend fun refreshStatus() {
            refreshes++
            reads += "permissions"
            refreshFailure?.let { throw it }
            check(failingRead != "permissions")
            status.value = RestAlertPermissionStatus(nextPermission)
        }
    }
    val scheduler = PersistedRestAlertScheduler(
        object : RestAlertStateStore {
            override suspend fun read() = state
            override suspend fun write(state: RestAlertState) { this@ExpiryFixture.state = state }
        },
        object : RestAlertPlatform {
            override suspend fun schedule(request: RestAlertRequest, wasArmed: Boolean) = error("Expiry cannot schedule")
            override suspend fun cancel(id: RestAlertId) { assertEquals(this@ExpiryFixture.id, id) }
            override suspend fun cancelNotification() { notificationCancellations++ }
        },
    ) { now }
    val handler = RestAlertExpiryHandler(scheduler, RestAlertCoordinator(scheduler) { now }, workouts, preferences, permissions)
}

private class ExpiryWorkouts(var saved: UnfinishedWorkout?, private val onRead: () -> Unit) : WorkoutRepository {
    override fun observeWorkoutChanges() = flowOf(Unit)
    override suspend fun getUnfinishedWorkout(): UnfinishedWorkout? { onRead(); return saved }
    override suspend fun getCompletedWorkout(workoutId: Long): CompletedWorkout? = error("Unexpected completed read")
    override suspend fun getResumablePreviousWorkout(): CompletedWorkout? = error("Unexpected previous read")
    override suspend fun reopenWorkout(workoutId: Long, completedAtEpochMillis: Long): UnfinishedWorkout? = error("Unexpected write")
    override suspend fun getNextWorkout(): Workout = error("Unexpected program read")
    override suspend fun getNextWorkoutPrescription(workoutOverride: Workout?): WorkoutPrescription = error("Unexpected prescription read")
    override suspend fun startWorkout(workout: Workout): UnfinishedWorkout = error("Unexpected write")
    override suspend fun switchWorkout(workoutId: Long, workout: Workout): UnfinishedWorkout? = error("Unexpected write")
    override suspend fun finalizeWorkout(workoutId: Long): Boolean = error("Unexpected write")
    override suspend fun setSetCompleted(exercisePosition: Int, setPosition: Int, isCompleted: Boolean): Boolean = error("Unexpected write")
    override suspend fun editExercise(workoutId: Long, exercisePosition: Int, edit: ExerciseEdit): UnfinishedWorkout? = error("Unexpected write")
    override suspend fun undoSetCompletion(workoutId: Long, exercisePosition: Int, setPosition: Int): UnfinishedWorkout? = error("Unexpected write")
    override suspend fun setCompletedReps(workoutId: Long, exercisePosition: Int, setPosition: Int, completedReps: Int): UnfinishedWorkout? = error("Unexpected write")
    override suspend fun completeSetAndStartRest(workoutId: Long, exercisePosition: Int, setPosition: Int, restDurationMillis: Long): UnfinishedWorkout? = error("Unexpected write")
}
