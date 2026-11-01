package com.hulkdx.core5x5.uitest.screen.today

import androidx.compose.ui.unit.dp
import com.hulkdx.core5x5.feature.workout.domain.Exercise
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.uitest.UiTestOperation
import com.hulkdx.core5x5.uitest.UiTestRequest
import com.hulkdx.core5x5.uitest.runAppUiTest
import com.hulkdx.core5x5.uitest.screen.workout.launchWorkoutScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TodayScreenTest {
    @Test
    fun previousCompletionReopensAfterRestartAndLostReplyWithItsRecordedReps() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {
            repeat(3) { tapSet(Exercise.SQUAT, 1) }
            advanceTimeBy(60_000L)
            pressFinish()
        }
        val completed = assertNotNull(savedWorkout())
        workout.returnToToday()
        restart()
        val today = launchTodayScreen(this) {}
        today.verify { previousWorkoutChoiceShown(Workout.A); nextWorkoutShown(Workout.B) }
        advanceTimeBy(30_000L)
        failNext(UiTestOperation.REOPEN, afterCommit = true)
        today.resumePreviousWorkout()
        workout.verify { workoutShown(Workout.A); setCompleted(Exercise.SQUAT, 1, 3); restTimerAbsent() }
        assertEquals(completed.id, unfinishedWorkout()?.id)
        assertEquals(completed.exercises, unfinishedWorkout()?.exercises)
        assertNull(savedWorkout())
        advanceTimeBy(30_000L)
        workout.pressFinish()
        workout.verify { workoutCompleteScreenShown(completedSets = 1, duration = "02:00") }
        assertEquals(completed.id, savedWorkout()?.id)
        workout.returnToToday()
        today.verify { previousWorkoutChoiceShown(Workout.A); nextWorkoutShown(Workout.B) }
        today.startWorkout()
        today.verify { activeWorkoutShown(Workout.B) }
        assertEquals(completed.id, savedWorkout()?.id)
    }

    @Test
    fun previousCompletionFailureAndPendingRetryPreserveSavedProgress() = runAppUiTest(
        width = 320.dp, height = 640.dp,
    ) {
        val workout = launchWorkoutScreen(this) { pressFinish() }
        val completed = assertNotNull(savedWorkout())
        workout.returnToToday()
        val today = launchTodayScreen(this) {}
        today.verify { previousWorkoutChoiceShown(Workout.A) }
        failNext(UiTestOperation.REOPEN)
        today.resumePreviousWorkout(waitForStart = false)
        today.verify { errorShown("Unable to resume the previous workout. Try again or refresh.") }
        assertEquals(completed, savedWorkout())
        assertNull(unfinishedWorkout())
        val reopen = pauseNext(UiTestOperation.REOPEN)
        today.resumePreviousWorkout(waitForStart = false)
        awaitStarted(reopen)
        today.verify { reopeningBlocked() }
        release(reopen)
        workout.verify { workoutShown(Workout.A); restTimerAbsent() }
        assertEquals(completed.id, unfinishedWorkout()?.id)
    }

    @Test
    fun initialLoadingWaitsForStorageWithoutOfferingAnInventedWorkout() = runAppUiTest {
        var read: UiTestRequest? = null
        restart(waitForReady = false) {
            read = pauseNext(UiTestOperation.LOAD_ACTIVE)
        }
        val pausedRead = checkNotNull(read)
        awaitStarted(pausedRead)
        val today = launchTodayScreen(this) {}
        today.verify { initialLoadingShown(); noSessionCreated() }
        release(pausedRead)
        today.verify { nextWorkoutShown(Workout.A) }
        today.startWorkout()
        today.verify { activeWorkoutShown(Workout.A) }
    }

    @Test
    fun initialLoadFailureRetriesWithoutCreatingASession() = runAppUiTest(
        prepare = { requests.block(UiTestOperation.LOAD_ACTIVE) }, waitForReady = false,
    ) {
        val today = launchTodayScreen(this) {}
        today.verify { errorShown("Unable to load your workout."); noSessionCreated() }
        allowRequests(UiTestOperation.LOAD_ACTIVE)
        today.retryLoad()
        today.verify { nextWorkoutShown(Workout.A); noSessionCreated() }
    }

    @Test
    fun failedPreStartSwitchKeepsPreviousProgramAndRetries() = runAppUiTest(
        width = 320.dp, height = 640.dp,
    ) {
        val today = launchTodayScreen(this) {}
        failNext(UiTestOperation.LOAD_PRESCRIPTION)
        today.pressSwitch()
        today.verify {
            errorShown("Unable to switch workouts. Try again.")
            nextWorkoutShown(Workout.A)
            noSessionCreated()
        }
        today.switchWorkout(Workout.B)
        today.startWorkout()
        today.verify { activeWorkoutShown(Workout.B) }
    }

    @Test
    fun pendingPreStartSwitchBlocksStartAndSwitchUntilPrescriptionArrives() = runAppUiTest {
        val today = launchTodayScreen(this) {}
        val read = pauseNext(UiTestOperation.LOAD_PRESCRIPTION)
        today.pressSwitch()
        awaitStarted(read)
        today.verify { actionsBlocked(); nextWorkoutShown(Workout.A); noSessionCreated() }
        release(read)
        today.verify { nextWorkoutShown(Workout.B) }
        today.startWorkout()
        today.verify { activeWorkoutShown(Workout.B) }
    }

    @Test
    fun failedStartRetainsSelectedProgramAndCanBeRetried() = runAppUiTest {
        val today = launchTodayScreen(this) { switchWorkout(Workout.B) }
        failNext(UiTestOperation.START)
        today.startWorkout(waitForStart = false)
        today.verify {
            errorShown("Unable to start your workout. Try again.")
            nextWorkoutShown(Workout.B)
            noSessionCreated()
        }
        today.startWorkout()
        today.verify { activeWorkoutShown(Workout.B) }
    }

    @Test
    fun pendingStartBlocksAnotherStartAndSwitch() = runAppUiTest {
        val today = launchTodayScreen(this) {}
        val start = pauseNext(UiTestOperation.START)
        today.startWorkout(waitForStart = false)
        awaitStarted(start)
        today.verify { actionsBlocked(); noSessionCreated() }
        release(start)
        today.verify { activeWorkoutShown(Workout.A) }
    }

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
