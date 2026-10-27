package com.hulkdx.core5x5.core.restalerts.data

import com.hulkdx.core5x5.core.restalerts.domain.RestAlertId

/** One native format shared by scheduling, cleanup and notification callbacks. */
object IosRestAlertIdentifier {
    private const val PREFIX = "core5x5.rest."

    fun encode(id: RestAlertId): String = "$PREFIX${id.sessionId}.${id.deadlineEpochMillis}"

    fun owns(value: String): Boolean = value.startsWith(PREFIX)

    fun decode(value: String): RestAlertId? {
        if (!owns(value)) return null
        val fields = value.removePrefix(PREFIX).split('.')
        if (fields.size != 2) return null
        val sessionId = fields[0].toLongOrNull()?.takeIf { it > 0 } ?: return null
        val deadline = fields[1].toLongOrNull() ?: return null
        return RestAlertId(sessionId, deadline)
    }
}
