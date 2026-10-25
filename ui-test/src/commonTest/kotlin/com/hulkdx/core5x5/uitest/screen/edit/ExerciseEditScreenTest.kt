package com.hulkdx.core5x5.uitest.screen.edit

import androidx.compose.ui.unit.dp
import com.hulkdx.core5x5.feature.workout.domain.Exercise.*
import com.hulkdx.core5x5.uitest.UiTestOperation
import com.hulkdx.core5x5.uitest.runAppUiTest
import com.hulkdx.core5x5.uitest.screen.workout.launchWorkoutScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ExerciseEditScreenTest {
    @Test
    fun legacyNameSurvivesPrescriptionSaveAndReopening() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {}
        val legacyName = "Tempo Front Squat"
        seedLegacyName(SQUAT, legacyName)
        workout.restartApp()
        workout.resumeWorkout()
        workout.editExercise(SQUAT) {
            verify { nameEditingUnavailable(legacyName) }
            increaseWeight()
            save()
        }
        workout.restartApp()
        workout.resumeWorkout()
        workout.verify {
            exerciseNameShown(SQUAT, legacyName)
            prescriptionShown(SQUAT, "5 × 5 · 22.5 kg")
        }
    }

    @Test
    fun failedShrinkRollsBackPrescriptionAndRecordedTrailingRepsThenRetries() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
            tapSet(SQUAT, 5)
            tapSet(SQUAT, 5)
        }
        val before = assertNotNull(unfinishedWorkout())
        blockSetRemoval()
        val editor = workout.editExercise(SQUAT) {
            increaseWeight()
            decreaseSets()
            save(waitForSave = false)
        }
        editor.verify {
            saveErrorShown()
            setsShown(4)
            weightShown("22.5")
        }
        assertEquals(before, unfinishedWorkout(), "A failed resize must roll back the entire edit")
        allowWorkoutWrites()
        editor.save()
        workout.restartApp()
        workout.resumeWorkout()
        workout.verify {
            prescriptionShown(SQUAT, "4 × 5 · 22.5 kg")
            setCompleted(SQUAT, 1, reps = 5)
        }
        workout.editExercise(SQUAT) { increaseSets(); save() }
        workout.verify { setIncomplete(SQUAT, 5) }
    }

    @Test
    fun loweringPrescriptionKeepsRecordedRepsAboveTheNewTarget() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
            tapSet(SQUAT, 1)
        }
        workout.editExercise(SQUAT) {
            repeat(3) { decreaseReps() }
            save()
        }
        workout.restartApp()
        workout.resumeWorkout()
        workout.verify {
            prescriptionShown(SQUAT, "5 × 2 · 20 kg")
            setCompleted(SQUAT, 1, reps = 4)
        }
        workout.tapSet(SQUAT, 2)
        workout.verify { setCompleted(SQUAT, 2, reps = 2) }
    }

    @Test
    fun savingPrescriptionBlocksSteppersRestAndCancellationUntilCommit() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {}
        val editor = workout.editExercise(SQUAT) { increaseWeight() }
        val save = pauseNext(UiTestOperation.EDIT)
        editor.save(waitForSave = false)
        awaitStarted(save)
        editor.verify { savingBlocksChangesAndDismissal() }
        assertEquals(20.0, assertNotNull(unfinishedWorkout()).exercises.first().weightKg)
        release(save)
        workout.verify { prescriptionShown(SQUAT, "5 × 5 · 22.5 kg") }
    }

    @Test
    fun editingSetOverflowKeepsTheLastSetReachable() = runAppUiTest(
        width = 320.dp, height = 640.dp,
    ) {
        val workout = launchWorkoutScreen(this) {}
        workout.editExercise(SQUAT) {
            repeat(7) { increaseSets() }
            openCustomRest()
            chooseCustomRest(0, 1)
            applyCustomRest()
            save()
        }
        workout.verify { setsStayOnOneHorizontallyScrollableRow(SQUAT, count = 12) }
        workout.tapSet(SQUAT, 12)
        workout.verify {
            setCompleted(SQUAT, 12, reps = 5)
            pinnedTimerDoesNotOverlapList()
        }
        workout.pressFinish()
        workout.verify { workoutCompleteScreenShown(completedSets = 1, prescribedSets = 22) }
        workout.returnToToday()
    }

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
