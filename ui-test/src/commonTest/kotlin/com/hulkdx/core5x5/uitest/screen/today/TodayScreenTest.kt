package com.hulkdx.core5x5.uitest.screen.today

import androidx.compose.ui.unit.dp
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.uitest.UiTestOperation
import com.hulkdx.core5x5.uitest.runAppUiTest
import com.hulkdx.core5x5.uitest.screen.workout.launchWorkoutScreen
import kotlin.test.Test

class TodayScreenTest {
    @Test
    fun initialLoadingWaitsForStorageWithoutOfferingAnInventedWorkout() = runAppUiTest {
        val read = pauseNext(UiTestOperation.LOAD_ACTIVE)
        restart(waitForReady = false)
        awaitStarted(read)
        val today = launchTodayScreen(this) {}
        today.verify { initialLoadingShown(); noSessionCreated() }
        release(read)
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
    fun failedPreStartSwitchKeepsPreviousProgramAndRetriesAtNarrowLargeText() = runAppUiTest(
        width = 320.dp, height = 640.dp, fontScale = 2f,
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
