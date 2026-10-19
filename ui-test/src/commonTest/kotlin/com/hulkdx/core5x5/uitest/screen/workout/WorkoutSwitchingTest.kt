package com.hulkdx.core5x5.uitest.screen.workout

import com.hulkdx.core5x5.feature.workout.domain.Exercise.*
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.uitest.UiTestOperation
import com.hulkdx.core5x5.uitest.runAppUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class WorkoutSwitchingTest {
    @Test
    fun lostSwitchReplyBlocksMutationsAndRetryConfirmsBWithoutSwitchingBack() = runAppUiTest {
        val workout = launchWorkoutScreen(this) { tapSet(SQUAT, 1) }
        val original = assertNotNull(unfinishedWorkout())
        failNext(UiTestOperation.SWITCH, afterCommit = true)
        workout.pressSwitchExpectingFailure()
        workout.verify {
            switchErrorShown()
            workoutShown(Workout.A)
            setCompleted(SQUAT, 1, reps = 5)
            mutationsBlocked(switchCanRetry = true)
        }
        assertEquals(Workout.B, assertNotNull(unfinishedWorkout()).workout)
        workout.switchWorkout(Workout.B)
        workout.verify {
            workoutShown(Workout.B)
            setIncomplete(SQUAT, 1)
            restTimerAbsent()
            mutationsAvailable()
        }
        val confirmed = assertNotNull(unfinishedWorkout())
        assertEquals(original.id, confirmed.id)
        assertEquals(original.startedAtEpochMillis, confirmed.startedAtEpochMillis)
        workout.tapSet(DEADLIFT, 1)
        workout.verify { setCompleted(DEADLIFT, 1, reps = 5) }
    }

    @Test
    fun pendingSwitchKeepsDisplayedProgramAndBlocksConflictingActions() = runAppUiTest {
        val workout = launchWorkoutScreen(this) { tapSet(SQUAT, 1) }
        val switch = pauseNext(UiTestOperation.SWITCH)
        workout.pressSwitchExpectingFailure()
        awaitStarted(switch)
        workout.verify {
            workoutShown(Workout.A)
            mutationsBlocked()
        }
        assertEquals(Workout.A, assertNotNull(unfinishedWorkout()).workout)
        release(switch)
        workout.verify {
            // Wait for the asynchronous program change before checking its controls.
            mutationsAvailable()
        }
        workout.verify { workoutShown(Workout.B) }
    }

    @Test
    fun switchingToBClearsProgressAndRest() = runAppUiTest {
        launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
            switchWorkout(Workout.B)
        }.verify {
            workoutShown(Workout.B)
            setIncomplete(SQUAT, 1)
            restTimerAbsent()
            restDeadlineSaved(null)
        }
    }

    @Test
    fun switchingBackToAClearsBProgress() = runAppUiTest {
        launchWorkoutScreen(this) {
            switchWorkout(Workout.B)
            tapSet(DEADLIFT, 1)
            switchWorkout(Workout.A)
        }.verify {
            workoutShown(Workout.A)
            setIncomplete(SQUAT, 1)
            nextSetHighlighted(BENCH_PRESS, 1)
            restTimerAbsent()
        }
    }

    @Test
    fun switchingKeepsTheEditedSquatWeight() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {}
        workout.editExercise(SQUAT) {
            increaseWeight()
            save()
        }
        workout.switchWorkout(Workout.B)
        workout.verify {
            prescriptionShown(SQUAT, "5 × 5 · 22.5 kg")
        }
    }

    @Test
    fun partialWorkoutBFinishesAndOffersA() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {
            switchWorkout(Workout.B)
            tapSet(DEADLIFT, 1)
            pressFinish()
        }
        workout.verify {
            workoutCompleteScreenShown(completedSets = 1, prescribedSets = 11)
        }
        workout.returnToToday()
        workout.verify {
            nextWorkoutShown(Workout.A)
        }
    }

    @Test
    fun failedSwitchKeepsProgressAndCanBeRetried() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
        }
        blockSwitching()
        workout.pressSwitchExpectingFailure()
        workout.verify {
            switchErrorShown()
            setCompleted(SQUAT, 1, reps = 5)
            mutationsBlocked(switchCanRetry = true)
        }
        allowWorkoutWrites()
        workout.switchWorkout(Workout.B)
        workout.verify {
            workoutShown(Workout.B)
            setIncomplete(SQUAT, 1)
        }
    }
}
