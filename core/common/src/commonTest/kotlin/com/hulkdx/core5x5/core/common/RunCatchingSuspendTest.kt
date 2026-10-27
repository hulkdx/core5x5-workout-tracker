package com.hulkdx.core5x5.core.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class RunCatchingSuspendTest {
    @Test
    fun returnsTheValueAfterSuspension() = runTest {
        val gate = CompletableDeferred<Int>()
        val result = async { runCatchingSuspend { gate.await() } }
        gate.complete(42)
        assertEquals(42, result.await().getOrThrow())
    }

    @Test
    fun retainsTheOriginalOrdinaryFailure() = runTest {
        val error = IllegalStateException("storage unavailable")
        assertSame(error, runCatchingSuspend<Unit> { throw error }.exceptionOrNull())
    }

    @Test
    fun cancellationAndSeriousErrorsPropagate() = runTest {
        val cancellation = CancellationException("owner closed")
        assertSame(cancellation, assertFailsWith<CancellationException> {
            runCatchingSuspend<Unit> { throw cancellation }
        })
        val error = AssertionError("unexpected invariant failure")
        assertSame(error, assertFailsWith<AssertionError> {
            runCatchingSuspend<Unit> { throw error }
        })
    }
}
