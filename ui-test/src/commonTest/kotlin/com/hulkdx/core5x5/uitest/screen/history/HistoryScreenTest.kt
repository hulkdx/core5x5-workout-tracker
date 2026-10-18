package com.hulkdx.core5x5.uitest.screen.history

import com.hulkdx.core5x5.core.training.domain.CompletedExerciseType
import com.hulkdx.core5x5.feature.workout.domain.Exercise.SQUAT
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.uitest.runAppUiTest
import com.hulkdx.core5x5.uitest.screen.workout.launchWorkoutScreen
import kotlin.test.Test

class HistoryScreenTest {
    @Test
    fun savedWorkoutAppearsInHistoryWithItsLoggedWeight() = runAppUiTest {
        launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
            pressFinish()
            returnToToday()
        }
        launchHistoryScreen(this) {}.verify {
            savedWorkoutShown(Workout.A)
            loggedSquatWeightShown("20 kg")
        }
    }

    @Test
    fun historyIsEmptyBeforeAnyWorkoutIsFinished() = runAppUiTest {
        launchHistoryScreen(this) {}.verify {
            emptyHistoryShown()
        }
    }

    @Test
    fun graphLiftSelectionSurvivesTabChanges() = runAppUiTest {
        launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
            pressFinish()
            returnToToday()
        }
        launchHistoryScreen(this) {
            selectLift(CompletedExerciseType.BENCH_PRESS)
            visitSettingsAndReturn()
        }.verify {
            selectedLiftShown("Bench Press")
        }
    }

    @Test
    fun successiveSavedWorkoutsAreBothAvailableInHistory() = runAppUiTest {
        launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
            pressFinish()
            returnToToday()
        }
        launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
            pressFinish()
            returnToToday()
        }
        launchHistoryScreen(this) {}.verify {
            savedWorkoutShown(Workout.A, id = 1)
            savedWorkoutShown(Workout.B, id = 2, prescribedSets = 11)
        }
    }

    @Test
    fun workoutWithoutLoggedSetsCanStillBeSavedAndShownInHistory() = runAppUiTest {
        launchWorkoutScreen(this) {
            pressFinish()
            returnToToday()
        }
        launchHistoryScreen(this) {}.verify {
            savedWorkoutShown(Workout.A)
        }
    }
}
