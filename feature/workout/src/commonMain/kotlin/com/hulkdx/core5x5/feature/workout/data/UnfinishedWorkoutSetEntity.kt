package com.hulkdx.core5x5.feature.workout.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey

@Entity(
    tableName = "unfinished_workout_set",
    primaryKeys = ["workoutId", "exercisePosition", "position"],
    foreignKeys = [ForeignKey(
        entity = UnfinishedWorkoutExerciseEntity::class,
        parentColumns = ["workoutId", "position"],
        childColumns = ["workoutId", "exercisePosition"],
        onDelete = ForeignKey.CASCADE,
    )],
)
internal data class UnfinishedWorkoutSetEntity(
    val workoutId: Long,
    val exercisePosition: Int,
    val position: Int,
    @ColumnInfo(defaultValue = "0") val isCompleted: Boolean = false,
)
