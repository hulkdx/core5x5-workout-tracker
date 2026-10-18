package com.hulkdx.core5x5.uitest.screen.workout

import com.hulkdx.core5x5.feature.workout.domain.Exercise.SQUAT
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.uitest.runAppUiTest
import kotlin.test.Test

class WorkoutScreenTest {
    @Test
    fun startsWorkoutA() = runAppUiTest {
        launchWorkoutScreen(this) {}.verify {
            workoutShown(Workout.A)
        }
    }

    @Test
    fun completingSetShowsFiveReps() = runAppUiTest {
        launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
        }.verify {
            setCompleted(SQUAT, 1, reps = 5)
        }
    }

    @Test
    fun tappingCompletedSetReducesRepsToFour() = runAppUiTest {
        launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
            tapSet(SQUAT, 1)
        }.verify {
            setCompleted(SQUAT, 1, reps = 4)
        }
    }

    @Test
    fun reducedRepsSurviveLeavingAndResumingWorkout() = runAppUiTest {
        launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
            tapSet(SQUAT, 1)
            leaveWorkout()
            resumeWorkout()
        }.verify {
            setCompleted(SQUAT, 1, reps = 4)
        }
    }

    @Test
    fun finishingWorkoutAOffersWorkoutB() = runAppUiTest {
        launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
            pressFinish()
            returnToToday()
        }.verify {
            nextWorkoutShown(Workout.B)
        }
    }
}
