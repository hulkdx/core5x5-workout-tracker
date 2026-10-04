package com.hulkdx.core5x5.feature.workout.data

import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutExercise
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.feature.workout.domain.WorkoutRepository
import kotlin.time.Clock

internal class RoomWorkoutRepository(
    private val dao: UnfinishedWorkoutDao,
    private val nowEpochMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : WorkoutRepository {
    override suspend fun startWorkout(workout: Workout): UnfinishedWorkout {
        val session = UnfinishedWorkoutEntity(workout, nowEpochMillis())
        val exercises = workout.exercises.mapIndexed { position, exercise ->
            UnfinishedWorkoutExerciseEntity(
                workoutId = session.id,
                position = position,
                exercise = exercise,
                sets = exercise.sets,
                reps = exercise.reps,
                weightKg = exercise.startingWeightKg,
            )
        }
        val stored = dao.insertSessionIfAbsent(session, exercises)
        return UnfinishedWorkout(
            workout = stored.session.workout,
            startedAtEpochMillis = stored.session.startedAtEpochMillis,
            exercises = stored.exercises.map {
                UnfinishedWorkoutExercise(it.exercise, it.sets, it.reps, it.weightKg)
            },
        )
    }
}
