package com.hulkdx.core5x5.core.restalerts.data

import com.hulkdx.core5x5.core.restalerts.data.RestAlertState.Status
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertId
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertRequest
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertScheduler
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PersistedRestAlertScheduler(
    private val stateStore: RestAlertStateStore,
    private val platform: RestAlertPlatform,
    private val nowEpochMillis: () -> Long,
) : RestAlertScheduler {
    private val mutex = Mutex()

    override suspend fun schedule(request: RestAlertRequest) = mutex.withLock {
        val isExpired = request.id.deadlineEpochMillis <= nowEpochMillis()
        if (!request.backgroundEnabled || isExpired) {
            cancelLocked()
            return@withLock
        }

        val previous = stateStore.read()
        if (previous?.id == request.id && previous.status == Status.HANDLED) {
            return@withLock
        }
        if (previous?.id != request.id) {
            previous?.let { platform.cancel(it.id) }
            platform.cancelNotification()
        }

        val wasArmed = previous?.id == request.id && previous.status == Status.ARMED
        stateStore.write(RestAlertState(request.id, Status.ARMED))
        // A rest accepted before expiry may become due while saving; allow its alarm to fire.
        platform.schedule(request, wasArmed)
    }

    override suspend fun cancel() = mutex.withLock { cancelLocked() }

    /**
     * Native callbacks supply fresh validation, including expiry, and presentation.
     * Both run under this lock and must not call schedule/cancel themselves.
     */
    suspend fun handleExpiry(
        id: RestAlertId,
        validate: suspend () -> RestAlertRequest?,
        present: suspend (RestAlertRequest) -> Unit,
    ) = mutex.withLock {
        val saved = stateStore.read() ?: return@withLock
        if (saved.id != id || saved.status != Status.ARMED) return@withLock

        val request = validate()
        if (request == null || request.id != id || !request.backgroundEnabled) {
            cancelLocked()
            return@withLock
        }

        stateStore.write(saved.copy(status = Status.HANDLED))
        present(request)
    }

    private suspend fun cancelLocked() {
        val saved = stateStore.read()
        if (saved != null) {
            if (saved.status == Status.ARMED) {
                stateStore.write(saved.copy(status = Status.CANCELLED))
            }
            platform.cancel(saved.id)
        }
        platform.cancelNotification()
    }
}
