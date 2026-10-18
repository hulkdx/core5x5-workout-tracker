package com.hulkdx.core5x5.uitest.screen.edit

import androidx.compose.ui.unit.dp
import com.hulkdx.core5x5.feature.workout.domain.Exercise.*
import com.hulkdx.core5x5.uitest.runAppUiTest
import com.hulkdx.core5x5.uitest.screen.workout.launchWorkoutScreen
import kotlin.test.Test

class ExerciseEditScreenTest {
    @Test
    fun savedExercisePrescriptionSurvivesReopening() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {}
        workout.editExercise(SQUAT) {
            increaseWeight()
            increaseReps()
            decreaseSets()
            save()
        }
        workout.restartApp()
        workout.resumeWorkout()
        workout.verify {
            exerciseNameShown(SQUAT, "Squat")
            prescriptionShown(SQUAT, "4 × 6 · 22.5 kg")
        }
        workout.tapSet(SQUAT, 1)
        workout.verify {
            setCompleted(SQUAT, 1, reps = 6)
        }
    }

    @Test
    fun cancellingDiscardsTheDraft() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {}
        workout.editExercise(SQUAT) {
            increaseWeight()
            cancel()
        }
        workout.verify {
            exerciseNameShown(SQUAT, "Squat")
            prescriptionShown(SQUAT, "5 × 5 · 20 kg")
        }
    }

    @Test
    fun increasingSetsToTenCanBeReversedBeforeSaving() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {}
        val editor = workout.editExercise(SQUAT) {
            repeat(5) { increaseSets() }
        }
        editor.verify { setsShown(10) }
        repeat(5) { editor.decreaseSets() }
        editor.verify { setsShown(5) }
        editor.save()
        workout.verify { prescriptionShown(SQUAT, "5 × 5 · 20 kg") }
    }

    @Test
    fun tenSavedSetsCanBeReducedToFiveAfterReopeningWithAnEarlierLoggedSet() = runAppUiTest {
        val workout = launchWorkoutScreen(this) { tapSet(SQUAT, 1) }
        workout.editExercise(SQUAT) {
            repeat(5) { increaseSets() }
            save()
        }
        workout.restartApp()
        workout.resumeWorkout()
        val editor = workout.editExercise(SQUAT) {}
        editor.verify { setsShown(10) }
        repeat(5) { editor.decreaseSets() }
        editor.verify { setsShown(5) }
        editor.save()
        workout.verify {
            prescriptionShown(SQUAT, "5 × 5 · 20 kg")
            setCompleted(SQUAT, 1, reps = 5)
        }
    }

    @Test
    fun reducingSetsClearsRemovedCompletionsAndReaddingStartsIncomplete() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {}
        workout.editExercise(SQUAT) {
            repeat(5) { increaseSets() }
            save()
        }
        workout.tapSet(SQUAT, 1)
        workout.tapSet(SQUAT, 10)
        workout.editExercise(SQUAT) {
            repeat(5) { decreaseSets() }
            save()
        }
        workout.verify {
            prescriptionShown(SQUAT, "5 × 5 · 20 kg")
            setCompleted(SQUAT, 1, reps = 5)
        }
        workout.restartApp()
        workout.resumeWorkout()
        workout.editExercise(SQUAT) {
            repeat(5) { increaseSets() }
            save()
        }
        workout.verify {
            setCompleted(SQUAT, 1, reps = 5)
            setIncomplete(SQUAT, 10)
        }
    }

    @Test
    fun cancellingSetReductionKeepsCompletedTrailingSets() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {}
        workout.editExercise(SQUAT) {
            repeat(5) { increaseSets() }
            save()
        }
        workout.tapSet(SQUAT, 10)
        workout.editExercise(SQUAT) {
            repeat(5) { decreaseSets() }
            cancel()
        }
        workout.verify {
            prescriptionShown(SQUAT, "10 × 5 · 20 kg")
            setCompleted(SQUAT, 10, reps = 5)
        }
    }

    @Test
    fun twelveSetsStayOnOneRowAndTheLastSetCanBeCompleted() = runAppUiTest(width = 320.dp) {
        val workout = launchWorkoutScreen(this) {}
        workout.editExercise(SQUAT) {
            repeat(7) { increaseSets() }
            save()
        }
        workout.verify { setsStayOnOneHorizontallyScrollableRow(SQUAT, count = 12) }
        workout.tapSet(SQUAT, 12)
        workout.verify { setCompleted(SQUAT, 12, reps = 5) }
    }

    @Test
    fun exerciseNameCannotBeEdited() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {}
        workout.editExercise(SQUAT) {}.verify {
            nameEditingUnavailable()
        }
    }

    @Test
    fun restOffDoesNotChangeRunningRestButClearsItOnNextCompletion() = runAppUiTest {
        val deadline = nowEpochMillis + 180_000
        val workout = launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
        }
        workout.editExercise(SQUAT) {
            selectRestOff()
            save()
        }
        workout.verify {
            restDeadlineSaved(deadline)
        }
        workout.tapSet(SQUAT, 2)
        workout.verify {
            restTimerAbsent()
            restDeadlineSaved(null)
        }
    }

    @Test
    fun appliedCustomRestSurvivesReopeningAndStartsTheSelectedDuration() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {}
        workout.editExercise(SQUAT) {
            openCustomRest()
            chooseCustomRest(2, 30)
            applyCustomRest()
            save()
        }
        workout.restartApp()
        workout.resumeWorkout()
        workout.tapSet(SQUAT, 1)
        workout.verify {
            restTimerShown("02:30")
        }
    }

    @Test
    fun cancellingCustomRestDoesNotApplyItsDraft() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {}
        workout.editExercise(SQUAT) {
            openCustomRest()
            chooseCustomRest(1, 30)
            cancelCustomRest()
            save()
        }
        workout.tapSet(SQUAT, 1)
        workout.verify {
            restTimerShown("03:00")
        }
    }

    @Test
    fun failedSavePreservesDraftAndRetryAppliesIt() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {}
        blockEditing()
        val editor = workout.editExercise(SQUAT) {
            increaseWeight()
            save(waitForSave = false)
        }
        editor.verify {
            saveErrorShown()
            weightShown("22.5")
        }
        allowWorkoutWrites()
        editor.save()
        workout.verify {
            exerciseNameShown(SQUAT, "Squat")
            prescriptionShown(SQUAT, "5 × 5 · 22.5 kg")
        }
    }
}
