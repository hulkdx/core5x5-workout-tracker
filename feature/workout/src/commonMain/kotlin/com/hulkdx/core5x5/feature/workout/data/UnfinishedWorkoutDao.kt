package com.hulkdx.core5x5.feature.workout.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction

@Dao
internal interface UnfinishedWorkoutDao {
    // Preserve an existing session rather than silently replacing it.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(workout: UnfinishedWorkoutEntity)

    @Query("SELECT * FROM unfinished_workout WHERE id = 1")
    suspend fun getUnfinishedWorkout(): UnfinishedWorkoutEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExercises(exercises: List<UnfinishedWorkoutExerciseEntity>)

    @Query("SELECT * FROM unfinished_workout_exercise WHERE workoutId = 1 ORDER BY position")
    suspend fun getExercises(): List<UnfinishedWorkoutExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSets(sets: List<UnfinishedWorkoutSetEntity>)

    @Query("SELECT * FROM unfinished_workout_set WHERE workoutId = 1 ORDER BY exercisePosition, position")
    suspend fun getSets(): List<UnfinishedWorkoutSetEntity>

    @Query("""
        UPDATE unfinished_workout_set SET isCompleted = :isCompleted
        WHERE workoutId = 1 AND exercisePosition = :exercisePosition AND position = :setPosition
    """)
    suspend fun setSetCompleted(exercisePosition: Int, setPosition: Int, isCompleted: Boolean): Int

    @Transaction
    suspend fun getSession(): StoredUnfinishedWorkout? {
        val session = getUnfinishedWorkout() ?: return null
        return StoredUnfinishedWorkout(session, getExercises(), getSets())
    }

    /** The write transaction serializes competing starts, including other database instances. */
    @Transaction
    suspend fun insertSessionIfAbsent(
        session: UnfinishedWorkoutEntity,
        exercises: List<UnfinishedWorkoutExerciseEntity>,
    ): StoredUnfinishedWorkout {
        getSession()?.let { return it }
        insert(session)
        insertExercises(exercises)
        insertSets(exercises.flatMap { exercise ->
            List(exercise.sets) { position ->
                UnfinishedWorkoutSetEntity(exercise.workoutId, exercise.position, position)
            }
        })
        return StoredUnfinishedWorkout(session, getExercises(), getSets())
    }
}
