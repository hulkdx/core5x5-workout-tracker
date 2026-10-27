package com.hulkdx.core5x5.core.restalerts.data

import com.hulkdx.core5x5.core.restalerts.data.RestAlertState.Status
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertId
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PersistedRestAlertSchedulerTest {
    private val request = RestAlertRequest(RestAlertId(7, 2_000), backgroundEnabled = true, soundEnabled = true)

    @Test
    fun acceptedRestIsDurableBeforeNativeSubmissionAndClearsOrphans() = runTest {
        val fixture = Fixture()
        fixture.platform.beforeSchedule = {
            assertEquals(RestAlertState(request.id, Status.ARMED), fixture.store.saved)
        }
        fixture.scheduler().schedule(request)
        assertEquals(listOf("notifications", "write:ARMED", "schedule:false"), fixture.events)
        assertEquals(request, fixture.platform.pending[request.id])
    }

    @Test
    fun repeatedRequestsAndSoundChangesReplaceOneIdentityUsingCurrentNativeAccess() = runTest {
        val fixture = Fixture()
        val scheduler = fixture.scheduler()
        scheduler.schedule(request)
        fixture.events.clear()
        scheduler.schedule(request)
        val silent = request.copy(soundEnabled = false)
        scheduler.schedule(silent)
        assertEquals(listOf("write:ARMED", "schedule:true", "write:ARMED", "schedule:true"), fixture.events)
        assertEquals(mapOf(request.id to silent), fixture.platform.pending)
    }

    @Test
    fun replacementCancelsThePreviousIdentityAndDeliveredArtifacts() = runTest {
        val fixture = Fixture()
        val scheduler = fixture.scheduler()
        scheduler.schedule(request)
        fixture.events.clear()
        val replacement = request.copy(id = RestAlertId(8, 3_000))
        scheduler.schedule(replacement)
        assertEquals(listOf("cancel", "notifications", "write:ARMED", "schedule:false"), fixture.events)
        assertEquals(mapOf(replacement.id to replacement), fixture.platform.pending)
        assertEquals(replacement.id, fixture.store.saved?.id)
    }

    @Test
    fun cancelledFutureRestCanBeEnabledAgainWithoutInferringDelivery() = runTest {
        val fixture = Fixture()
        val scheduler = fixture.scheduler()
        scheduler.schedule(request)
        scheduler.cancel()
        assertEquals(Status.CANCELLED, fixture.store.saved?.status)
        assertTrue(fixture.platform.pending.isEmpty())
        scheduler.schedule(request)
        assertEquals(Status.ARMED, fixture.store.saved?.status)
        assertEquals(request, fixture.platform.pending[request.id])
        assertEquals("schedule:false", fixture.events.last())
    }

    @Test
    fun handledFutureIdentityNeitherReplaysNorDismissesItsNotification() = runTest {
        val fixture = Fixture(RestAlertState(request.id, Status.HANDLED))
        fixture.scheduler().schedule(request)
        assertTrue(fixture.events.isEmpty())
        fixture.scheduler().cancel()
        assertEquals(Status.HANDLED, fixture.store.saved?.status)
        assertEquals(listOf("cancel", "notifications"), fixture.events)
    }

    @Test
    fun expiryOnEntryAndDisabledPolicyCancelInsteadOfSubmittingCatchUp() = runTest {
        for (candidate in listOf(
            request.copy(id = request.id.copy(deadlineEpochMillis = 1_000)),
            request.copy(id = request.id.copy(deadlineEpochMillis = 999)),
            request.copy(backgroundEnabled = false),
        )) {
            val fixture = Fixture(RestAlertState(candidate.id, Status.ARMED))
            repeat(2) { fixture.scheduler().schedule(candidate) }
            assertEquals(Status.CANCELLED, fixture.store.saved?.status)
            assertTrue(fixture.platform.pending.isEmpty())
            assertEquals(2, fixture.events.count { it == "notifications" })
        }
    }

    @Test
    fun cancellationWithoutSavedStateStillCleansOwnedNotifications() = runTest {
        val fixture = Fixture()
        repeat(2) { fixture.scheduler().cancel() }
        assertNull(fixture.store.saved)
        assertEquals(listOf("notifications", "notifications"), fixture.events)
    }

    @Test
    fun firstAcceptedRestMayBecomeDueDuringPersistenceWithoutBeingDropped() = runTest {
        val fixture = Fixture()
        fixture.store.beforeWrite = { fixture.now = request.id.deadlineEpochMillis }
        fixture.scheduler().schedule(request)
        assertEquals(request, fixture.platform.pending[request.id])
        assertEquals("schedule:false", fixture.events.last())
    }

    @Test
    fun nativeSubmissionFailureRetainsEligibilityForRecoveryAfterRecreation() = runTest {
        val fixture = Fixture()
        val failure = IllegalStateException("native add failed")
        fixture.platform.scheduleFailure = failure
        assertSame(failure, assertFailsWith<IllegalStateException> { fixture.scheduler().schedule(request) })
        assertEquals(Status.ARMED, fixture.store.saved?.status)
        assertTrue(fixture.platform.pending.isEmpty())
        fixture.platform.scheduleFailure = null
        fixture.scheduler().schedule(request)
        assertEquals(request, fixture.platform.pending[request.id])
        assertEquals("schedule:true", fixture.events.last())
    }

    @Test
    fun readAndArmedWriteFailuresPropagateWithoutSubmittingAnUnsavedAlert() = runTest {
        val fixture = Fixture()
        val failure = IllegalStateException("disk unavailable")
        fixture.store.readFailure = failure
        assertSame(failure, assertFailsWith<IllegalStateException> { fixture.scheduler().schedule(request) })
        assertTrue(fixture.events.isEmpty())
        fixture.store.readFailure = null
        fixture.store.writeFailure = failure
        assertSame(failure, assertFailsWith<IllegalStateException> { fixture.scheduler().schedule(request) })
        assertNull(fixture.store.saved)
        assertTrue(fixture.platform.pending.isEmpty())
    }

    @Test
    fun failedCancellationWriteDoesNotPretendNativeCleanupSucceededAndCanRetry() = runTest {
        val fixture = Fixture(RestAlertState(request.id, Status.ARMED))
        fixture.store.writeFailure = IllegalStateException("disk full")
        assertFailsWith<IllegalStateException> { fixture.scheduler().cancel() }
        assertEquals(Status.ARMED, fixture.store.saved?.status)
        assertTrue(fixture.events.isEmpty())
        fixture.store.writeFailure = null
        fixture.scheduler().cancel()
        assertEquals(listOf("write:CANCELLED", "cancel", "notifications"), fixture.events)
    }

    @Test
    fun failedNativeCancellationRetainsCancelledStateAndStopsFurtherCleanupUntilRetry() = runTest {
        val fixture = Fixture(RestAlertState(request.id, Status.ARMED))
        fixture.platform.cancelFailure = IllegalStateException("alarm removal failed")
        assertFailsWith<IllegalStateException> { fixture.scheduler().cancel() }
        assertEquals(Status.CANCELLED, fixture.store.saved?.status)
        assertEquals(listOf("write:CANCELLED", "cancel"), fixture.events)
        fixture.platform.cancelFailure = null
        fixture.scheduler().cancel()
        assertEquals("notifications", fixture.events.last())
    }

    @Test
    fun failedNotificationCleanupPropagatesAndRetryDoesNotRearmACancelledRest() = runTest {
        val fixture = Fixture(RestAlertState(request.id, Status.ARMED))
        val failure = IllegalStateException("notification cleanup failed")
        fixture.platform.notificationFailure = failure
        assertSame(failure, assertFailsWith<IllegalStateException> { fixture.scheduler().cancel() })
        assertEquals(Status.CANCELLED, fixture.store.saved?.status)
        fixture.platform.notificationFailure = null
        fixture.scheduler().cancel()
        assertEquals(Status.CANCELLED, fixture.store.saved?.status)
        assertTrue(fixture.platform.pending.isEmpty())
    }

    @Test
    fun missingStaleCancelledAndHandledCallbacksNeverValidateOrPresent() = runTest {
        for (state in listOf(
            null, RestAlertState(request.id.copy(sessionId = 8), Status.ARMED),
            RestAlertState(request.id.copy(deadlineEpochMillis = 2_001), Status.ARMED),
            RestAlertState(request.id, Status.CANCELLED), RestAlertState(request.id, Status.HANDLED),
        )) {
            val fixture = Fixture(state)
            fixture.scheduler().handleExpiry(request.id, { error("unexpected validation") }, { error("unexpected delivery") })
            assertTrue(fixture.events.isEmpty())
            assertEquals(state, fixture.store.saved)
        }
    }

    @Test
    fun validExpiryClaimsBeforePresentationAndUsesFreshSoundPolicy() = runTest {
        val fixture = Fixture(RestAlertState(request.id, Status.ARMED))
        val silent = request.copy(soundEnabled = false)
        var presented: RestAlertRequest? = null
        fixture.scheduler().handleExpiry(request.id, { silent }) {
            assertEquals(Status.HANDLED, fixture.store.saved?.status)
            presented = it
        }
        assertEquals(silent, presented)
        fixture.scheduler().handleExpiry(request.id, { error("already claimed") }, { error("duplicate") })
    }

    @Test
    fun rejectedValidationCancelsTheEligibleRequest() = runTest {
        for (validated in listOf(null, request.copy(id = RestAlertId(8, 3_000)), request.copy(backgroundEnabled = false))) {
            val fixture = Fixture(RestAlertState(request.id, Status.ARMED))
            fixture.scheduler().handleExpiry(request.id, { validated }, { error("rejected delivery") })
            assertEquals(Status.CANCELLED, fixture.store.saved?.status)
            assertEquals(listOf("write:CANCELLED", "cancel", "notifications"), fixture.events)
        }
    }

    @Test
    fun validationAndHandledWriteFailuresPreventPresentationAndRetainRetryableEligibility() = runTest {
        val fixture = Fixture(RestAlertState(request.id, Status.ARMED))
        val failure = IllegalStateException("validation failed")
        assertSame(failure, assertFailsWith<IllegalStateException> {
            fixture.scheduler().handleExpiry(request.id, { throw failure }, { error("unexpected delivery") })
        })
        assertEquals(Status.ARMED, fixture.store.saved?.status)
        fixture.store.writeFailure = failure
        assertSame(failure, assertFailsWith<IllegalStateException> {
            fixture.scheduler().handleExpiry(request.id, { request }, { error("unsaved claim") })
        })
        assertEquals(Status.ARMED, fixture.store.saved?.status)
    }

    @Test
    fun failedPresentationRemainsHandledAcrossRestartAndCannotReplay() = runTest {
        val fixture = Fixture(RestAlertState(request.id, Status.ARMED))
        assertFailsWith<IllegalStateException> {
            fixture.scheduler().handleExpiry(request.id, { request }, { error("presentation failed") })
        }
        assertEquals(Status.HANDLED, fixture.store.saved?.status)
        fixture.scheduler().handleExpiry(request.id, { error("already handled") }, { error("duplicate") })
        fixture.scheduler().cancel()
        assertEquals(Status.HANDLED, fixture.store.saved?.status)
    }

    @Test
    fun concurrentExpiryCallbacksShareOneClaimAndCancellationWaitsForPresentation() = runTest {
        val fixture = Fixture(RestAlertState(request.id, Status.ARMED))
        val scheduler = fixture.scheduler()
        val validationGate = CompletableDeferred<Unit>()
        var deliveries = 0
        val first = launch {
            scheduler.handleExpiry(request.id, { validationGate.await(); request }, { deliveries++ })
        }
        runCurrent()
        val duplicate = launch { scheduler.handleExpiry(request.id, { request }, { deliveries++ }) }
        val cancellation = launch { scheduler.cancel() }
        runCurrent()
        assertFalse(duplicate.isCompleted)
        assertFalse(cancellation.isCompleted)
        validationGate.complete(Unit)
        first.join()
        duplicate.join()
        cancellation.join()
        assertEquals(1, deliveries)
        assertEquals(Status.HANDLED, fixture.store.saved?.status)
    }

    @Test
    fun cancellationWinningTheLockSuppressesALaterCallback() = runTest {
        val fixture = Fixture(RestAlertState(request.id, Status.ARMED))
        val scheduler = fixture.scheduler()
        scheduler.cancel()
        scheduler.handleExpiry(request.id, { error("cancelled") }, { error("late delivery") })
        assertEquals(Status.CANCELLED, fixture.store.saved?.status)
    }

    @Test
    fun cancellingSuspendedValidationReleasesTheLockWithoutClaimingDelivery() = runTest {
        val fixture = Fixture(RestAlertState(request.id, Status.ARMED))
        val scheduler = fixture.scheduler()
        val validationGate = CompletableDeferred<Unit>()
        val callback = launch {
            scheduler.handleExpiry(request.id, { validationGate.await(); request }, { error("cancelled delivery") })
        }
        runCurrent()
        callback.cancel()
        callback.join()
        assertEquals(Status.ARMED, fixture.store.saved?.status)
        scheduler.cancel()
        assertEquals(Status.CANCELLED, fixture.store.saved?.status)
    }

    @Test
    fun schedulingAndCancellationAreSerializedAcrossSuspendingStorage() = runTest {
        val fixture = Fixture()
        val gate = CompletableDeferred<Unit>()
        fixture.store.beforeWrite = { gate.await() }
        val scheduler = fixture.scheduler()
        val scheduling = async { scheduler.schedule(request) }
        runCurrent()
        val cancellation = async { scheduler.cancel() }
        runCurrent()
        assertFalse(cancellation.isCompleted)
        gate.complete(Unit)
        scheduling.await()
        cancellation.await()
        assertEquals(Status.CANCELLED, fixture.store.saved?.status)
        assertTrue(fixture.platform.pending.isEmpty())
    }

    @Test
    fun nativeSubmissionCancellationPropagatesAndTheNextFutureAttemptCanRecover() = runTest {
        val fixture = Fixture()
        fixture.platform.scheduleFailure = CancellationException("interrupted submission")
        assertFailsWith<CancellationException> { fixture.scheduler().schedule(request) }
        assertEquals(Status.ARMED, fixture.store.saved?.status)
        fixture.platform.scheduleFailure = null
        fixture.scheduler().schedule(request)
        assertEquals(request, fixture.platform.pending[request.id])
    }
}

