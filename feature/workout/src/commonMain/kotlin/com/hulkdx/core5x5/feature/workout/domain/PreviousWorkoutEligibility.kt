package com.hulkdx.core5x5.feature.workout.domain

/** Matches the UTC calendar dates used in History; future completions are ineligible. */
fun isWorkoutFinishedToday(completedAtEpochMillis: Long, nowEpochMillis: Long): Boolean =
    completedAtEpochMillis <= nowEpochMillis &&
        completedAtEpochMillis.floorDiv(86_400_000L) == nowEpochMillis.floorDiv(86_400_000L)
