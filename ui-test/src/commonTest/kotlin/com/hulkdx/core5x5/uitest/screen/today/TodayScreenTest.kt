package com.hulkdx.core5x5.uitest.screen.today

import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.uitest.runAppUiTest
import com.hulkdx.core5x5.uitest.screen.workout.launchWorkoutScreen
import kotlin.test.Test

class TodayScreenTest {
    @Test
    fun choosingWorkoutBStartsB() = runAppUiTest {
        launchTodayScreen(this) {
            switchWorkout(Workout.B)
            startWorkout()
        }.verify {
            activeWorkoutShown(Workout.B)
        }
    }

    @Test
    fun choosingAnotherProgramDoesNotCreateASession() = runAppUiTest {
        launchTodayScreen(this) {
            switchWorkout(Workout.B)
        }.verify {
            nextWorkoutShown(Workout.B)
            noSessionCreated()
        }
    }

    @Test
    fun anUnfinishedWorkoutTakesPrecedenceOverStartingAnother() = runAppUiTest {
        launchWorkoutScreen(this) {
            leaveWorkout()
        }
        launchTodayScreen(this) {}.verify {
            resumeOnlyShown()
        }
    }
}
