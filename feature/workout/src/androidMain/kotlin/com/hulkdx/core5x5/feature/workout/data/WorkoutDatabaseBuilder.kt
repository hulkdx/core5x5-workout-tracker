package com.hulkdx.core5x5.feature.workout.data

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

internal fun workoutDatabaseBuilder(context: Context): RoomDatabase.Builder<WorkoutDatabase> {
    val appContext = context.applicationContext
    return Room.databaseBuilder<WorkoutDatabase>(
        context = appContext,
        name = appContext.getDatabasePath(WORKOUT_DATABASE_NAME).absolutePath,
    )
}
