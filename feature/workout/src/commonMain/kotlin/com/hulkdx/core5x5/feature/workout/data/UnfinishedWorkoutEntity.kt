package com.hulkdx.core5x5.feature.workout.data

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.hulkdx.core5x5.feature.workout.domain.Workout

/** Session metadata retained after completion; exercise snapshots are stored separately. */
@Entity(
    tableName = "unfinished_workout",
    indices = [Index(value = ["unfinishedSlot"], unique = true)],
)
internal data class UnfinishedWorkoutEntity(
    val workout: Workout,
    val startedAtEpochMillis: Long,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val completedAtEpochMillis: Long? = null,
    // The unique slot admits one unfinished session and any number of completed sessions (null).
    val unfinishedSlot: Int? = 1,
    val restDeadlineEpochMillis: Long? = null,
) {
    init {
        require(unfinishedSlot == null || unfinishedSlot == 1)
        require((completedAtEpochMillis == null) == (unfinishedSlot == 1))
        require(completedAtEpochMillis == null || restDeadlineEpochMillis == null)
    }
}
