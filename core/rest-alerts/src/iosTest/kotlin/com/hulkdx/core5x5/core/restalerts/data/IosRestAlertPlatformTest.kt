package com.hulkdx.core5x5.core.restalerts.data

import com.hulkdx.core5x5.core.restalerts.domain.RestAlertId
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import platform.Foundation.timeIntervalSince1970
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class IosRestAlertPlatformTest {
    private val center = FakeNotificationCenter()
    private val platform = IosRestAlertPlatform(center)

    @Test
    fun schedulesUtcCopyAndCurrentSoundPolicy() = runTest {
        val request = futureRequest(sessionId = 901, soundEnabled = true)

        platform.schedule(request, wasArmed = false)

        val pending = assertNotNull(center.pendingFor(request.id))
        assertEquals("Set complete", pending.content.title)
        assertEquals("Ready for your next set.", pending.content.body)
        assertNotNull(pending.content.sound)
        val trigger = pending.trigger as UNCalendarNotificationTrigger
        assertEquals("GMT", trigger.dateComponents.timeZone?.name)

        platform.schedule(request.copy(soundEnabled = false), wasArmed = true)
        assertNull(assertNotNull(center.pendingFor(request.id)).content.sound)
    }

    @Test
    fun unchangedArmedRequestIsLeftAloneAndChangedSoundReplacesIt() = runTest {
        val request = futureRequest(sessionId = 902, soundEnabled = true)
        center.pending[identifier(request.id)] = seedRequest(request.id, "Existing request", soundEnabled = true)

        platform.schedule(request, wasArmed = true)
        assertEquals(0, center.added.size)
        assertEquals("Existing request", assertNotNull(center.pendingFor(request.id)).content.title)

        platform.schedule(request.copy(soundEnabled = false), wasArmed = true)
        val replaced = assertNotNull(center.pendingFor(request.id))
        assertEquals("Set complete", replaced.content.title)
        assertNull(replaced.content.sound)
        assertEquals(1, center.added.size)
    }

    @Test
    fun missingFutureArmedRequestRecoversAndExpiredArmedRequestCannotReplay() = runTest {
        val future = futureRequest(sessionId = 903, soundEnabled = false)
        val expired = RestAlertRequest(
            id = RestAlertId(904, nowEpochMillis() - 5_000),
            backgroundEnabled = true,
            soundEnabled = false,
        )

        platform.schedule(future, wasArmed = true)
        assertNotNull(center.pendingFor(future.id))

        center.pending.remove(identifier(future.id))
        center.delivered += identifier(expired.id)
        platform.schedule(expired, wasArmed = true)

        assertNull(center.pendingFor(expired.id))
        assertTrue(identifier(expired.id) !in center.delivered)
        assertEquals(1, center.added.size)
    }

    @Test
    fun firstAcceptedDeadlineThatBecomesDueUsesAnImmediateRequest() = runTest {
        val due = RestAlertRequest(
            id = RestAlertId(905, nowEpochMillis() - 5_000),
            backgroundEnabled = true,
            soundEnabled = true,
        )

        platform.schedule(due, wasArmed = false)

        assertNull(assertNotNull(center.pendingFor(due.id)).trigger)
    }

    @Test
    fun futureDeadlineWithSubsecondPrecisionRoundsUpToTheNextUtcSecond() = runTest {
        val deadline = ((nowEpochMillis() + 5 * 60_000) / 1000) * 1000 + 1
        val request = RestAlertRequest(
            id = RestAlertId(910, deadline),
            backgroundEnabled = true,
            soundEnabled = false,
        )

        platform.schedule(request, wasArmed = false)

        val trigger = assertNotNull(center.pendingFor(request.id)).trigger as UNCalendarNotificationTrigger
        val actualEpochSeconds = assertNotNull(trigger.nextTriggerDate()).timeIntervalSince1970
        val expectedEpochSeconds = (deadline / 1000 + 1).toDouble()
        assertEquals(expectedEpochSeconds, actualEpochSeconds, absoluteTolerance = 0.01)
        assertEquals("GMT", trigger.dateComponents.timeZone?.name)
    }

    @Test
    fun unrepresentableDeadlineFailsBeforeNativeSubmission() = runTest {
        val request = RestAlertRequest(
            id = RestAlertId(906, Long.MAX_VALUE),
            backgroundEnabled = true,
            soundEnabled = true,
        )

        val failure = assertFailsWith<IllegalStateException> {
            platform.schedule(request, wasArmed = false)
        }

        assertTrue(failure.message.orEmpty().contains("represent"))
        assertTrue(center.added.isEmpty())
    }

    @Test
    fun cancellationTargetsOneIdentityAndOwnedCleanupLeavesForeignRequests() = runTest {
        val first = futureRequest(sessionId = 907, soundEnabled = false)
        val second = futureRequest(sessionId = 908, soundEnabled = false)
        val ownedOrphan = "core5x5.rest.malformed-test-orphan"
        val foreign = "foreign.rest.test-${first.id.sessionId}"
        center.pending[ownedOrphan] = seedRequest(ownedOrphan, "Owned orphan", soundEnabled = false)
        center.pending[foreign] = seedRequest(foreign, "Foreign", soundEnabled = false)
        center.delivered += listOf(ownedOrphan, foreign)
        platform.schedule(first, wasArmed = false)
        platform.schedule(second, wasArmed = false)

        platform.cancel(first.id)
        assertNull(center.pendingFor(first.id))
        assertNotNull(center.pendingFor(second.id))

        platform.cancelNotification()
        assertNull(center.pending[ownedOrphan])
        assertNotNull(center.pending[foreign])
        assertTrue(ownedOrphan !in center.delivered)
        assertTrue(foreign in center.delivered)
    }

    @Test
    fun nativeFailuresAndCancellationPropagateWithoutBeingConvertedToSuccess() = runTest {
        val request = futureRequest(sessionId = 909, soundEnabled = true)
        val failure = IllegalStateException("native add failed")
        center.addFailure = failure
        assertSame(failure, assertFailsWith<IllegalStateException> {
            platform.schedule(request, wasArmed = false)
        })

        center.addFailure = CancellationException("owner closed")
        assertFailsWith<CancellationException> {
            platform.schedule(request, wasArmed = false)
        }
        assertTrue(center.pending.isEmpty())
    }

    private fun futureRequest(sessionId: Long, soundEnabled: Boolean): RestAlertRequest =
        RestAlertRequest(
            id = RestAlertId(sessionId, nowEpochMillis() + 5 * 60_000),
            backgroundEnabled = true,
            soundEnabled = soundEnabled,
        )

    private fun seedRequest(id: RestAlertId, title: String, soundEnabled: Boolean): UNNotificationRequest =
        seedRequest(identifier(id), title, soundEnabled)

    private fun seedRequest(identifier: String, title: String, soundEnabled: Boolean): UNNotificationRequest {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setSound(if (soundEnabled) UNNotificationSound.defaultSound else null)
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(60.0, repeats = false)
        return UNNotificationRequest.requestWithIdentifier(identifier, content, trigger)
    }

    private fun identifier(id: RestAlertId): String = IosRestAlertIdentifier.encode(id)

    private fun nowEpochMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
}

private class FakeNotificationCenter : IosRestAlertNotificationCenter {
    val pending = mutableMapOf<String, UNNotificationRequest>()
    val delivered = mutableSetOf<String>()
    val added = mutableListOf<UNNotificationRequest>()
    var addFailure: Throwable? = null

    override suspend fun pendingRequests(): List<UNNotificationRequest> = pending.values.toList()

    override suspend fun addNotificationRequest(request: UNNotificationRequest) {
        addFailure?.let { throw it }
        pending[request.identifier] = request
        added += request
    }

    override fun removePendingNotificationRequests(identifiers: List<String>) {
        identifiers.forEach(pending::remove)
    }

    override suspend fun deliveredNotificationIdentifiers(): List<String> = delivered.toList()

    override fun removeDeliveredNotifications(identifiers: List<String>) {
        delivered.removeAll(identifiers.toSet())
    }

    fun pendingFor(id: RestAlertId): UNNotificationRequest? = pending[IosRestAlertIdentifier.encode(id)]
}
