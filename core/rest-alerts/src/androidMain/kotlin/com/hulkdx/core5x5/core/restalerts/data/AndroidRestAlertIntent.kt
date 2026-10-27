package com.hulkdx.core5x5.core.restalerts.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertId

object AndroidRestAlertIntent {
    fun create(context: Context, receiverClass: Class<out BroadcastReceiver>, id: RestAlertId): Intent =
        Intent(context, receiverClass).apply {
            action = ACTION
            // PendingIntent identity includes data, but not extras.
            data = Uri.parse("$SCHEME://$AUTHORITY/${id.sessionId}/${id.deadlineEpochMillis}")
        }

    fun decode(intent: Intent): RestAlertId? {
        if (intent.action != ACTION) return null
        val data = intent.data ?: return null
        if (data.scheme != SCHEME || data.authority != AUTHORITY) return null
        val fields = data.pathSegments
        if (fields.size != 2) return null
        val sessionId = fields[0].toLongOrNull()?.takeIf { it > 0 } ?: return null
        val deadline = fields[1].toLongOrNull() ?: return null
        return RestAlertId(sessionId, deadline)
    }

    private const val ACTION = "com.hulkdx.core5x5.REST_ALERT_EXPIRED"
    private const val SCHEME = "core5x5"
    private const val AUTHORITY = "rest-alert"
}
