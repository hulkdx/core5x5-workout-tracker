@file:OptIn(ExperimentalTestApi::class)

package com.hulkdx.core5x5.uitest.screen.edit

import androidx.compose.ui.test.*
import com.hulkdx.core5x5.feature.workout.domain.Exercise
import com.hulkdx.core5x5.uitest.AppUiTest

fun launchExerciseEditor(app: AppUiTest, exercise: Exercise, actions: ExerciseEditScreenRobot.() -> Unit): ExerciseEditScreenRobot {
    val card = hasTestTag("exercise:${exercise.name}")
    app.ui.onNodeWithTag("active-workout-list").performScrollToNode(card)
    app.awaitNode(hasTestTag("edit-exercise") and hasAnyAncestor(card) and isEnabled()).performClick()
    app.awaitNode(hasTestTag("exercise-editor"))
    return ExerciseEditScreenRobot(app).apply(actions)
}

class ExerciseEditScreenRobot(private val app: AppUiTest) {
    fun increaseWeight() = step("weight", "increase")
    fun increaseSets() = step("sets", "increase")
    fun decreaseSets() = step("sets", "decrease")
    fun increaseReps() = step("reps", "increase")
    private fun step(field: String, direction: String) {
        app.awaitNode(hasTestTag("edit:$field:$direction") and isEnabled()).performScrollTo().performClick()
    }
    fun selectRestOff() { app.awaitNode(hasTestTag("edit-rest:0")).performScrollTo().performClick() }
    fun openCustomRest() {
        app.awaitNode(hasTestTag("edit-rest:2")).performScrollTo().performClick()
        app.awaitNode(hasTestTag("custom-rest"))
    }
    fun chooseCustomRest(minutes: Int, seconds: Int) {
        for ((label, value) in listOf("Minutes" to minutes, "Seconds" to seconds)) {
            app.ui.onNodeWithTag("duration-wheel:$label").performScrollToIndex(value)
            app.awaitNode(hasTestTag("duration-value:$label:$value")).performClick()
        }
    }
    fun applyCustomRest() {
        app.awaitNode(hasTestTag("custom-rest-apply") and isEnabled()).performScrollTo().performClick()
        app.awaitNode(hasTestTag("edit-save") and isEnabled())
    }
    fun cancelCustomRest() {
        app.awaitNode(hasTestTag("custom-rest-cancel")).performScrollTo().performClick()
        app.awaitNode(hasTestTag("edit-save") and isEnabled())
    }
    fun save(waitForSave: Boolean = true) {
        app.awaitNode(hasTestTag("edit-save") and isEnabled()).performClick()
        if (waitForSave) app.awaitNode(hasTestTag("active-workout"))
    }
    fun cancel() {
        app.awaitNode(hasTestTag("edit-cancel")).performClick()
        app.awaitNode(hasTestTag("active-workout"))
    }
    fun verify(assertions: ExerciseEditScreenVerify.() -> Unit) = ExerciseEditScreenVerify(app).apply(assertions)
}

class ExerciseEditScreenVerify(private val app: AppUiTest) {
    fun weightShown(weight: String) = app.awaitNode(hasTestTag("edit:weight:value")).assertTextEquals(weight)
    fun setsShown(sets: Int) = app.awaitNode(hasTestTag("edit:sets:value")).assertTextEquals(sets.toString())
    fun restOffChosen() {
        app.ui.onNodeWithTag("edit-rest:0").performScrollTo().assertIsSelected()
    }
    fun saveErrorShown() {
        app.awaitNode(hasText("Unable to save changes. Try again.")).performScrollTo().assertIsDisplayed()
    }
    fun nameEditingUnavailable() {
        app.ui.onNodeWithTag("edit-name").assertDoesNotExist()
        app.ui.onNodeWithText("Exercise Name").assertDoesNotExist()
        app.awaitNode(hasText("Edit Squat")).assertIsDisplayed()
        app.ui.onNodeWithTag("edit-save").assertIsEnabled()
    }
}
