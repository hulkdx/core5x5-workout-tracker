package com.hulkdx.core5x5.feature.workout.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.hulkdx.core5x5.feature.workout.domain.ExerciseEdit
import com.hulkdx.core5x5.feature.workout.domain.RestTimerRules
import androidx.room3.Transaction
import com.hulkdx.core5x5.feature.workout.domain.Workout
import kotlinx.coroutines.flow.Flow

@Dao
interface UnfinishedWorkoutDao {
    // Room reruns this small query on changes to any of the three tables. The value is
    // only an invalidation signal; do not distinctUntilChanged it (edits can retain the ID).
    @Query("""
        SELECT session.id FROM unfinished_workout AS session
        LEFT JOIN unfinished_workout_exercise AS exercise ON exercise.workoutId = session.id
        LEFT JOIN unfinished_workout_set AS workoutSet ON workoutSet.workoutId = exercise.workoutId
            AND workoutSet.exercisePosition = exercise.position
        ORDER BY session.id DESC LIMIT 1
    """)
    fun observeWorkoutChanges(): Flow<Long?>

    // Preserve an existing session rather than silently replacing it.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(workout: UnfinishedWorkoutEntity): Long

    @Query("SELECT * FROM unfinished_workout WHERE unfinishedSlot = 1 AND completedAtEpochMillis IS NULL")
    suspend fun getUnfinishedWorkout(): UnfinishedWorkoutEntity?

    // Only one session can be unfinished, so generated IDs preserve training order even if the clock changes.
    @Query("""
        SELECT workout FROM unfinished_workout WHERE completedAtEpochMillis IS NOT NULL
        ORDER BY id DESC LIMIT 1
    """)
    suspend fun getLastCompletedWorkout(): Workout?

    @Query("SELECT * FROM unfinished_workout WHERE id = :workoutId")
    suspend fun getWorkout(workoutId: Long): UnfinishedWorkoutEntity?

    @Query("""
        SELECT * FROM unfinished_workout
        WHERE completedAtEpochMillis IS NOT NULL
        ORDER BY completedAtEpochMillis DESC, id DESC
    """)
    suspend fun getCompletedWorkouts(): List<UnfinishedWorkoutEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExercises(exercises: List<UnfinishedWorkoutExerciseEntity>)

    @Query("SELECT * FROM unfinished_workout_exercise WHERE workoutId = :workoutId ORDER BY position")
    suspend fun getExercises(workoutId: Long): List<UnfinishedWorkoutExerciseEntity>

    // Each lift repeats its latest completed-session weight, even across the two A/B programs.
    @Query("""
        SELECT snapshot.* FROM unfinished_workout_exercise AS snapshot
        WHERE snapshot.workoutId = (
            SELECT MAX(session.id) FROM unfinished_workout AS session
            INNER JOIN unfinished_workout_exercise AS candidate ON candidate.workoutId = session.id
            WHERE candidate.exercise = snapshot.exercise AND session.completedAtEpochMillis IS NOT NULL
        )
    """)
    suspend fun getLatestCompletedExercises(): List<UnfinishedWorkoutExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSets(sets: List<UnfinishedWorkoutSetEntity>)

    @Query("SELECT * FROM unfinished_workout_set WHERE workoutId = :workoutId ORDER BY exercisePosition, position")
    suspend fun getSets(workoutId: Long): List<UnfinishedWorkoutSetEntity>

    @Query("""
        UPDATE unfinished_workout SET workout = :workout, restDeadlineEpochMillis = NULL
        WHERE id = :workoutId AND unfinishedSlot = 1 AND completedAtEpochMillis IS NULL
    """)
    suspend fun updateWorkoutProgram(workoutId: Long, workout: Workout): Int

    // The exercise foreign key cascades to its set rows when switching programs.
    @Query("DELETE FROM unfinished_workout_exercise WHERE workoutId = :workoutId AND position = :position")
    suspend fun removeExercise(workoutId: Long, position: Int)

    @Query("UPDATE unfinished_workout_set SET isCompleted = 0, completedReps = NULL WHERE workoutId = :workoutId")
    suspend fun resetWorkoutSets(workoutId: Long)

    /** Reset all progress/rest atomically while preserving shared exercise prescriptions. */
    @Transaction
    suspend fun switchWorkout(workoutId: Long, workout: Workout): StoredUnfinishedWorkout? {
        val stored = getSession(workoutId) ?: return null
        if (stored.session.completedAtEpochMillis != null || stored.session.unfinishedSlot != 1) return null
        // An explicit target makes retries after a lost acknowledgement idempotent.
        if (stored.session.workout == workout) return stored
        val savedWeights = getLatestCompletedExercises().associate { it.exercise to it.weightKg }
        resetWorkoutSets(workoutId)
        workout.exercises.forEachIndexed { position, exercise ->
            val previous = stored.exercises.firstOrNull { it.position == position }
            if (previous?.exercise != exercise) {
                removeExercise(workoutId, position)
                val replacement = UnfinishedWorkoutExerciseEntity(
                    workoutId, position, exercise, exercise.sets, exercise.reps,
                    savedWeights[exercise] ?: exercise.startingWeightKg,
                )
                insertExercises(listOf(replacement))
                insertSets(List(replacement.sets) { UnfinishedWorkoutSetEntity(workoutId, position, it) })
            }
        }
        check(updateWorkoutProgram(workoutId, workout) == 1)
        return getSession(workoutId)
    }

