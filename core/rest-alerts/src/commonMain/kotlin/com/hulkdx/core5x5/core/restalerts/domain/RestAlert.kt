package com.hulkdx.core5x5.core.restalerts.domain

/** Identifies one timed rest using committed workout data. */
data class RestAlertId(val sessionId: Long, val deadlineEpochMillis: Long) {
    init {
        require(sessionId > 0) { "Rest alerts require a saved session ID" }
    }
}

/** Saved policies: sound controls native rest-alert audibility. */
data class RestAlertRequest(
    val id: RestAlertId,
    val backgroundEnabled: Boolean,
    val soundEnabled: Boolean,
)
