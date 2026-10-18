@file:OptIn(ExperimentalTestApi::class)

package com.hulkdx.core5x5.uitest.screen.history

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
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
    fun selectLift(exercise: CompletedExerciseType) {
        app.ui.onNodeWithTag("history-list").performScrollToNode(hasTestTag("history-lift-selector"))
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
        app.ui.onNodeWithTag("history-list").performScrollToNode(hasTestTag("history-lift-selector"))
        app.awaitNode(hasTestTag("history-lift-selector")).assertTextContains("Lift: $name")
    }
    fun loggedSquatWeightShown(weight: String) {
        app.ui.onNodeWithTag("history-list").performScrollToNode(hasTestTag("history-weight-plot"))
        app.awaitNode(hasTestTag("history-weight-plot")).assertIsDisplayed()
            .assertContentDescriptionContains("Squat weight history.", substring = true)
            .assertContentDescriptionContains(weight, substring = true)
    }
}