    @Query("""
        UPDATE unfinished_workout_set SET isCompleted = :isCompleted,
        completedReps = CASE WHEN :isCompleted THEN COALESCE(completedReps, (
            SELECT reps FROM unfinished_workout_exercise
            WHERE workoutId = unfinished_workout_set.workoutId
            AND position = unfinished_workout_set.exercisePosition
        )) ELSE NULL END
        WHERE workoutId = (
            SELECT id FROM unfinished_workout WHERE unfinishedSlot = 1 AND completedAtEpochMillis IS NULL
        ) AND exercisePosition = :exercisePosition AND position = :setPosition
    """)
    suspend fun setSetCompleted(exercisePosition: Int, setPosition: Int, isCompleted: Boolean): Int

    @Query("""
        UPDATE unfinished_workout_set SET isCompleted = 1, completedReps = (
            SELECT reps FROM unfinished_workout_exercise
            WHERE workoutId = unfinished_workout_set.workoutId
            AND position = unfinished_workout_set.exercisePosition
        )
        WHERE workoutId = :workoutId AND exercisePosition = :exercisePosition AND position = :setPosition
        AND isCompleted = 0 AND workoutId IN (
            SELECT id FROM unfinished_workout WHERE unfinishedSlot = 1 AND completedAtEpochMillis IS NULL
        )
    """)
    suspend fun completeNewSet(workoutId: Long, exercisePosition: Int, setPosition: Int): Int

    @Query("""
        UPDATE unfinished_workout_set SET isCompleted = 0, completedReps = NULL
        WHERE workoutId = :workoutId AND exercisePosition = :exercisePosition AND position = :setPosition
        AND isCompleted = 1 AND workoutId IN (
            SELECT id FROM unfinished_workout WHERE unfinishedSlot = 1 AND completedAtEpochMillis IS NULL
        )
    """)
    suspend fun markSetIncomplete(workoutId: Long, exercisePosition: Int, setPosition: Int): Int

    @Query("""
        UPDATE unfinished_workout_set SET completedReps = :completedReps
        WHERE workoutId = :workoutId AND exercisePosition = :exercisePosition AND position = :setPosition
        AND isCompleted = 1 AND workoutId IN (
            SELECT id FROM unfinished_workout WHERE unfinishedSlot = 1 AND completedAtEpochMillis IS NULL
        )
    """)
    suspend fun updateCompletedReps(workoutId: Long, exercisePosition: Int, setPosition: Int, completedReps: Int): Int

    /** Explicit values make retries safe after a committed save loses its acknowledgement. */
    @Transaction
    suspend fun setCompletedReps(
        workoutId: Long, exercisePosition: Int, setPosition: Int, completedReps: Int,
    ): StoredUnfinishedWorkout? {
        require(completedReps > 0)
        if (updateCompletedReps(workoutId, exercisePosition, setPosition, completedReps) != 1) return null
        return getSession(workoutId)
    }

    /** Undo is an explicit saved value, so retrying a lost acknowledgement cannot re-complete it. */
    @Transaction
    suspend fun undoSetCompletion(
        workoutId: Long,
        exercisePosition: Int,
        setPosition: Int,
    ): StoredUnfinishedWorkout? {
        val stored = getSession(workoutId) ?: return null
        if (stored.session.completedAtEpochMillis != null || stored.session.unfinishedSlot != 1) return null
        val set = stored.sets.firstOrNull {
            it.exercisePosition == exercisePosition && it.position == setPosition
        } ?: return null
        if (!set.isCompleted) return stored
        check(markSetIncomplete(workoutId, exercisePosition, setPosition) == 1)
        return getSession(workoutId)
    }

    @Query("""
        UPDATE unfinished_workout SET restDeadlineEpochMillis = :deadlineEpochMillis
        WHERE id = :workoutId AND unfinishedSlot = 1 AND completedAtEpochMillis IS NULL
    """)
    suspend fun replaceRestDeadline(workoutId: Long, deadlineEpochMillis: Long?): Int

    /** The set and rest share one commit; duplicate completion never replaces the deadline. */
    @Transaction
    suspend fun completeSetAndStartRest(
        workoutId: Long,
        exercisePosition: Int,
        setPosition: Int,
        restDurationMillis: Long,
        nowEpochMillis: Long,
    ): StoredUnfinishedWorkout? {
        val stored = getSession(workoutId) ?: return null
        if (stored.session.completedAtEpochMillis != null || stored.session.unfinishedSlot != 1) return null
        val set = stored.sets.firstOrNull {
            it.exercisePosition == exercisePosition && it.position == setPosition
        } ?: return null
        if (set.isCompleted) return stored
        val exercise = stored.exercises.firstOrNull { it.position == exercisePosition } ?: return null
        val duration = exercise.restDurationMillis ?: restDurationMillis
        val deadlineEpochMillis = if (duration == 0L) null
            else RestTimerRules { nowEpochMillis }.start(duration).deadlineEpochMillis
        check(completeNewSet(workoutId, exercisePosition, setPosition) == 1)
        check(replaceRestDeadline(workoutId, deadlineEpochMillis) == 1)
        return getSession(workoutId)
    }

