package com.hulkdx.core5x5.feature.workout.data

import com.hulkdx.core5x5.feature.workout.domain.CompletedWorkout
import com.hulkdx.core5x5.feature.workout.domain.ExercisePrescription
import com.hulkdx.core5x5.feature.workout.domain.RestTimer
import com.hulkdx.core5x5.feature.workout.domain.RestTimerRules
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkout
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutExercise
import com.hulkdx.core5x5.feature.workout.domain.UnfinishedWorkoutSet
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.feature.workout.domain.WorkoutPrescription
import com.hulkdx.core5x5.core.training.domain.CompletedExerciseType
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutExercise
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutRecord
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutSet
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutSource
import com.hulkdx.core5x5.core.training.domain.CompletedWorkoutType
import com.hulkdx.core5x5.feature.workout.domain.WorkoutRepository
import kotlin.time.Clock

internal class RoomWorkoutRepository(
    private val dao: UnfinishedWorkoutDao,
    private val nowEpochMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : WorkoutRepository, CompletedWorkoutSource {
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

    override suspend fun getCompletedWorkouts(): List<CompletedWorkoutRecord> =
        dao.getCompletedSessions().map { it.toCompletedWorkoutRecord() }

    override suspend fun getCompletedWorkoutById(workoutId: Long): CompletedWorkoutRecord? =
        dao.getSession(workoutId)?.takeIf { it.session.completedAtEpochMillis != null }
            ?.toCompletedWorkoutRecord()

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

    override suspend fun completeSetAndStartRest(
        workoutId: Long,
        exercisePosition: Int,
        setPosition: Int,
        restDurationMillis: Long,
    ): UnfinishedWorkout? {
        val timer = RestTimerRules(nowEpochMillis).start(restDurationMillis)
        return dao.completeSetAndStartRest(
            workoutId, exercisePosition, setPosition, timer.deadlineEpochMillis,
        )?.toDomain()
    }

    private fun StoredUnfinishedWorkout.toDomain(): UnfinishedWorkout = UnfinishedWorkout(
        workout = session.workout,
        startedAtEpochMillis = session.startedAtEpochMillis,
        exercises = exerciseSnapshots(),
        id = session.id,
        restTimer = session.restDeadlineEpochMillis?.let(::RestTimer),
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

    private fun StoredUnfinishedWorkout.toCompletedWorkoutRecord(): CompletedWorkoutRecord {
        val completedAt = requireNotNull(session.completedAtEpochMillis)
        return CompletedWorkoutRecord(
            id = session.id,
            workout = session.workout.toCompletedWorkoutType(),
            startedAtEpochMillis = session.startedAtEpochMillis,
            completedAtEpochMillis = completedAt,
            exercises = exercises.map { exercise ->
                CompletedWorkoutExercise(
                    exercise = exercise.exercise.toCompletedExerciseType(),
                    sets = exercise.sets,
                    reps = exercise.reps,
                    weightKg = exercise.weightKg,
                    setStates = sets.filter { it.exercisePosition == exercise.position }.map {
                        CompletedWorkoutSet(it.position, it.isCompleted)
                    },
                )
            },
        )
    }

    private fun Workout.toCompletedWorkoutType(): CompletedWorkoutType = when (this) {
        Workout.A -> CompletedWorkoutType.A
        Workout.B -> CompletedWorkoutType.B
    }

    private fun Exercise.toCompletedExerciseType(): CompletedExerciseType = when (this) {
        Exercise.SQUAT -> CompletedExerciseType.SQUAT
        Exercise.BENCH_PRESS -> CompletedExerciseType.BENCH_PRESS
        Exercise.BARBELL_ROW -> CompletedExerciseType.BARBELL_ROW
        Exercise.OVERHEAD_PRESS -> CompletedExerciseType.OVERHEAD_PRESS
        Exercise.DEADLIFT -> CompletedExerciseType.DEADLIFT
    }
}
