package com.hulkdx.core5x5.uitest.screen.workout

import com.hulkdx.core5x5.feature.workout.domain.Exercise.*
import com.hulkdx.core5x5.uitest.runAppUiTest
import kotlin.test.Test

class WorkoutRepTest {
    @Test
    fun reducingPastOneResetsTheSet() = runAppUiTest {
        launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
            repeat(5) {
                tapSet(SQUAT, 1)
            }
        }.verify {
            setIncomplete(SQUAT, 1)
        }
    }

    @Test
    fun resetSetCanBeCompletedAgain() = runAppUiTest {
        launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
            repeat(5) {
                tapSet(SQUAT, 1)
            }
            tapSet(SQUAT, 1)
        }.verify {
            setCompleted(SQUAT, 1, reps = 5)
        }
    }

    @Test
    fun nextSetHighlightIsIndependentForEachExercise() = runAppUiTest {
        launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
        }.verify {
            nextSetHighlighted(SQUAT, 2)
            nextSetHighlighted(BENCH_PRESS, 1)
            nextSetHighlighted(BARBELL_ROW, 1)
        }
    }
}
