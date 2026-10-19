@file:OptIn(ExperimentalTestApi::class)

package com.hulkdx.core5x5.uitest.screen.history

import androidx.compose.ui.test.*
import com.hulkdx.core5x5.feature.history.navigation.WorkoutDetailKey
import com.hulkdx.core5x5.uitest.AppUiTest

fun restoreWorkoutDetail(app: AppUiTest, workoutId: Long): WorkoutDetailScreenRobot {
    app.restart(destination = WorkoutDetailKey(workoutId), waitForReady = false)
    return WorkoutDetailScreenRobot(app)
}

class WorkoutDetailScreenRobot(private val app: AppUiTest) {
    fun retry() { app.awaitNode(hasText("Try Again")).performScrollTo().performClick() }
    fun backToHistory() {
        app.awaitNode(hasTestTag("workout-back")).performScrollTo()
            .assertContentDescriptionEquals("Back to History").performClick()
        app.awaitNode(hasTestTag("history-list"))
    }
    fun verify(assertions: WorkoutDetailScreenVerify.() -> Unit) = WorkoutDetailScreenVerify(app).apply(assertions)
}

class WorkoutDetailScreenVerify(private val app: AppUiTest) {
    fun loadingShown() { app.awaitNode(hasText("Loading saved workout…")).assertIsDisplayed() }
    fun savedSessionShown() { app.awaitNode(hasText("Saved completed session")).performScrollTo().assertIsDisplayed() }
    fun missingShown() {
        app.awaitNode(hasText("This saved workout is no longer available.")).performScrollTo().assertIsDisplayed()
        savedClaimAbsent()
    }
    fun loadErrorShown() {
        app.awaitNode(hasText("Unable to load this saved workout. Try again.")).performScrollTo().assertIsDisplayed()
        savedClaimAbsent()
    }
    fun savedClaimAbsent() { app.ui.onNodeWithText("Saved completed session").assertDoesNotExist() }
    fun exerciseShown(name: String, prescription: String) {
        app.awaitNode(hasText(name)).performScrollTo().assertIsDisplayed()
        app.awaitNode(hasText(prescription)).performScrollTo().assertIsDisplayed()
    }
}
