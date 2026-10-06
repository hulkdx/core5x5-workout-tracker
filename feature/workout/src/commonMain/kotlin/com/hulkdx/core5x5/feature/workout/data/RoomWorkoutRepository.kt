package com.hulkdx.core5x5.feature.workout.data

import com.hulkdx.core5x5.feature.workout.domain.CompletedWorkout
import com.hulkdx.core5x5.feature.workout.domain.ExercisePrescription
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutExercise
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutSet
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.feature.workout.domain.WorkoutPrescription
import com.hulkdx.core5x5.feature.workout.domain.WorkoutRepository
import kotlin.time.Clock

internal class RoomWorkoutRepository(
    private val dao: UnfinishedWorkoutDao,
    private val nowEpochMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : WorkoutRepository {
    override suspend fun getUnfinishedWorkout(): UnfinishedWorkout? =
        dao.getSession()?.toDomain()

    override suspend fun getCompletedWorkout(workoutId: Long): CompletedWorkout? {
        val stored = dao.getSession(workoutId) ?: return null
        val completedAt = stored.session.completedAtEpochMillis ?: return null
        return CompletedWorkout(
            id = stored.session.id,
            workout = stored.session.workout,
            startedAtEpochMillis = stored.session.startedAtEpochMillis,
            completedAtEpochMillis = completedAt,
            exercises = stored.exerciseSnapshots(),
        )
    }

    override suspend fun getNextWorkout(): Workout =
        dao.getLastCompletedWorkout()?.nextWorkout() ?: Workout.A

    override suspend fun getNextWorkoutPrescription(): WorkoutPrescription {
        val history = dao.getPrescriptionHistory()
        val nextWorkout = history.lastCompletedWorkout?.nextWorkout() ?: Workout.A
        val savedWeights = history.exercises.associate { it.exercise to it.weightKg }
        return WorkoutPrescription(
            workout = nextWorkout,
            exercises = nextWorkout.exercises.map { exercise ->
                ExercisePrescription(
                    exercise = exercise,
                    sets = exercise.sets,
                    reps = exercise.reps,
                    weightKg = savedWeights[exercise] ?: exercise.startingWeightKg,
                )
            },
        )
    }

    override suspend fun startWorkout(workout: Workout): UnfinishedWorkout {
        val session = UnfinishedWorkoutEntity(workout, nowEpochMillis())
        return dao.startSessionIfAbsent(session).toDomain()
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
        exercises = exerciseSnapshots(),
        id = session.id,
    )

    private fun StoredUnfinishedWorkout.exerciseSnapshots(): List<UnfinishedWorkoutExercise> =
        exercises.map { exercise ->
            UnfinishedWorkoutExercise(
                exercise = exercise.exercise,
                sets = exercise.sets,
                reps = exercise.reps,
                weightKg = exercise.weightKg,
                setStates = sets.filter { it.exercisePosition == exercise.position }.map {
                    UnfinishedWorkoutSet(it.position, it.isCompleted)
                },
            )
        }
}
