package com.example.myapplication.feature.workout.data

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(entities = [UnfinishedWorkoutEntity::class], version = 1, exportSchema = true)
@ConstructedBy(WorkoutDatabaseConstructor::class)
internal abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun unfinishedWorkoutDao(): UnfinishedWorkoutDao
}

@Suppress("KotlinNoActualForExpect")
internal expect object WorkoutDatabaseConstructor : RoomDatabaseConstructor<WorkoutDatabase> {
    override fun initialize(): WorkoutDatabase
}

internal fun RoomDatabase.Builder<WorkoutDatabase>.buildWorkoutDatabase(): WorkoutDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

internal const val WORKOUT_DATABASE_NAME = "workout.db"