private class Fixture(initial: RestAlertState? = null) {
    var now = 1_000L
    val events = mutableListOf<String>()
    val store = MemoryStateStore(initial, events)
    val platform = RecordingPlatform(events)

    // A new scheduler models process recreation using the same durable store and OS requests.
    fun scheduler() = PersistedRestAlertScheduler(store, platform) { now }
}

private class MemoryStateStore(var saved: RestAlertState?, private val events: MutableList<String>) : RestAlertStateStore {
    var readFailure: Throwable? = null
    var writeFailure: Throwable? = null
    var beforeWrite: suspend () -> Unit = {}

    override suspend fun read(): RestAlertState? {
        readFailure?.let { throw it }
        return saved
    }

    override suspend fun write(state: RestAlertState) {
        beforeWrite()
        writeFailure?.let { throw it }
        saved = state
        events += "write:${state.status}"
    }
}

private class RecordingPlatform(private val events: MutableList<String>) : RestAlertPlatform {
    val pending = mutableMapOf<RestAlertId, RestAlertRequest>()
    var scheduleFailure: Throwable? = null
    var cancelFailure: Throwable? = null
    var notificationFailure: Throwable? = null
    var beforeSchedule: () -> Unit = {}

    override suspend fun schedule(request: RestAlertRequest, wasArmed: Boolean) {
        beforeSchedule()
        events += "schedule:$wasArmed"
        scheduleFailure?.let { throw it }
        pending[request.id] = request
    }

    override suspend fun cancel(id: RestAlertId) {
        events += "cancel"
        cancelFailure?.let { throw it }
        pending.remove(id)
    }

    override suspend fun cancelNotification() {
        events += "notifications"
        notificationFailure?.let { throw it }
    }
}
