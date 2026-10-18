package com.hulkdx.core5x5.uitest.screen.workout

import com.hulkdx.core5x5.feature.workout.domain.Exercise.*
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.uitest.runAppUiTest
import com.hulkdx.core5x5.uitest.screen.history.launchHistoryScreen
import kotlin.test.Test

class WorkoutRecoveryTest {
    @Test
    fun reducedRepsSurviveReopeningDatabaseAndAppComposition() = runAppUiTest {
        launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
            tapSet(SQUAT, 1)
            restartApp()
            resumeWorkout()
        }.verify {
            workoutShown(Workout.A)
            setCompleted(SQUAT, 1, reps = 4)
        }
    }

    @Test
    fun savedWorkoutStaysInHistoryAfterReopeningWithoutReopeningActive() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
            pressFinish()
            restartApp()
        }
        workout.verify {
            nextWorkoutShown(Workout.B)
        }
        launchHistoryScreen(this) {}.verify {
            savedWorkoutShown(Workout.A)
        }
    }
}
