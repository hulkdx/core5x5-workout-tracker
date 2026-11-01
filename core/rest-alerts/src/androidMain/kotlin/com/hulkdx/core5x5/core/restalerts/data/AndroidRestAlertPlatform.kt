package com.hulkdx.core5x5.core.restalerts.data

import android.content.BroadcastReceiver
import android.content.Context
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertId
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertRequest

class AndroidRestAlertPlatform(
    private val context: Context,
    private val receiverClass: Class<out BroadcastReceiver>,
) : RestAlertPlatform {
    override suspend fun schedule(request: RestAlertRequest, wasArmed: Boolean) {
        AndroidRestAlertAlarm.schedule(context, receiverClass, request.id)
    }

    override suspend fun cancel(id: RestAlertId) {
        AndroidRestAlertAlarm.cancel(context, receiverClass, id)
    }

    override suspend fun cancelNotification() {
        AndroidRestAlertNotification.cancel(context)
    }
}
