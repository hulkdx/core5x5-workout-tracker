@file:OptIn(ExperimentalTestApi::class)

package com.hulkdx.core5x5.uitest.screen.workout

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.IntSize
import com.hulkdx.core5x5.feature.workout.domain.Exercise
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.feature.workout.presentation.displayName
import com.hulkdx.core5x5.uitest.AppUiTest
import com.hulkdx.core5x5.uitest.screen.edit.ExerciseEditScreenRobot
import com.hulkdx.core5x5.uitest.screen.edit.launchExerciseEditor
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

fun launchWorkoutScreen(app: AppUiTest, actions: WorkoutScreenRobot.() -> Unit): WorkoutScreenRobot {
    app.awaitNode(hasTestTag("start-workout") and isEnabled()).performScrollTo().performClick()
    app.awaitNode(hasTestTag("workout-title"))
    return WorkoutScreenRobot(app).apply(actions)
}

class WorkoutScreenRobot(private val app: AppUiTest) {

    fun tapSet(exercise: Exercise, set: Int, waitForSave: Boolean = true, scroll: Boolean = true) {
        val target = setMatcher(exercise, set)
        val node = if (scroll) {
            app.scrollToNode("active-workout-list", target and isEnabled(), hasTestTag("exercise:${exercise.name}"))
                .performScrollTo()
        } else app.awaitNode(target and isEnabled())
        val before = node.fetchSemanticsNode().config
        val beforeState = before[SemanticsProperties.StateDescription]
        val beforeDescription = before[SemanticsProperties.ContentDescription]
        node.performClick()
        val changed = SemanticsMatcher("set value changed") {
            it.config[SemanticsProperties.StateDescription] != beforeState ||
                it.config[SemanticsProperties.ContentDescription] != beforeDescription
        }
        if (waitForSave) {
            val saved = target and isEnabled() and changed
            // A newly pinned timer can move a lower lazy item out of composition.
            if (scroll) {
                app.scrollToNode("active-workout-list", saved, hasTestTag("exercise:${exercise.name}"))
            } else app.awaitNode(saved)
        }
    }

    fun completeAllSets(workout: Workout = Workout.A) {
        for (exercise in workout.exercises) {
            for (set in 1..exercise.sets) tapSet(exercise, set)
        }
    }

    fun leaveWorkout() {
        app.ui.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("workout-back"))
        app.awaitNode(hasTestTag("workout-back") and isEnabled()).performClick()
        app.awaitNode(hasTestTag("resume-workout") and isEnabled())
    }

    fun resumeWorkout() {
        app.awaitNode(hasTestTag("resume-workout") and isEnabled()).performScrollTo().performClick()
        app.awaitNode(hasTestTag("workout-title"))
    }

    fun pressFinish(waitForSave: Boolean = true) {
        app.ui.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("finish-workout"))
        app.awaitNode(hasTestTag("finish-workout") and isEnabled()).performClick()
        if (waitForSave) app.awaitNode(hasTestTag("back-to-today"))
    }

    fun returnToToday() {
        app.awaitNode(hasTestTag("back-to-today")).performScrollTo().performClick()
        app.awaitNode(hasTestTag("start-workout") and isEnabled())
    }

    fun restartApp() = app.restart()

    fun switchWorkout(target: Workout) {
        app.ui.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("workout-switch"))
        app.awaitNode(hasTestTag("workout-switch") and isEnabled()).performClick()
        app.awaitNode(hasTestTag("workout-title") and hasText("Workout ${target.name}"))
    }

    fun pressSwitchExpectingFailure() {
        app.ui.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("workout-switch"))
        app.awaitNode(hasTestTag("workout-switch") and isEnabled()).performClick()
    }

    fun editExercise(exercise: Exercise, actions: ExerciseEditScreenRobot.() -> Unit) =
        launchExerciseEditor(app, exercise, actions)

    fun advanceTimeBy(millis: Long) = app.advanceTimeBy(millis)

    fun verify(assertions: WorkoutScreenVerify.() -> Unit) = WorkoutScreenVerify(app).apply(assertions)
}

