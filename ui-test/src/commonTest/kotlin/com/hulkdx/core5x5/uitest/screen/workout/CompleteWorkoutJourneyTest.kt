package com.hulkdx.core5x5.uitest.screen.workout

import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.uitest.runAppUiTest
import com.hulkdx.core5x5.uitest.screen.history.launchHistoryScreen
import kotlin.test.Test

class CompleteWorkoutJourneyTest {
    @Test
    fun completeWorkoutASavesAllSetsAppearsInHistoryAndOffersWorkoutB() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {
            completeAllSets()
            advanceTimeBy(60_000)
            pressFinish()
        }
        workout.verify {
            workoutCompleteScreenShown(completedSets = 15, duration = "01:00")
            allSetsSavedWithFiveReps()
        }
        workout.returnToToday()
        workout.verify {
            nextWorkoutShown(Workout.B)
        }
        launchHistoryScreen(this) {}.verify {
            savedWorkoutShown(Workout.A)
            loggedSquatWeightShown("20 kg")
        }
    }
}
