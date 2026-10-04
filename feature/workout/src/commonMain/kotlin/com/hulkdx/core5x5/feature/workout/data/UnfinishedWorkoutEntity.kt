package com.hulkdx.core5x5.feature.workout.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.hulkdx.core5x5.feature.workout.domain.Workout

/** The single unfinished session; fixed prescriptions remain in the domain. */
@Entity(tableName = "unfinished_workout")
internal data class UnfinishedWorkoutEntity(
    val workout: Workout,
    val startedAtEpochMillis: Long,
    @PrimaryKey val id: Int = 1,
) {
    init {
        require(id == 1) { "Only one unfinished workout can be stored" }
    }
}
