package com.hulkdx.core5x5.app

import com.hulkdx.core5x5.core.restalerts.domain.RestAlertId
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import platform.UserNotifications.UNNotificationPresentationOptionSound
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IosRestAlertNotificationDelegateTest {
    @Test
    fun validatedForegroundExpiryReturnsOnlyTheAllowedSoundOption() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(Job() + dispatcher)
        val id = RestAlertId(31, 45_000)

        for (soundEnabled in listOf(true, false)) {
            val results = mutableListOf<ULong>()
            var handledId: RestAlertId? = null
            handleIosForegroundRestAlert(
                applicationScope = scope,
                id = id,
                handleExpiry = { requestId, present ->
                    handledId = requestId
                    present(RestAlertRequest(requestId, backgroundEnabled = true, soundEnabled = soundEnabled))
                },
                withCompletionHandler = { results += it },
                dispatcher = dispatcher,
            )

            runCurrent()

            assertEquals(id, handledId)
            assertEquals(listOf(if (soundEnabled) UNNotificationPresentationOptionSound else 0uL), results)
        }
        scope.cancel()
    }

    @Test
    fun invalidNativeIdentifierCompletesImmediatelyWithoutRunningExpiry() = runTest {
        val scope = CoroutineScope(Job())
        val results = mutableListOf<ULong>()
        var handled = false

        handleIosForegroundRestAlert(
            applicationScope = scope,
            id = null,
            handleExpiry = { _, _ -> handled = true },
            withCompletionHandler = { results += it },
        )

        assertFalse(handled)
        assertEquals(listOf(0uL), results)
        scope.cancel()
    }

    @Test
    fun expiryFailureReturnsNoPresentationAndCompletesOnce() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(Job() + dispatcher)
        val failure = IllegalStateException("saved inputs failed")
        val results = mutableListOf<ULong>()

        handleIosForegroundRestAlert(
            applicationScope = scope,
            id = RestAlertId(32, 46_000),
            handleExpiry = { _, _ -> throw failure },
            withCompletionHandler = { results += it },
            dispatcher = dispatcher,
        )
        runCurrent()

        assertEquals(listOf(0uL), results)
        scope.cancel()
    }

    @Test
    fun cancellationBeforeExpiryStartsCompletesOnceWithoutRunningHandler() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val owner = Job()
        val scope = CoroutineScope(owner + dispatcher)
        val results = mutableListOf<ULong>()
        var handled = false

        handleIosForegroundRestAlert(
            applicationScope = scope,
            id = RestAlertId(33, 47_000),
            handleExpiry = { _, _ -> handled = true },
            withCompletionHandler = { results += it },
            dispatcher = dispatcher,
        )
        owner.cancel(CancellationException("application closed"))
        runCurrent()

        assertFalse(handled)
        assertEquals(listOf(0uL), results)
        assertTrue(owner.isCancelled)
    }
}
