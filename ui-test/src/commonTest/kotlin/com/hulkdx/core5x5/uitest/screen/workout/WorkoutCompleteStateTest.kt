@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.hulkdx.core5x5.uitest.screen.workout

import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.hulkdx.core5x5.feature.workout.domain.Exercise.SQUAT
import com.hulkdx.core5x5.feature.workout.navigation.WorkoutCompleteKey
import com.hulkdx.core5x5.uitest.AppUiTest
import com.hulkdx.core5x5.uitest.UiTestOperation
import com.hulkdx.core5x5.uitest.runAppUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WorkoutCompleteStateTest {
    @Test
    fun loadingSummaryDoesNotClaimSuccessAndReturnRemainsReachableAtLargeText() = runAppUiTest(
        width = 320.dp, height = 640.dp, fontScale = 2f,
    ) {
        val workout = launchWorkoutScreen(this) { tapSet(SQUAT, 1); pressFinish() }
        val saved = assertNotNull(savedWorkout())
        val read = pauseNext(UiTestOperation.LOAD_COMPLETE)
        restart(destination = WorkoutCompleteKey(saved.id), waitForReady = false)
        awaitStarted(read)
        awaitNode(hasText("Loading saved workout…")).performScrollTo().assertIsDisplayed()
        assertNoSavedClaim()
        awaitNode(hasText("Back to Today")).performScrollTo().assertIsDisplayed()
        release(read)
        workout.verify { workoutCompleteScreenShown(completedSets = 1) }
        workout.returnToToday()
    }

    @Test
    fun summaryReadFailureRetriesTheSameSavedSession() = runAppUiTest {
        val workout = launchWorkoutScreen(this) { tapSet(SQUAT, 1); pressFinish() }
        val saved = assertNotNull(savedWorkout())
        failNext(UiTestOperation.LOAD_COMPLETE)
        restart(destination = WorkoutCompleteKey(saved.id), waitForReady = false)
        awaitNode(hasText("Unable to load the saved workout. Try again.")).assertIsDisplayed()
        assertNoSavedClaim()
        awaitNode(hasText("Try Again")).performScrollTo().performClick()
        workout.verify { workoutCompleteScreenShown(completedSets = 1) }
        assertEquals(saved, savedWorkout())
    }

    @Test
    fun missingSummaryDoesNotSubstituteAnotherSavedSession() = runAppUiTest {
        launchWorkoutScreen(this) { pressFinish() }
        val saved = assertNotNull(savedWorkout())
        restart(destination = WorkoutCompleteKey(saved.id + 1), waitForReady = false)
        awaitNode(hasText("No saved completed workout was found.")).assertIsDisplayed()
        assertNoSavedClaim()
        awaitNode(hasText("Try Again")).performScrollTo().performClick()
        awaitNode(hasText("No saved completed workout was found.")).assertIsDisplayed()
        assertEquals(saved, savedWorkout())
        awaitNode(hasText("Back to Today")).performScrollTo().performClick()
        awaitNode(hasTestTag("start-workout") and isEnabled())
    }

    @Test
    fun unfinishedSummaryCannotFinalizeTheActiveSession() = runAppUiTest {
        launchWorkoutScreen(this) { tapSet(SQUAT, 1) }
        val active = assertNotNull(unfinishedWorkout())
        restart(destination = WorkoutCompleteKey(active.id), waitForReady = false)
        awaitNode(hasText("No saved completed workout was found.")).assertIsDisplayed()
        assertNoSavedClaim()
        awaitNode(hasText("Try Again")).performScrollTo().performClick()
        awaitNode(hasText("No saved completed workout was found.")).assertIsDisplayed()
        assertEquals(active, unfinishedWorkout())
        assertNull(savedWorkout())
        awaitNode(hasText("Back to Today")).performScrollTo().performClick()
        awaitNode(hasTestTag("resume-workout") and isEnabled())
    }
}

private fun AppUiTest.assertNoSavedClaim() {
    ui.onNodeWithText("Workout Complete").assertDoesNotExist()
    ui.onNodeWithText("Workout saved. Your next workout is ready.").assertDoesNotExist()
}
