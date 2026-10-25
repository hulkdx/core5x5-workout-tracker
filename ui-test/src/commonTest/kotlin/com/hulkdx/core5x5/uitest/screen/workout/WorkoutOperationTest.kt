package com.hulkdx.core5x5.uitest.screen.workout

import androidx.compose.ui.unit.dp
import com.hulkdx.core5x5.feature.workout.domain.Exercise.BENCH_PRESS
import com.hulkdx.core5x5.feature.workout.domain.Exercise.DEADLIFT
import com.hulkdx.core5x5.feature.workout.domain.Exercise.SQUAT
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.uitest.UiTestOperation
import com.hulkdx.core5x5.uitest.runAppUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WorkoutOperationTest {
    @Test
    fun changingSelectedExerciseWithPinnedRestPreservesTheVisibleScrollAnchor() = runAppUiTest(
        width = 320.dp, height = 640.dp,
    ) {
        val workout = launchWorkoutScreen(this) { tapSet(SQUAT, 1) }
        val verify = workout.verify {}
        verify.geometry(BENCH_PRESS) // Bring this card into view once, before recording its position.
        val anchor = verify.exerciseTop(BENCH_PRESS)
        workout.tapSet(BENCH_PRESS, 1, scroll = false)
        assertEquals(anchor, verify.exerciseTop(BENCH_PRESS))
        verify.pinnedTimerDoesNotOverlapList()
        workout.pressFinish()
    }

    @Test
    fun firstSetSaveBlocksConflictsAndKeepsGeometryThroughRunningAndExpiredRest() = runAppUiTest(
        width = 320.dp, height = 640.dp,
    ) {
        val workout = launchWorkoutScreen(this) {}
        val expectedGeometry = workout.verify {}.geometry(SQUAT)
        val save = pauseNext(UiTestOperation.COMPLETE_SET)
        workout.tapSet(SQUAT, 1, waitForSave = false)
        awaitStarted(save)
        workout.verify {
            mutationsBlocked()
            setIncomplete(SQUAT, 1)
            restTimerAbsent()
            assertEquals(expectedGeometry, geometry(SQUAT))
        }
        release(save)
        workout.verify {
            setCompleted(SQUAT, 1, reps = 5)
            restTimerShown("03:00")
            assertEquals(expectedGeometry, geometry(SQUAT))
            pinnedTimerDoesNotOverlapList()
        }
        workout.advanceTimeBy(180_000)
        workout.restartApp()
        workout.resumeWorkout()
        workout.verify {
            restTimerShown("00:00", expired = true)
            assertEquals(expectedGeometry, geometry(SQUAT))
            pinnedTimerDoesNotOverlapList()
        }
        workout.tapSet(SQUAT, 2)
        workout.verify { restTimerShown("03:00"); assertEquals(expectedGeometry, geometry(SQUAT)) }
        workout.pressFinish()
    }

    @Test
    fun setSaveWithRunningRestKeepsOldProgressAndDeadlineUntilCommit() = runAppUiTest {
        val workout = launchWorkoutScreen(this) { tapSet(SQUAT, 1) }
        val before = assertNotNull(unfinishedWorkout())
        val expectedGeometry = workout.verify {}.geometry(SQUAT)
        advanceTimeBy(10_000)
        val save = pauseNext(UiTestOperation.COMPLETE_SET)
        workout.tapSet(SQUAT, 2, waitForSave = false)
        awaitStarted(save)
        workout.verify {
            mutationsBlocked()
            setCompleted(SQUAT, 1, reps = 5)
            setIncomplete(SQUAT, 2)
            assertEquals(expectedGeometry, geometry(SQUAT))
        }
        assertEquals(before, unfinishedWorkout())
        release(save)
        workout.verify {
            setCompleted(SQUAT, 2, reps = 5)
            restTimerShown("03:00")
            restDeadlineSaved(nowEpochMillis + 180_000)
            assertEquals(expectedGeometry, geometry(SQUAT))
        }
    }

    @Test
    fun restOffSetSaveBlocksConflictsWithoutAddingATimerOrResizingControls() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {}
        workout.editExercise(SQUAT) { selectRestOff(); save() }
        val expectedGeometry = workout.verify {}.geometry(SQUAT)
        val save = pauseNext(UiTestOperation.COMPLETE_SET)
        workout.tapSet(SQUAT, 1, waitForSave = false)
        awaitStarted(save)
        workout.verify { mutationsBlocked(); restTimerAbsent(); assertEquals(expectedGeometry, geometry(SQUAT)) }
        release(save)
        workout.verify {
            setCompleted(SQUAT, 1, reps = 5)
            restTimerAbsent()
            assertEquals(expectedGeometry, geometry(SQUAT))
        }
    }

    @Test
    fun workoutBDeadliftKeepsSingleSetGeometryDuringRestAndExpiry() = runAppUiTest(
        width = 320.dp, height = 640.dp,
    ) {
        val workout = launchWorkoutScreen(this) { switchWorkout(Workout.B) }
        val expectedGeometry = workout.verify {}.geometry(DEADLIFT)
        workout.tapSet(DEADLIFT, 1)
        workout.verify { setCompleted(DEADLIFT, 1, reps = 5); assertEquals(expectedGeometry, geometry(DEADLIFT)) }
        advanceTimeBy(180_000)
        workout.restartApp()
        workout.resumeWorkout()
        workout.verify {
            restTimerShown("00:00", expired = true)
            assertEquals(expectedGeometry, geometry(DEADLIFT))
            pinnedTimerDoesNotOverlapList()
        }
        workout.pressFinish()
    }

    @Test
    fun finalizingBlocksConflictsAndKeepsSavedProgressUntilCompletionIsConfirmed() = runAppUiTest {
        val workout = launchWorkoutScreen(this) { tapSet(SQUAT, 1) }
        val before = assertNotNull(unfinishedWorkout())
        val expectedGeometry = workout.verify {}.geometry(SQUAT)
        val finish = pauseNext(UiTestOperation.FINISH)
        workout.pressFinish(waitForSave = false)
        awaitStarted(finish)
        workout.verify { mutationsBlocked(); finishShowsSaving(); assertEquals(expectedGeometry, geometry(SQUAT)) }
        assertEquals(before, unfinishedWorkout())
        assertNull(savedWorkout())
        release(finish)
        workout.verify { workoutCompleteScreenShown(completedSets = 1) }
    }

    @Test
    fun lostSetReplyRetriesWithoutReducingRepsOrReplacingTheCommittedDeadline() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {}
        failNext(UiTestOperation.COMPLETE_SET, afterCommit = true)
        workout.tapSet(SQUAT, 1, waitForSave = false)
        workout.verify { setSaveErrorShown(); setIncomplete(SQUAT, 1); restTimerAbsent() }
        val committed = assertNotNull(unfinishedWorkout())
        assertEquals(5, committed.exercises.first().setStates.first().completedReps)
        advanceTimeBy(10_000)
        workout.tapSet(SQUAT, 1)
        workout.verify {
            setCompleted(SQUAT, 1, reps = 5)
            restTimerShown("02:50")
            restDeadlineSaved(assertNotNull(committed.restTimer).deadlineEpochMillis)
        }
        assertEquals(committed, unfinishedWorkout())
    }

    @Test
    fun lostRepReductionReplyRetriesTheSameCountInsteadOfDecrementingTwice() = runAppUiTest {
        val workout = launchWorkoutScreen(this) { tapSet(SQUAT, 1) }
        failNext(UiTestOperation.SAVE_REPS, afterCommit = true)
        workout.tapSet(SQUAT, 1, waitForSave = false)
        workout.verify { setSaveErrorShown(); setCompleted(SQUAT, 1, reps = 5) }
        val committed = assertNotNull(unfinishedWorkout())
        assertEquals(4, committed.exercises.first().setStates.first().completedReps)
        workout.tapSet(SQUAT, 1)
        workout.verify { setCompleted(SQUAT, 1, reps = 4) }
        assertEquals(committed, unfinishedWorkout())
    }

    @Test
    fun lostResetReplyRetriesWithoutCompletingTheSetAgain() = runAppUiTest {
        val workout = launchWorkoutScreen(this) { repeat(5) { tapSet(SQUAT, 1) } }
        workout.verify { setCompleted(SQUAT, 1, reps = 1) }
        failNext(UiTestOperation.RESET_SET, afterCommit = true)
        workout.tapSet(SQUAT, 1, waitForSave = false)
        workout.verify { setSaveErrorShown(); setCompleted(SQUAT, 1, reps = 1) }
        val committed = assertNotNull(unfinishedWorkout())
        assertNull(committed.exercises.first().setStates.first().completedReps)
        workout.tapSet(SQUAT, 1)
        workout.verify { setIncomplete(SQUAT, 1) }
        assertEquals(committed, unfinishedWorkout())
    }

    @Test
    fun lostFinishReplyRecoversTheOriginalCompletionWithoutChangingTimestamp() = runAppUiTest {
        val workout = launchWorkoutScreen(this) { tapSet(SQUAT, 1) }
        failNext(UiTestOperation.FINISH, afterCommit = true)
        workout.pressFinish(waitForSave = false)
        workout.verify { finishErrorShown() }
        val committed = assertNotNull(savedWorkout())
        advanceTimeBy(10_000)
        workout.pressFinish()
        workout.verify { workoutCompleteScreenShown(completedSets = 1) }
        assertEquals(committed, savedWorkout())
        assertNull(unfinishedWorkout())
        workout.returnToToday()
        workout.verify { nextWorkoutShown(Workout.B) }
    }
}
