package com.hulkdx.core5x5.feature.workout.data

import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutExercise
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutSet
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.feature.workout.domain.WorkoutRepository
import kotlin.time.Clock

internal class RoomWorkoutRepository(
    private val dao: UnfinishedWorkoutDao,
    private val nowEpochMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : WorkoutRepository {
    override suspend fun getUnfinishedWorkout(): UnfinishedWorkout? =
        dao.getSession()?.toDomain()

    override suspend fun getNextWorkout(): Workout =
        dao.getLastCompletedWorkout()?.nextWorkout() ?: Workout.A

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
        return dao.insertSessionIfAbsent(session, exercises).toDomain()
    }

    override suspend fun finalizeWorkout(workoutId: Long): Boolean =
        dao.finalizeWorkout(workoutId, nowEpochMillis()) == 1

    override suspend fun setSetCompleted(
        exercisePosition: Int,
        setPosition: Int,
        isCompleted: Boolean,
    ): Boolean = dao.setSetCompleted(exercisePosition, setPosition, isCompleted) == 1

    private fun StoredUnfinishedWorkout.toDomain(): UnfinishedWorkout = UnfinishedWorkout(
        workout = session.workout,
        startedAtEpochMillis = session.startedAtEpochMillis,
        exercises = exercises.map { exercise ->
            UnfinishedWorkoutExercise(
                exercise = exercise.exercise,
                sets = exercise.sets,
                reps = exercise.reps,
                weightKg = exercise.weightKg,
                setStates = sets.filter { it.exercisePosition == exercise.position }.map {
                    UnfinishedWorkoutSet(it.position, it.isCompleted)
                },
            )
        },
        id = session.id,
    )
}
