package com.hulkdx.core5x5.uitest.screen.workout

import com.hulkdx.core5x5.feature.workout.domain.Exercise.*
import com.hulkdx.core5x5.uitest.runAppUiTest
import kotlin.test.Test

class RestTimerScreenTest {
    @Test
    fun firstCompletionStartsDefaultRestAndKeepsSetsAvailable() = runAppUiTest {
        launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
        }.verify {
            restTimerShown("03:00")
            nextSetHighlighted(SQUAT, 2)
            bottomNavigationHidden()
        }
    }

    @Test
    fun runningRestRecoversFromSavedDeadlineAfterReopening() = runAppUiTest {
        launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
            advanceTimeBy(10_000)
            restartApp()
            resumeWorkout()
        }.verify {
            restTimerShown("02:50")
        }
    }

    @Test
    fun expiredRestRecoversAtZeroAndTheNextSetStartsFreshRest() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
            advanceTimeBy(180_000)
            restartApp()
            resumeWorkout()
        }
        workout.verify {
            restTimerShown("00:00", expired = true)
        }
        workout.tapSet(SQUAT, 2)
        workout.verify {
            restTimerShown("03:00")
        }
    }

    @Test
    fun repReductionAndResetKeepRestButRecompletionReplacesIt() = runAppUiTest {
        val originalDeadline = nowEpochMillis + 180_000
        val workout = launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
            advanceTimeBy(10_000)
            repeat(5) {
                tapSet(SQUAT, 1)
            }
        }
        workout.verify {
            setIncomplete(SQUAT, 1)
            restDeadlineSaved(originalDeadline)
        }
        workout.tapSet(SQUAT, 1)
        workout.verify {
            restTimerShown("03:00")
            restDeadlineSaved(originalDeadline + 10_000)
        }
    }
}
