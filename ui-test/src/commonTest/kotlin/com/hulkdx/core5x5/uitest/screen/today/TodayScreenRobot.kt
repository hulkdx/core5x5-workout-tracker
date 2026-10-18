@file:OptIn(ExperimentalTestApi::class)

package com.hulkdx.core5x5.uitest.screen.today

import androidx.compose.ui.test.*
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.uitest.AppUiTest
import kotlin.test.assertNull

fun launchTodayScreen(app: AppUiTest, actions: TodayScreenRobot.() -> Unit) = TodayScreenRobot(app).apply(actions)

class TodayScreenRobot(private val app: AppUiTest) {
    fun switchWorkout(target: Workout) {
        app.awaitNode(hasTestTag("workout-switch") and isEnabled()).performScrollTo().performClick()
        app.awaitNode(hasTestTag("today-workout") and hasText("Workout ${target.name}"))
    }
    fun startWorkout() {
        app.awaitNode(hasTestTag("start-workout") and isEnabled()).performScrollTo().performClick()
        app.awaitNode(hasTestTag("workout-title"))
    }
    fun verify(assertions: TodayScreenVerify.() -> Unit) = TodayScreenVerify(app).apply(assertions)
}

class TodayScreenVerify(private val app: AppUiTest) {
    fun nextWorkoutShown(workout: Workout) {
        app.awaitNode(hasTestTag("today-workout") and hasText("Workout ${workout.name}")).assertIsDisplayed()
    }
    fun activeWorkoutShown(workout: Workout) {
        app.awaitNode(hasTestTag("workout-title")).assertTextEquals("Workout ${workout.name}")
    }
    fun prescriptionsShown(text: String) = app.ui.onAllNodesWithText(text).assertCountEquals(3)
    fun noSessionCreated() = assertNull(app.unfinishedWorkout())
    fun resumeOnlyShown() {
        app.awaitNode(hasTestTag("resume-workout") and isEnabled()).assertIsDisplayed()
        app.ui.onNodeWithTag("start-workout").assertDoesNotExist()
        app.ui.onNodeWithTag("workout-switch").assertDoesNotExist()
    }
}
