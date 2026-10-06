package com.hulkdx.core5x5.core.preferences.domain

/** Display preferences never change the kilograms stored with exercise prescriptions or sessions. */
data class TrainingPreferences(
    val weightUnit: WeightUnit = WeightUnit.KG,
    val restDurationSeconds: Long = DEFAULT_REST_DURATION_SECONDS,
) {
    init {
        require(restDurationSeconds in 1..MAX_REST_DURATION_SECONDS) {
            "Rest duration must be positive and representable in milliseconds"
        }
    }

    /** Snapshot this when creating a timer; preference updates must not rewrite a running deadline. */
    val restDurationMillis: Long get() = restDurationSeconds * 1_000L

    companion object {
        const val DEFAULT_REST_DURATION_SECONDS = 180L
        const val MAX_REST_DURATION_SECONDS = Long.MAX_VALUE / 1_000L
    }
}
