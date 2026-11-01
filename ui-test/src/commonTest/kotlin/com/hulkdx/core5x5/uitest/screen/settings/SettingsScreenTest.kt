@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.hulkdx.core5x5.uitest.screen.settings

import androidx.compose.ui.unit.dp
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissionStatus
import com.hulkdx.core5x5.core.restalerts.domain.RestNotificationPermission
import com.hulkdx.core5x5.feature.workout.domain.Exercise.SQUAT
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.uitest.UiTestOperation
import com.hulkdx.core5x5.uitest.runAppUiTest
import com.hulkdx.core5x5.uitest.screen.history.launchHistoryScreen
import com.hulkdx.core5x5.uitest.screen.today.launchTodayScreen
import com.hulkdx.core5x5.uitest.screen.workout.launchWorkoutScreen
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsScreenTest {
    @Test
    fun switchesPersistIndependentlyAndMasterOffRetainsTheSavedSoundChoice() = runAppUiTest {
        val settings = launchSettingsScreen(this) {}
        settings.verify { notificationSwitches(enable = true, sound = true); preciseTimingAbsent() }
        settings.toggleNotifications()
        settings.verify { notificationSwitches(enable = false, sound = true) }
        restart()
        val restored = launchSettingsScreen(this) {}
        restored.verify { notificationSwitches(enable = false, sound = true) }
        restored.toggleNotifications()
        restored.toggleSound()
        restored.verify { notificationSwitches(enable = true, sound = false) }
        restart()
        launchSettingsScreen(this) {}.verify { notificationSwitches(enable = true, sound = false) }
    }

    @Test
    fun deniedPermissionKeepsEnableOffAndRefreshesTimingStatus() = runAppUiTest {
        setRestAlertPermissionStatus(RestAlertPermissionStatus(RestNotificationPermission.DENIED, false))
        val settings = launchSettingsScreen(this) {}
        settings.verify {
            notificationSwitches(enable = false, sound = true)
            preciseTimingShown(allowed = false)
        }
        setRestAlertPermissionStatus(RestAlertPermissionStatus(RestNotificationPermission.GRANTED, true))
        settings.verify {
            notificationSwitches(enable = true, sound = true)
            preciseTimingShown(allowed = true)
        }
        setRestAlertPermissionStatus(RestAlertPermissionStatus(RestNotificationPermission.GRANTED))
        settings.verify { preciseTimingAbsent() }
    }

    @Test
    fun pendingSwitchWriteKeepsSavedPositionsAndDisablesBothRowsUntilCommitted() = runAppUiTest {
        val settings = launchSettingsScreen(this) {}
        val write = pauseNext(UiTestOperation.WRITE_PREFERENCES)
        settings.toggleSound()
        awaitStarted(write)
        settings.verify { notificationSwitches(enable = true, sound = true, enabled = false); noSaveStatusText() }
        release(write)
        settings.verify { notificationSwitches(enable = true, sound = false) }
    }

    @Test
    fun failedSwitchWriteRetainsSavedPositionsWithoutStatusTextAndAllowsRetry() = runAppUiTest {
        val settings = launchSettingsScreen(this) {}
        blockPreferenceWrites()
        settings.toggleNotifications()
        settings.verify { notificationSwitches(enable = true, sound = true); noSaveStatusText() }
        allowPreferenceWrites()
        settings.toggleNotifications()
        settings.verify { notificationSwitches(enable = false, sound = true) }
    }

    @Test
    fun preciseTimingRemainsAvailableAfterPreferenceReadAndSettingsLaunchFailures() = runAppUiTest(
        prepare = { blockPreferenceReads() },
    ) {
        setRestAlertPermissionStatus(RestAlertPermissionStatus(RestNotificationPermission.DENIED, false))
        val settings = launchSettingsScreen(this, waitForPreferences = false) {}
        settings.verify {
            loadErrorShown()
            notificationSwitches(enable = false, sound = true, enabled = false)
            preciseTimingShown(allowed = false)
        }
        failNextTimingSettingsOpen = true
        settings.openPreciseTimingSettings()
        settings.verify { preciseTimingShown(allowed = false) }
        settings.openPreciseTimingSettings()
        ui.waitForIdle()
        assertEquals(2, timingSettingsOpens)
    }

    @Test
    fun cancellingUnitsPreservesTheSavedSelectionAfterReopening() = runAppUiTest {
        val settings = launchSettingsScreen(this) { openUnits() }
        settings.verify { unitSelected(WeightUnit.KG) }
        settings.cancelUnits()
        restart()
        launchSettingsScreen(this) {}.verify { unitsShown("Kilograms (kg)") }
    }

    @Test
    fun pendingUnitWriteBlocksBothChoicesAndCancelUntilPersistenceCompletes() = runAppUiTest {
        val settings = launchSettingsScreen(this) { openUnits() }
        val write = pauseNext(UiTestOperation.WRITE_PREFERENCES)
        settings.chooseUnit(WeightUnit.LB, waitForSave = false)
        awaitStarted(write)
        settings.verify { savingBlocksUnitChangesAndCancellation() }
        release(write)
        awaitAbsent("settings-dialog")
        restart()
        launchSettingsScreen(this) {}.verify { unitsShown("Pounds (lb)") }
    }

    @Test
    fun preferenceLoadingLeavesAboutLicenseAndPrivacyAvailable() = runAppUiTest {
        val read = pauseNext(UiTestOperation.READ_PREFERENCES)
        restart()
        awaitStarted(read)
        val settings = launchSettingsScreen(this, waitForPreferences = false) {}
        settings.verify { loadingShown(); notificationSwitches(enable = true, sound = true, enabled = false) }
        settings.openAbout()
        settings.openLicense()
        settings.verify { licenseShown() }
        settings.backToAbout()
        settings.closeAbout()
        settings.openPrivacy()
        settings.verify { uriOpened("https://sabajafarzadeh.com/core5x5/privacy/") }
        release(read)
        settings.verify { unitsShown("Kilograms (kg)") }
    }

    @Test
    fun preferenceLoadFailureKeepsInformationalActionsAndRetries() = runAppUiTest(
        width = 320.dp, height = 640.dp,
        prepare = { blockPreferenceReads() },
    ) {
        val settings = launchSettingsScreen(this, waitForPreferences = false) {}
        settings.verify { loadErrorShown() }
        settings.openAbout()
        settings.openLicense()
        settings.verify { licenseShown() }
        settings.backToAbout()
        settings.closeAbout()
        settings.openPrivacy()
        settings.verify { uriOpened("https://sabajafarzadeh.com/core5x5/privacy/") }
        allowPreferenceReads()
        settings.retryPreferences()
        settings.openUnits()
        settings.verify { unitSelected(WeightUnit.KG) }
        settings.chooseUnit(WeightUnit.LB)
        settings.verify { unitsShown("Pounds (lb)"); restControlsAbsent() }
    }

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
            failedUnitSaveRetainsSelectionForRetry()
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
