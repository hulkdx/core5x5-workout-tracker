package com.example.myapplication.feature.workout.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
internal interface UnfinishedWorkoutDao {
    // Preserve an existing session rather than silently replacing it.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(workout: UnfinishedWorkoutEntity)

    @Query("SELECT * FROM unfinished_workout WHERE id = 1")
    suspend fun getUnfinishedWorkout(): UnfinishedWorkoutEntity?
}
