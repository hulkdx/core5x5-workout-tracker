package com.hulkdx.core5x5.core.restalerts.data

import com.hulkdx.core5x5.core.restalerts.domain.RestAlertId

interface RestAlertStateStore {
    suspend fun read(): RestAlertState?
    suspend fun write(state: RestAlertState)
}

data class RestAlertState(
    val id: RestAlertId,
    val status: Status,
) {
    enum class Status { ARMED, CANCELLED, HANDLED }

    fun encode(): String = "${id.sessionId}:${id.deadlineEpochMillis}:${status.name}"

    companion object {
        fun decode(value: String?): RestAlertState? {
            val fields = value?.split(':') ?: return null
            if (fields.size != 3) return null
            val sessionId = fields[0].toLongOrNull()?.takeIf { it > 0 } ?: return null
            val deadline = fields[1].toLongOrNull() ?: return null
            val status = Status.entries.firstOrNull { it.name == fields[2] } ?: return null
            return RestAlertState(RestAlertId(sessionId, deadline), status)
        }
    }
}
