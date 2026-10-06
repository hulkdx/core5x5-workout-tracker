package com.hulkdx.core5x5.feature.workout.data

import androidx.room3.Entity
import androidx.room3.ForeignKey
import com.hulkdx.core5x5.feature.workout.domain.Exercise

@Entity(
    tableName = "unfinished_workout_exercise",
    primaryKeys = ["workoutId", "position"],
    foreignKeys = [ForeignKey(
        entity = UnfinishedWorkoutEntity::class,
        parentColumns = ["id"],
        childColumns = ["workoutId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
internal data class UnfinishedWorkoutExerciseEntity(
    val workoutId: Int,
    val position: Int,
    val exercise: Exercise,
    val sets: Int,
    val reps: Int,
    val weightKg: Double,
)

internal data class StoredUnfinishedWorkout(
    val session: UnfinishedWorkoutEntity,
    val exercises: List<UnfinishedWorkoutExerciseEntity>,
    val sets: List<UnfinishedWorkoutSetEntity>,
)
