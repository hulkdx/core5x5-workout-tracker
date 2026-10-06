package com.hulkdx.core5x5.feature.workout.domain

/** A rest deadline independent of countdown updates, presentation, and storage. */
internal data class RestTimer(val deadlineEpochMillis: Long)

/** v1 default; a future duration preference will supply the value for new timers. */
internal const val DEFAULT_REST_DURATION_MILLIS = 3 * 60 * 1_000L

internal sealed interface RestTimerState {
    data object Idle : RestTimerState

    data class Running(val remainingMillis: Long) : RestTimerState

    data object Expired : RestTimerState
}

/**
 * Calculates rest transitions from immutable deadlines and an injected epoch-millisecond clock.
 * The caller owns the current timer and supplies durations; these rules do not schedule work.
 */
internal class RestTimerRules(private val nowEpochMillis: () -> Long) {
    /** Creates a fresh deadline; storing the result replaces any previous rest. */
    fun start(durationMillis: Long): RestTimer {
        require(durationMillis > 0L) { "Rest duration must be positive." }
        return RestTimer(addDuration(nowEpochMillis(), durationMillis))
    }

    /** Clears running or expired rest without reading the clock. */
    fun skip(): RestTimer? = null

    /**
     * Adds time to a running deadline, or starts that much rest from now after expiry.
     * An absent timer stays absent; extension does not start rest before a set is completed.
     */
    fun extend(timer: RestTimer?, durationMillis: Long): RestTimer? {
        require(durationMillis > 0L) { "Rest extension must be positive." }
        if (timer == null) return null
        val baseEpochMillis = maxOf(timer.deadlineEpochMillis, nowEpochMillis())
        return RestTimer(addDuration(baseEpochMillis, durationMillis))
    }

    /** Expiry is derived on every read; the deadline remains available until skipped or replaced. */
    fun state(timer: RestTimer?): RestTimerState {
        if (timer == null) return RestTimerState.Idle
        val now = nowEpochMillis()
        if (now >= timer.deadlineEpochMillis) return RestTimerState.Expired
        val remainingMillis = timer.deadlineEpochMillis - now
        return RestTimerState.Running(
            // A very large backward clock jump must not wrap into a negative countdown.
            remainingMillis = if (remainingMillis < 0L) Long.MAX_VALUE else remainingMillis,
        )
    }

    private fun addDuration(baseEpochMillis: Long, durationMillis: Long): Long {
        require(baseEpochMillis <= Long.MAX_VALUE - durationMillis) {
            "Rest deadline exceeds the supported timestamp range."
        }
        return baseEpochMillis + durationMillis
    }
}
