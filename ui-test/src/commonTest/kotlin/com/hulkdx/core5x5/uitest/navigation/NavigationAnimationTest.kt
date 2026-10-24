@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.hulkdx.core5x5.uitest.navigation

import androidx.compose.ui.test.*
import com.hulkdx.core5x5.feature.workout.domain.Exercise
import com.hulkdx.core5x5.uitest.AppUiTest
import com.hulkdx.core5x5.uitest.UiTestOperation
import com.hulkdx.core5x5.uitest.runAppUiTest
import com.hulkdx.core5x5.uitest.screen.today.launchTodayScreen
import com.hulkdx.core5x5.uitest.screen.workout.launchWorkoutScreen
import kotlin.test.Test
import kotlin.test.assertEquals

/** Inspect the production host before its 500 ms destination animations have settled. */
class NavigationAnimationTest {
    @Test
    fun startKeepsItsOriginalCardAndBlockedActionDuringThePush() = runAppUiTest {
        val start = pauseNext(UiTestOperation.START)
        launchTodayScreen(this) { startWorkout(waitForStart = false) }
        awaitStarted(start)
        ui.mainClock.autoAdvance = false
        try {
            release(start)
            advanceUntilComposed("workout-title")
            ui.onNodeWithTag("start-workout").assertTextEquals("Start Workout").assertIsNotEnabled()
            ui.onNodeWithTag("resume-workout").assertDoesNotExist()
            ui.onNode(hasTestTag("workout-switch") and hasAnyAncestor(hasTestTag("today"))).assertIsNotEnabled()
        } finally {
            ui.mainClock.autoAdvance = true
        }
    }

    @Test
    fun resumeStaysBlockedAfterItsRequestIsConsumedDuringThePush() = runAppUiTest {
        launchWorkoutScreen(this) { leaveWorkout() }
        ui.mainClock.autoAdvance = false
        try {
            ui.onNodeWithTag("resume-workout").performClick()
            advanceUntilComposed("workout-title")
            ui.onNodeWithTag("resume-workout").assertTextEquals("Resume Workout").assertIsNotEnabled()
            ui.mainClock.advanceTimeBy(100)
            ui.onNodeWithTag("resume-workout").assertIsNotEnabled()
        } finally {
            ui.mainClock.autoAdvance = true
        }
    }

    @Test
    fun finishKeepsRestAndItsVerticalPositionDuringThePush() = runAppUiTest {
        val workout = launchWorkoutScreen(this) { tapSet(Exercise.SQUAT, 1) }
        val verification = pauseNext(UiTestOperation.LOAD_COMPLETE)
        workout.pressFinish(waitForSave = false)
        awaitStarted(verification)
        val finishY = ui.onNodeWithTag("finish-workout").fetchSemanticsNode().positionInRoot.y
        ui.mainClock.autoAdvance = false
        try {
            release(verification)
            advanceUntilComposed("back-to-today")
            ui.onNodeWithTag("rest-timer").assertExists()
            ui.onNodeWithTag("finish-workout").assertTextEquals("Saving…").assertIsNotEnabled()
            assertEquals(finishY, ui.onNodeWithTag("finish-workout").fetchSemanticsNode().positionInRoot.y)
        } finally {
            ui.mainClock.autoAdvance = true
        }
    }

    @Test
    fun returningAfterCompletionNeverRevealsTheOldResumeCardDuringASlowRefresh() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {}
        val refresh = pauseNext(UiTestOperation.LOAD_ACTIVE)
        workout.pressFinish()
        awaitStarted(refresh)
        ui.mainClock.autoAdvance = false
        try {
            ui.onNodeWithTag("back-to-today").performClick()
            advanceUntilComposed("today")
            ui.onNodeWithTag("resume-workout").assertDoesNotExist()
            ui.onNodeWithTag("start-workout").assertDoesNotExist()
            release(refresh)
            advanceUntilComposed("start-workout")
            ui.onNodeWithTag("today-workout").assertTextEquals("Workout B")
            ui.onNodeWithTag("resume-workout").assertDoesNotExist()
        } finally {
            ui.mainClock.autoAdvance = true
        }
    }

    @Test
    fun rapidTabChangesReplaceContentWithoutSlidingDuplicateBars() = runAppUiTest {
        val barBounds = ui.onNodeWithTag("tab:TODAY").fetchSemanticsNode().boundsInRoot
        ui.mainClock.autoAdvance = false
        try {
            repeat(2) {
                listOf("HISTORY" to "history-list", "SETTINGS" to "settings", "TODAY" to "today").forEach { (tab, screen) ->
                    ui.onNodeWithTag("tab:$tab").performClick()
                    ui.mainClock.advanceTimeByFrame()
                    ui.mainClock.advanceTimeByFrame()
                    // Even a zero-duration transition needs render frames to dispose the old
                    // scene. Both bars must stay stationary throughout that bounded cleanup.
                    repeat(3) {
                        val bars = ui.onAllNodesWithTag("tab:TODAY").fetchSemanticsNodes()
                        bars.forEach { assertEquals(barBounds, it.boundsInRoot) }
                        ui.mainClock.advanceTimeByFrame()
                    }
                    ui.onNodeWithTag(screen).assertIsDisplayed()
                    ui.onAllNodesWithTag("tab:TODAY").assertCountEquals(1)
                    ui.onAllNodesWithTag("tab:HISTORY").assertCountEquals(1)
                    ui.onAllNodesWithTag("tab:SETTINGS").assertCountEquals(1)
                }
            }
        } finally {
            ui.mainClock.autoAdvance = true
        }
    }

    private fun AppUiTest.advanceUntilComposed(tag: String) {
        ui.waitUntil(timeoutMillis = 5_000) {
            ui.mainClock.advanceTimeByFrame()
            ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