class WorkoutScreenVerify(private val app: AppUiTest) {
    fun mutationsBlocked(switchCanRetry: Boolean = false) {
        app.ui.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("workout-switch"))
        val switch = app.awaitNode(hasTestTag("workout-switch"))
        if (switchCanRetry) switch.assertIsEnabled() else switch.assertIsNotEnabled()
        app.ui.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("exercise:SQUAT"))
        app.awaitNode(setMatcher(Exercise.SQUAT, 1)).assertIsNotEnabled()
        app.awaitNode(hasTestTag("edit-exercise") and hasAnyAncestor(hasTestTag("exercise:SQUAT")))
            .assertIsNotEnabled()
        app.ui.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("finish-workout"))
        app.awaitNode(hasTestTag("finish-workout")).assertIsNotEnabled()
    }

    fun mutationsAvailable() {
        app.ui.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("workout-switch"))
        app.awaitNode(hasTestTag("workout-switch") and isEnabled()).assertIsEnabled()
        app.ui.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("exercise:SQUAT"))
        app.awaitNode(setMatcher(Exercise.SQUAT, 1) and isEnabled()).assertIsEnabled()
        app.awaitNode(hasTestTag("edit-exercise") and hasAnyAncestor(hasTestTag("exercise:SQUAT")))
            .assertIsEnabled()
        app.ui.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("finish-workout"))
        app.awaitNode(hasTestTag("finish-workout") and isEnabled()).assertIsEnabled()
    }

    fun finishShowsSaving() {
        app.awaitNode(hasTestTag("finish-workout")).assertTextEquals("Saving…").assertIsNotEnabled()
    }

    fun geometry(exercise: Exercise): ExerciseGeometry {
        val card = hasTestTag("exercise:${exercise.name}")
        app.ui.onNodeWithTag("active-workout-list").performScrollToNode(card)
        return ExerciseGeometry(
            app.awaitNode(card).fetchSemanticsNode().size,
            app.awaitNode(setMatcher(exercise, 1)).fetchSemanticsNode().size,
        )
    }

    // Do not scroll before this assertion: that would hide an unwanted anchor shift.
    fun exerciseTop(exercise: Exercise): Float =
        app.awaitNode(hasTestTag("exercise:${exercise.name}")).fetchSemanticsNode().positionInRoot.y

    fun pinnedTimerDoesNotOverlapList() {
        val list = app.ui.onNodeWithTag("active-workout-list").fetchSemanticsNode().boundsInRoot
        val timer = app.awaitNode(hasTestTag("rest-timer")).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue(list.bottom <= timer.top, "Pinned rest must reserve space below scrolling controls")
    }

    fun workoutShown(workout: Workout) {
        app.awaitNode(hasTestTag("workout-title") and hasText("Workout ${workout.name}"))
            .assertTextEquals("Workout ${workout.name}").assertIsDisplayed()
    }

    fun setCompleted(exercise: Exercise, set: Int, reps: Int, name: String = exercise.displayName()) {
        app.ui.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("exercise:${exercise.name}"))
        val completed = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Completed")
        app.awaitNode(setMatcher(exercise, set) and completed).performScrollTo().assertIsDisplayed()
            .assertContentDescriptionEquals("$name, set $set, $reps reps")
    }

    fun workoutCompleteScreenShown(completedSets: Int, duration: String? = null, prescribedSets: Int = 15) {
        app.awaitNode(hasTestTag("back-to-today"))
        val inSummary = hasAnyAncestor(hasTestTag("workout-complete"))
        app.awaitNode(hasText("$completedSets / $prescribedSets") and inSummary).performScrollTo().assertIsDisplayed()
        if (duration != null) app.awaitNode(hasText(duration) and inSummary).performScrollTo().assertIsDisplayed()
    }

    fun nextWorkoutShown(workout: Workout) {
        app.awaitNode(hasTestTag("today-workout") and hasText("Workout ${workout.name}")).assertIsDisplayed()
        app.ui.onNodeWithTag("resume-workout").assertDoesNotExist()
    }

    fun setIncomplete(exercise: Exercise, set: Int) {
        app.ui.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("exercise:${exercise.name}"))
        val incomplete = SemanticsMatcher("set is incomplete") {
            it.config[SemanticsProperties.StateDescription] != "Completed"
        }
        app.awaitNode(setMatcher(exercise, set) and incomplete).performScrollTo().assertIsDisplayed()
    }

    fun nextSetHighlighted(exercise: Exercise, set: Int) {
        app.ui.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("exercise:${exercise.name}"))
        val next = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Next set")
        app.awaitNode(setMatcher(exercise, set) and next).performScrollTo().assertIsDisplayed()
    }

    fun setsStayOnOneHorizontallyScrollableRow(exercise: Exercise, count: Int) {
        app.ui.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("exercise:${exercise.name}"))
        val row = app.awaitNode(hasTestTag("set-row") and hasAnyAncestor(hasTestTag("exercise:${exercise.name}")))
        row.assert(hasScrollAction())
        val first = app.awaitNode(setMatcher(exercise, 1)).fetchSemanticsNode()
        val last = app.awaitNode(setMatcher(exercise, count)).fetchSemanticsNode()
        assertEquals(first.positionInRoot.y, last.positionInRoot.y, "Sets must stay on the same row")
        assertTrue(row.fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange].maxValue() > 0f)
        row.performTouchInput { swipeLeft() }
        app.awaitNode(setMatcher(exercise, count)).assertIsDisplayed()
    }

    fun prescriptionShown(exercise: Exercise, text: String) {
        app.awaitNode(hasTestTag("active-workout-list")).performScrollToNode(hasTestTag("exercise:${exercise.name}"))
        val prescription = hasTestTag("exercise-prescription") and hasAnyAncestor(hasTestTag("exercise:${exercise.name}"))
        app.awaitNode(prescription).assertTextEquals(text).assertIsDisplayed()
    }

    fun restTimerShown(countdown: String, expired: Boolean = false) {
        val label = if (expired) "Rest complete" else "Rest"
        app.awaitNode(hasTestTag("rest-timer") and hasContentDescription("$label, $countdown")).assertIsDisplayed()
    }

    fun restTimerAbsent() = app.ui.onNodeWithTag("rest-timer").assertDoesNotExist()

    fun restDeadlineSaved(deadline: Long?) {
        assertEquals(deadline, app.unfinishedWorkout()?.restTimer?.deadlineEpochMillis)
    }

    fun bottomNavigationHidden() = app.ui.onNodeWithTag("tab:HISTORY").assertDoesNotExist()

    fun setSaveErrorShown() { app.scrollToNode("active-workout-list", hasText("Unable to save the set. Tap it again to retry.")).assertIsDisplayed() }
    fun finishErrorShown() { app.scrollToNode("active-workout-list", hasText("Unable to confirm the workout was saved. Try finishing again.")).assertIsDisplayed() }
    fun switchErrorShown() {
        app.scrollToNode("active-workout-list", hasText("Unable to confirm the workout switch. Tap Switch to retry before logging sets.")).assertIsDisplayed()
    }

    fun exerciseNameShown(exercise: Exercise, name: String) {
        app.ui.onNodeWithTag("active-workout-list").performScrollToNode(hasTestTag("exercise:${exercise.name}"))
        val target = hasTestTag("exercise-name") and hasAnyAncestor(hasTestTag("exercise:${exercise.name}"))
        app.awaitNode(target).assertTextEquals(name)
    }

    fun savedWeightKg(exercise: Exercise, expected: Double) {
        assertEquals(expected, app.unfinishedWorkout()?.exercises?.single { it.exercise == exercise }?.weightKg)
    }

    fun allSetsSavedWithFiveReps() {
        val saved = assertNotNull(app.savedWorkout())
        assertNull(app.unfinishedWorkout())
        assertEquals(Workout.A, saved.workout)
        assertEquals(15, saved.completedSets)
        assertEquals(60_000L, saved.durationMillis)
        assertTrue(saved.exercises.all { it.weightKg == 20.0 })
        assertTrue(saved.exercises.flatMap { it.setStates }.all { it.isCompleted && it.completedReps == 5 })
    }
}

data class ExerciseGeometry(val card: IntSize, val set: IntSize)

private fun setMatcher(exercise: Exercise, set: Int) =
    hasTestTag("set:${set - 1}") and hasAnyAncestor(hasTestTag("exercise:${exercise.name}"))
