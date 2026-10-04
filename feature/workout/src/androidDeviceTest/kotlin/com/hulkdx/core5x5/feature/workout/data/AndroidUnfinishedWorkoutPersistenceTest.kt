package com.hulkdx.core5x5.feature.workout.data

import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class AndroidUnfinishedWorkoutPersistenceTest : UnfinishedWorkoutPersistenceTest() {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    override fun databasePath(name: String): String {
        val file = context.getDatabasePath(name)
        check(requireNotNull(file.parentFile).let { it.isDirectory || it.mkdirs() })
        return file.absolutePath
    }

    override fun openDatabase(name: String): WorkoutDatabase =
        Room.databaseBuilder<WorkoutDatabase>(context = context, name = databasePath(name))
            .buildWorkoutDatabase()

    override fun deleteDatabase(name: String) {
        context.deleteDatabase(name)
    }
}
