package com.hulkdx.core5x5.core.restalerts.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class RestAlertPermissionsTest {
    @Test
    fun resumeRefreshesEveryTimeIncludingUnchangedStatus() = runTest {
        val permissions = TestPermissions()
        assertEquals(RestAlertPermissionStatus(), permissions.status.value)
        repeat(3) { permissions.onResume() }
        assertEquals(3, permissions.refreshes)
        assertEquals(RestNotificationPermission.GRANTED, permissions.status.value.notificationPermission)
    }

    @Test
    fun directFailuresPropagateWhileResumeRetainsTheLastKnownStatusAndCanRetry() = runTest {
        val permissions = TestPermissions()
        permissions.refreshStatus()
        val saved = permissions.status.value
        val failure = IllegalStateException("query failed")
        permissions.failure = failure
        assertSame(failure, assertFailsWith<IllegalStateException> { permissions.refreshStatus() })
        permissions.onResume()
        assertEquals(saved, permissions.status.value)
        permissions.failure = null
        permissions.next = RestAlertPermissionStatus(RestNotificationPermission.DENIED, false)
        permissions.onResume()
        assertEquals(permissions.next, permissions.status.value)
    }

    @Test
    fun resumeDoesNotConsumeCancellationOrSeriousErrors() = runTest {
        val permissions = TestPermissions()
        permissions.failure = CancellationException("owner closed")
        assertFailsWith<CancellationException> { permissions.onResume() }
        permissions.failure = AssertionError("unexpected failure")
        assertFailsWith<AssertionError> { permissions.onResume() }
    }
}

private class TestPermissions : RestAlertPermissions {
    override val status = MutableStateFlow(RestAlertPermissionStatus())
    var next = RestAlertPermissionStatus(RestNotificationPermission.GRANTED, true)
    var failure: Throwable? = null
    var refreshes = 0

    override suspend fun refreshStatus() {
        refreshes++
        failure?.let { throw it }
        status.value = next
    }
}
