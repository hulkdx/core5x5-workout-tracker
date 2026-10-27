package com.hulkdx.core5x5.core.restalerts.data

import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissionStatus
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissions
import com.hulkdx.core5x5.core.restalerts.domain.RestNotificationPermission
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class IosRestAlertPermissionsTest {
    private val center = FakePermissionCenter()
    private val permissions: RestAlertPermissions = IosRestAlertPermissions(center)

    @Test
    fun authorizationStatesMapToSharedPermissionValuesAndStartUnknown() = runTest {
        assertEquals(RestAlertPermissionStatus(), permissions.status.value)

        val cases = listOf(
            UNAuthorizationStatusAuthorized to RestNotificationPermission.GRANTED,
            UNAuthorizationStatusProvisional to RestNotificationPermission.GRANTED,
            UNAuthorizationStatusEphemeral to RestNotificationPermission.GRANTED,
            UNAuthorizationStatusDenied to RestNotificationPermission.DENIED,
            Long.MAX_VALUE to RestNotificationPermission.UNKNOWN,
        )
        for ((nativeStatus, expected) in cases) {
            center.status = nativeStatus
            permissions.refreshStatus()
            assertEquals(RestAlertPermissionStatus(expected), permissions.status.value)
        }
    }

    @Test
    fun queryFailuresPropagateAndKeepTheLastSuccessfulSnapshot() = runTest {
        center.status = UNAuthorizationStatusAuthorized
        permissions.refreshStatus()
        val failure = IllegalStateException("settings query failed")
        center.failure = failure

        assertSame(failure, assertFailsWith<IllegalStateException> { permissions.refreshStatus() })
        assertEquals(RestAlertPermissionStatus(RestNotificationPermission.GRANTED), permissions.status.value)
    }

    @Test
    fun queryCancellationPropagatesAndKeepsTheLastSuccessfulSnapshot() = runTest {
        center.status = UNAuthorizationStatusDenied
        permissions.refreshStatus()
        center.failure = CancellationException("owner closed")

        assertFailsWith<CancellationException> { permissions.refreshStatus() }
        assertEquals(RestAlertPermissionStatus(RestNotificationPermission.DENIED), permissions.status.value)
    }
}

private class FakePermissionCenter : IosRestAlertPermissionCenter {
    var status: Long = Long.MAX_VALUE
    var failure: Throwable? = null

    override suspend fun authorizationStatus(): Long {
        failure?.let { throw it }
        return status
    }
}
