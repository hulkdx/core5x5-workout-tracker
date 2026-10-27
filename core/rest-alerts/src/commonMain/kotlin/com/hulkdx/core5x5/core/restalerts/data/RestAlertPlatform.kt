package com.hulkdx.core5x5.core.restalerts.data

import com.hulkdx.core5x5.core.restalerts.domain.RestAlertId
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertRequest

/** Native operations; the shared scheduler owns durable state. */
interface RestAlertPlatform {
    /**
     * wasArmed distinguishes repeated attempts from the first accepted submission.
     * iOS may deliver a first submission that becomes due during saving immediately,
     * but must not turn a repeated attempt into an immediate alert. It is not a delivery receipt.
     */
    suspend fun schedule(request: RestAlertRequest, wasArmed: Boolean)

    suspend fun cancel(id: RestAlertId)

    /** Clear owned notification artifacts, including when no saved identity is available. */
    suspend fun cancelNotification()
}
