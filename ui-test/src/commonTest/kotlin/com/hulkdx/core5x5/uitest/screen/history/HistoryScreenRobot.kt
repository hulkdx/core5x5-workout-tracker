@file:OptIn(ExperimentalTestApi::class)

package com.hulkdx.core5x5.uitest.screen.history

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import kotlin.test.assertTrue
import androidx.compose.ui.test.click
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.hulkdx.core5x5.core.training.domain.CompletedExerciseType
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.uitest.AppUiTest

fun launchHistoryScreen(app: AppUiTest, actions: HistoryScreenRobot.() -> Unit): HistoryScreenRobot {
    app.awaitNode(hasTestTag("tab:HISTORY")).performClick()
    app.awaitNode(hasTestTag("history-list"))
    return HistoryScreenRobot(app).apply(actions)
}

class HistoryScreenRobot(private val app: AppUiTest) {
    fun scrollWithVerticalSwipe() {
        val list = app.awaitNode(hasTestTag("history-list"))
        val before = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        list.performTouchInput { swipeUp() }
        val after = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        assertTrue(after > before, "A vertical swipe must scroll History")
    }

    fun revealDelete(id: Long = 1) {
        app.scrollToNode("history-list", hasTestTag("history-workout:$id"))
        app.awaitNode(hasTestTag("history-workout:$id")).performTouchInput { swipeLeft() }
        app.awaitNode(hasTestTag("history-swipe-delete:$id") and isEnabled())
    }

    fun revealDeleteWithAccessibility(id: Long = 1) {
        app.scrollToNode("history-list", hasTestTag("history-workout:$id"))
        val actions = app.awaitNode(hasTestTag("history-workout:$id"))
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertTrue(app.ui.runOnIdle { actions.single { it.label == "Show Delete" }.action() })
        app.awaitNode(hasTestTag("history-swipe-delete:$id") and isEnabled())
    }

    fun hideDeleteWithAccessibility(id: Long = 1) {
        val actions = app.awaitNode(hasTestTag("history-workout:$id"))
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertTrue(app.ui.runOnIdle { actions.single { it.label == "Hide Delete" }.action() })
    }

    fun closeDeleteWithSwipe(id: Long = 1) {
        app.awaitNode(hasTestTag("history-workout:$id")).performTouchInput { swipeRight() }
    }

    fun tapWorkout(id: Long = 1) {
        app.scrollToNode("history-list", hasTestTag("history-workout:$id"))
        app.awaitNode(hasTestTag("history-workout:$id")).performTouchInput { click() }
    }

    fun tapRevealedDelete(id: Long = 1, waitForSave: Boolean = true) {
        app.awaitNode(hasTestTag("history-swipe-delete:$id") and isEnabled()).performClick()
        if (waitForSave) awaitDeleted(id)
    }

    fun deleteWorkout(id: Long = 1, waitForSave: Boolean = true) {
        revealDelete(id)
        tapRevealedDelete(id, waitForSave)
    }

    fun retryDeletion(id: Long = 1) {
        app.scrollToNode("history-list", hasTestTag("history-retry-delete:$id"))
        app.awaitNode(hasTestTag("history-retry-delete:$id") and isEnabled()).performClick()
        awaitDeleted(id)
    }

    private fun awaitDeleted(id: Long) {
        app.ui.waitUntil(timeoutMillis = 5_000) {
            app.ui.onAllNodesWithTag("history-workout:$id").fetchSemanticsNodes().isEmpty()
        }
    }

    fun openGraph() {
        app.scrollToNode("history-list", hasTestTag("history-open-graph"))
        app.awaitNode(hasTestTag("history-open-graph")).performClick()
        app.awaitNode(hasTestTag("history-graph-screen"))
    }

    fun returnFromGraph() {
        app.awaitNode(hasTestTag("workout-back")).performClick()
        app.awaitNode(hasTestTag("history-list"))
    }

    fun selectLift(exercise: CompletedExerciseType) {
        app.awaitNode(hasTestTag("history-graph-screen"))
        app.awaitNode(hasTestTag("history-lift-selector")).performClick()
        app.awaitNode(hasTestTag("history-lift:${exercise.name}")).performClick()
    }
    fun visitSettingsAndReturn() {
        app.awaitNode(hasTestTag("tab:SETTINGS")).performClick()
        app.awaitNode(hasTestTag("settings"))
        app.awaitNode(hasTestTag("tab:HISTORY")).performClick()
        app.awaitNode(hasTestTag("history-list"))
    }

    fun verify(assertions: HistoryScreenVerify.() -> Unit) = HistoryScreenVerify(app).apply(assertions)
}

class HistoryScreenVerify(private val app: AppUiTest) {
    fun deleteRevealState(id: Long, revealed: Boolean) {
        app.scrollToNode("history-list", hasTestTag("history-workout:$id"))
        app.awaitNode(hasTestTag("history-workout:$id")).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,
                if (revealed) "Delete revealed" else "Delete hidden"),
        )
    }

    fun deletionErrorShown() {
        app.scrollToNode("history-list", hasText("Unable to verify deletion. Try again."))
        app.awaitNode(hasText("Unable to verify deletion. Try again.")).assertIsDisplayed()
    }
    fun noGraphShown() {
        assertTrue(app.ui.onAllNodesWithTag("history-weight-plot").fetchSemanticsNodes().isEmpty())
    }
    fun selectedPointShown(weight: String, date: String) {
        app.awaitNode(hasTestTag("history-selected-point"))
            .assertTextContains("Selected: $weight · $date")
    }
    fun selectNextGraphPoint() {
        app.awaitNode(hasTestTag("history-weight-plot")).performClick()
    }

    fun savedWorkoutShown(workout: Workout, id: Long = 1, prescribedSets: Int = 15) {
        app.scrollToNode("history-list", hasTestTag("history-workout:$id"))
        app.awaitNode(hasTestTag("history-workout:$id")).assertIsDisplayed()
            .assertTextContains("Workout ${workout.name}")
            .assertTextContains("3 exercises  ·  $prescribedSets sets")
    }

    fun emptyHistoryShown() {
        app.awaitNode(hasText("No completed workouts yet. Finish a workout to see it here.")).assertIsDisplayed()
    }
    fun selectedLiftShown(name: String) {
        app.awaitNode(hasTestTag("history-graph-screen"))
        app.awaitNode(hasTestTag("history-lift-selector")).assertTextContains("Lift: $name")
    }
    fun latestWeightShown(weight: String) {
        app.awaitNode(hasTestTag("history-graph-screen"))
        app.awaitNode(hasText("Latest: $weight")).assertIsDisplayed()
    }
    fun loggedSquatWeightShown(weight: String) {
        app.awaitNode(hasTestTag("history-graph-screen"))
        app.awaitNode(hasTestTag("history-weight-plot")).assertIsDisplayed()
            .assertContentDescriptionContains("Squat weight history.", substring = true)
            .assertContentDescriptionContains(weight, substring = true)
    }
}
