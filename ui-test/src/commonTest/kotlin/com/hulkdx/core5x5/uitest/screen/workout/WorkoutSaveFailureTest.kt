package com.hulkdx.core5x5.uitest.screen.workout

import com.hulkdx.core5x5.feature.workout.domain.Exercise.*
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.uitest.runAppUiTest
import kotlin.test.Test

class WorkoutSaveFailureTest {
    @Test
    fun failedSetSaveKeepsProgressAndRetrySavesOnce() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {}
        blockSetWrites()
        workout.tapSet(SQUAT, 1, waitForSave = false)
        workout.verify {
            setSaveErrorShown()
            setIncomplete(SQUAT, 1)
        }
        allowWorkoutWrites()
        workout.tapSet(SQUAT, 1)
        workout.verify {
            setCompleted(SQUAT, 1, reps = 5)
        }
    }

    @Test
    fun failedFinishKeepsTheWorkoutAndCanBeRetried() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
        }
        blockFinishing()
        workout.pressFinish(waitForSave = false)
        workout.verify {
            finishErrorShown()
            setCompleted(SQUAT, 1, reps = 5)
        }
        allowWorkoutWrites()
        workout.pressFinish()
        workout.verify {
            workoutCompleteScreenShown(completedSets = 1)
        }
        workout.returnToToday()
        workout.verify {
            nextWorkoutShown(Workout.B)
        }
    }
}
