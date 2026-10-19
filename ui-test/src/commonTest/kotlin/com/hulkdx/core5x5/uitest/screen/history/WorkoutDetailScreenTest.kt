package com.hulkdx.core5x5.uitest.screen.history

import androidx.compose.ui.unit.dp
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.feature.workout.domain.Exercise.SQUAT
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.uitest.UiTestOperation
import com.hulkdx.core5x5.uitest.runAppUiTest
import com.hulkdx.core5x5.uitest.screen.settings.launchSettingsScreen
import com.hulkdx.core5x5.uitest.screen.workout.launchWorkoutScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WorkoutDetailScreenTest {
    @Test
    fun savedCustomAndResetRepsSurviveReopeningAndShowInNarrowLargeTextDetail() = runAppUiTest(
        width = 320.dp, height = 640.dp, fontScale = 2f,
    ) {
        launchSettingsScreen(this) { openUnits(); chooseUnit(WeightUnit.LB); returnToToday() }
        val workout = launchWorkoutScreen(this) {}
        val legacyName = "Paused Front Squat with a deliberately long legacy name"
        seedLegacyName(SQUAT, legacyName)
        workout.restartApp()
        workout.resumeWorkout()
        workout.tapSet(SQUAT, 1)
        workout.tapSet(SQUAT, 1) // Four actual reps, independently of the later two-rep target.
        workout.tapSet(SQUAT, 2)
        repeat(5) { workout.tapSet(SQUAT, 2) } // Reset clears the recorded count.
        workout.editExercise(SQUAT) {
            repeat(3) { decreaseReps() }
            decreaseSets()
            save()
        }
        workout.tapSet(SQUAT, 3)
        workout.pressFinish()
        workout.verify { workoutCompleteScreenShown(completedSets = 2, prescribedSets = 14) }
        val saved = assertNotNull(savedWorkout())
        val detail = restoreWorkoutDetail(this, saved.id)
        detail.verify {
            savedSessionShown()
            exerciseShown(legacyName, "4 × 2 · 40 lb · 2 / 4 sets complete · Reps: 4 / – / 2 / –")
        }
        assertEquals(saved, savedWorkout(), "Detail must not mutate the saved session")
        assertNull(unfinishedWorkout())
        detail.backToHistory()
        launchHistoryScreen(this) {}.verify {
            savedWorkoutShown(Workout.A, prescribedSets = 14)
            loggedSquatWeightShown("40 lb")
        }
    }

    @Test
    fun detailLoadingDoesNotClaimSuccessBeforeTheIdentifiedRecordArrives() = runAppUiTest {
        launchWorkoutScreen(this) { tapSet(SQUAT, 1); pressFinish() }
        val saved = assertNotNull(savedWorkout())
        val read = pauseNext(UiTestOperation.LOAD_DETAIL)
        val detail = restoreWorkoutDetail(this, saved.id)
        awaitStarted(read)
        detail.verify { loadingShown(); savedClaimAbsent() }
        release(read)
        detail.verify { savedSessionShown() }
    }

    @Test
    fun detailReadFailureCanRetryWithoutChangingSavedData() = runAppUiTest {
        launchWorkoutScreen(this) { tapSet(SQUAT, 1); pressFinish() }
        val saved = assertNotNull(savedWorkout())
        failNext(UiTestOperation.LOAD_DETAIL)
        val detail = restoreWorkoutDetail(this, saved.id)
        detail.verify { loadErrorShown() }
        detail.retry()
        detail.verify { savedSessionShown() }
        assertEquals(saved, savedWorkout())
    }

    @Test
    fun missingDetailDoesNotSubstituteAnotherCompletedWorkout() = runAppUiTest {
        launchWorkoutScreen(this) { pressFinish() }
        val saved = assertNotNull(savedWorkout())
        val detail = restoreWorkoutDetail(this, saved.id + 1)
        detail.verify { missingShown() }
        detail.retry()
        detail.verify { missingShown() }
        assertEquals(saved, savedWorkout())
        detail.backToHistory()
    }

    @Test
    fun unfinishedDetailCannotFinalizeTheActiveSession() = runAppUiTest {
        launchWorkoutScreen(this) { tapSet(SQUAT, 1) }
        val active = assertNotNull(unfinishedWorkout())
        val detail = restoreWorkoutDetail(this, active.id)
        detail.verify { missingShown() }
        detail.retry()
        detail.verify { missingShown() }
        assertEquals(active, unfinishedWorkout())
        assertNull(savedWorkout())
    }
}
