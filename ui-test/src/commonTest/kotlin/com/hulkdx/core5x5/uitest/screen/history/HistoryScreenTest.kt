package com.hulkdx.core5x5.uitest.screen.history

import com.hulkdx.core5x5.core.training.domain.CompletedExerciseType
import com.hulkdx.core5x5.feature.workout.domain.Exercise.SQUAT
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.uitest.UiTestOperation
import com.hulkdx.core5x5.uitest.runAppUiTest
import com.hulkdx.core5x5.uitest.screen.workout.launchWorkoutScreen
import kotlin.test.Test

class HistoryScreenTest {
    @Test
    fun immediateDeleteAndLostReplyRetryReachEmptyHistory() = runAppUiTest {
        launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
            pressFinish()
            returnToToday()
        }
        val history = launchHistoryScreen(this) {}
        history.verify { savedWorkoutShown(Workout.A) }
        failNext(UiTestOperation.DELETE_COMPLETE, afterCommit = true)
        history.deleteWorkout(waitForSave = false)
        history.verify { deletionErrorShown() }
        history.retryDeletion()
        history.verify {
            emptyHistoryShown()
            noGraphShown()
        }
        restart()
        launchHistoryScreen(this) {}.verify { emptyHistoryShown() }
    }

    @Test
    fun deletingLatestSessionRecomputesGraphFromRemainingHistory() = runAppUiTest {
        launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
            pressFinish()
            returnToToday()
        }
        launchWorkoutScreen(this) {
            editExercise(SQUAT) {
                increaseWeight()
                save()
            }
            tapSet(SQUAT, 1)
            pressFinish()
            returnToToday()
        }
        val history = launchHistoryScreen(this) {}
        history.openGraph()
        history.verify { loggedSquatWeightShown("22.5 kg") }
        history.returnFromGraph()
        history.revealDelete(2)
        history.verify { savedWorkoutShown(Workout.B, id = 2, prescribedSets = 11) }
        history.tapRevealedDelete(2)
        history.verify { savedWorkoutShown(Workout.A, id = 1) }
        history.openGraph()
        history.verify { latestWeightShown("20 kg") }
    }

    @Test
    fun swipesRevealOneDeleteAtATimeAndDismissWithoutDeleting() = runAppUiTest {
        repeat(5) {
            launchWorkoutScreen(this) {
                pressFinish()
                returnToToday()
            }
        }
        val history = launchHistoryScreen(this) {}
        history.scrollWithVerticalSwipe()
        history.revealDelete(1)
        history.verify { deleteRevealState(1, revealed = true) }
        history.closeDeleteWithSwipe(1)
        history.verify { deleteRevealState(1, revealed = false) }
        history.revealDeleteWithAccessibility(1)
        history.verify { deleteRevealState(1, revealed = true) }
        history.hideDeleteWithAccessibility(1)
        history.verify { deleteRevealState(1, revealed = false) }
        history.revealDelete(2)
        history.verify {
            deleteRevealState(1, revealed = false)
            deleteRevealState(2, revealed = true)
        }
        history.tapWorkout(1)
        history.verify {
            deleteRevealState(2, revealed = false)
            savedWorkoutShown(Workout.A, id = 1)
            savedWorkoutShown(Workout.B, id = 2, prescribedSets = 11)
        }
    }

    @Test
    fun savedWorkoutAppearsInHistoryWithItsLoggedWeight() = runAppUiTest {
        launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
            pressFinish()
            returnToToday()
        }
        launchHistoryScreen(this) { openGraph() }.verify {
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
    fun graphLiftSelectionUpdatesWithinDedicatedScreen() = runAppUiTest {
        launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
            pressFinish()
            returnToToday()
        }
        launchHistoryScreen(this) {
            openGraph()
            selectLift(CompletedExerciseType.BENCH_PRESS)
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
