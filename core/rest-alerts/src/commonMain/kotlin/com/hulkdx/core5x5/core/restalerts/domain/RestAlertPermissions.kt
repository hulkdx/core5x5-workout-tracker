package com.hulkdx.core5x5.core.restalerts.domain

import com.hulkdx.core5x5.core.common.Logger
import com.hulkdx.core5x5.core.common.runCatchingSuspend
import kotlinx.coroutines.flow.StateFlow

enum class RestNotificationPermission { UNKNOWN, GRANTED, DENIED }

data class RestAlertPermissionStatus(
    /** Unknown before refresh or when the user has not yet chosen. */
    val notificationPermission: RestNotificationPermission = RestNotificationPermission.UNKNOWN,
    /** Null before refresh or on platforms without separate precise-timing access. */
    val preciseTimingAllowed: Boolean? = null,
)

/** Publishes the latest system check without opening permission UI. */
interface RestAlertPermissions {
    val status: StateFlow<RestAlertPermissionStatus>

    /** Reads current system status; failures propagate to the caller. */
    suspend fun refreshStatus()

    /** A lifecycle refresh logs ordinary failures; cancellation still propagates. */
    suspend fun onResume() {
        runCatchingSuspend { refreshStatus() }.onFailure { error ->
            Logger.warn("RestAlerts", "Could not refresh rest alert permissions", error)
        }
    }
}
