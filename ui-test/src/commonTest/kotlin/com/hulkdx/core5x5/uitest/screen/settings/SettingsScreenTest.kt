package com.hulkdx.core5x5.uitest.screen.settings

import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.feature.workout.domain.Exercise.SQUAT
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.uitest.runAppUiTest
import com.hulkdx.core5x5.uitest.screen.history.launchHistoryScreen
import com.hulkdx.core5x5.uitest.screen.today.launchTodayScreen
import com.hulkdx.core5x5.uitest.screen.workout.launchWorkoutScreen
import kotlin.test.Test

class SettingsScreenTest {
    @Test
    fun poundsSurviveReopeningAndAppearOnTodayAndActiveWorkout() = runAppUiTest {
        launchSettingsScreen(this) {
            openUnits()
            chooseUnit(WeightUnit.LB)
        }
        restart()
        launchTodayScreen(this) {}.verify {
            prescriptionsShown("5 × 5 · 40 lb")
        }
        launchSettingsScreen(this) {}.verify {
            unitsShown("Pounds (lb)")
        }
        launchSettingsScreen(this) {
            returnToToday()
        }
        launchWorkoutScreen(this) {}.verify {
            prescriptionShown(SQUAT, "5 × 5 · 40 lb")
        }
    }

    @Test
    fun savingAnUnchangedPoundValueKeepsStoredKilogramPrecision() = runAppUiTest {
        launchSettingsScreen(this) {
            openUnits()
            chooseUnit(WeightUnit.LB)
            returnToToday()
        }
        val workout = launchWorkoutScreen(this) {}
        workout.editExercise(SQUAT) {
            save()
        }
        workout.verify {
            prescriptionShown(SQUAT, "5 × 5 · 40 lb")
            savedWeightKg(SQUAT, 20.0)
        }
    }

    @Test
    fun historyUsesTheChosenWeightUnit() = runAppUiTest {
        launchSettingsScreen(this) {
            openUnits()
            chooseUnit(WeightUnit.LB)
            returnToToday()
        }
        launchWorkoutScreen(this) {
            tapSet(SQUAT, 1)
            pressFinish()
            returnToToday()
        }
        launchHistoryScreen(this) {}.verify {
            savedWorkoutShown(Workout.A)
            loggedSquatWeightShown("40 lb")
        }
    }

    @Test
    fun restDurationControlsAreNotShownInSettings() = runAppUiTest {
        launchSettingsScreen(this) {}.verify { restControlsAbsent() }
    }

    @Test
    fun failedUnitPreferenceWriteKeepsTheSelectionDialogAndCanBeRetried() = runAppUiTest {
        blockPreferenceWrites()
        val settings = launchSettingsScreen(this) {
            openUnits()
            chooseUnit(WeightUnit.LB, waitForSave = false)
        }
        settings.verify {
            preferenceSaveErrorShown()
            unitsShown("Kilograms (kg)")
        }
        allowPreferenceWrites()
        settings.chooseUnit(WeightUnit.LB)
        restart()
        launchSettingsScreen(this) {}.verify { unitsShown("Pounds (lb)") }
    }

    @Test
    fun aboutLicenseAndRepositoryActionsWork() = runAppUiTest {
        val settings = launchSettingsScreen(this) {
            openAbout()
            openLicense()
        }
        settings.verify {
            licenseShown()
        }
        settings.backToAbout()
        settings.openRepository()
        settings.verify {
            uriOpened("https://github.com/hulkdx/core5x5-workout-tracker")
        }
        settings.closeAbout()
    }

    @Test
    fun privacyOpenFailureCanBeRetried() = runAppUiTest {
        failNextUriOpen = true
        val settings = launchSettingsScreen(this) {
            openPrivacy()
        }
        settings.verify {
            privacyOpenErrorShown()
        }
        settings.openPrivacy()
        settings.verify {
            privacyOpenErrorCleared()
            uriOpened("https://sabajafarzadeh.com/core5x5/privacy/")
        }
    }
}