    @Update
    suspend fun updateExercise(exercise: UnfinishedWorkoutExerciseEntity): Int

    @Query("DELETE FROM unfinished_workout_set WHERE workoutId = :workoutId AND exercisePosition = :exercisePosition AND position >= :sets")
    suspend fun removeTrailingSets(workoutId: Long, exercisePosition: Int, sets: Int)

    @Transaction
    suspend fun editExercise(workoutId: Long, exercisePosition: Int, edit: ExerciseEdit): StoredUnfinishedWorkout? {
        val stored = getSession(workoutId) ?: return null
        if (stored.session.completedAtEpochMillis != null || stored.session.unfinishedSlot != 1) return null
        val exercise = stored.exercises.firstOrNull { it.position == exercisePosition } ?: return null
        // Shrinking intentionally removes trailing sets and their logged reps in this transaction.
        check(updateExercise(exercise.copy(weightKg = edit.weightKg,
            sets = edit.sets, reps = edit.reps, restDurationMillis = edit.restDurationMillis)) == 1)
        if (edit.sets < exercise.sets) removeTrailingSets(workoutId, exercisePosition, edit.sets)
        if (edit.sets > exercise.sets) insertSets((exercise.sets until edit.sets).map {
            UnfinishedWorkoutSetEntity(workoutId, exercisePosition, it)
        })
        return getSession(workoutId)
    }

    /** A conditional update makes completion atomic and safe to retry across database instances. */
    @Query("""
        UPDATE unfinished_workout SET completedAtEpochMillis = :completedAtEpochMillis,
        unfinishedSlot = NULL, restDeadlineEpochMillis = NULL
        WHERE id = :workoutId AND unfinishedSlot = 1 AND completedAtEpochMillis IS NULL
    """)
    suspend fun finalizeWorkout(workoutId: Long, completedAtEpochMillis: Long): Int

    @Transaction
    suspend fun getSession(): StoredUnfinishedWorkout? {
        val session = getUnfinishedWorkout() ?: return null
        return StoredUnfinishedWorkout(session, getExercises(session.id), getSets(session.id))
    }

    @Transaction
    suspend fun getSession(workoutId: Long): StoredUnfinishedWorkout? {
        val session = getWorkout(workoutId) ?: return null
        return StoredUnfinishedWorkout(session, getExercises(session.id), getSets(session.id))
    }

    @Transaction
    suspend fun getCompletedSessions(): List<StoredUnfinishedWorkout> =
        getCompletedWorkouts().map { session ->
            StoredUnfinishedWorkout(session, getExercises(session.id), getSets(session.id))
        }

    @Transaction
    suspend fun getPrescriptionHistory(): StoredPrescriptionHistory =
        StoredPrescriptionHistory(getLastCompletedWorkout(), getLatestCompletedExercises())

    /** Read carried weights in the same write transaction that creates the new session. */
    @Transaction
    suspend fun startSessionIfAbsent(session: UnfinishedWorkoutEntity): StoredUnfinishedWorkout {
        getSession()?.let { return it }
        val savedWeights = getLatestCompletedExercises().associate { it.exercise to it.weightKg }
        val exercises = session.workout.exercises.mapIndexed { position, exercise ->
            UnfinishedWorkoutExerciseEntity(
                workoutId = session.id,
                position = position,
                exercise = exercise,
                sets = exercise.sets,
                reps = exercise.reps,
                weightKg = savedWeights[exercise] ?: exercise.startingWeightKg,
            )
        }
        return insertSessionIfAbsent(session, exercises)
    }

    /** The write transaction serializes competing starts, including other database instances. */
    @Transaction
    suspend fun insertSessionIfAbsent(
        session: UnfinishedWorkoutEntity,
        exercises: List<UnfinishedWorkoutExerciseEntity>,
    ): StoredUnfinishedWorkout {
        getSession()?.let { return it }
        val storedSession = session.copy(id = insert(session))
        val storedExercises = exercises.map { it.copy(workoutId = storedSession.id) }
        insertExercises(storedExercises)
        insertSets(storedExercises.flatMap { exercise ->
            List(exercise.sets) { position ->
                UnfinishedWorkoutSetEntity(exercise.workoutId, exercise.position, position)
            }
        })
        return StoredUnfinishedWorkout(storedSession, getExercises(storedSession.id), getSets(storedSession.id))
    }
}

data class StoredPrescriptionHistory(
    val lastCompletedWorkout: Workout?,
    val exercises: List<UnfinishedWorkoutExerciseEntity>,
)
