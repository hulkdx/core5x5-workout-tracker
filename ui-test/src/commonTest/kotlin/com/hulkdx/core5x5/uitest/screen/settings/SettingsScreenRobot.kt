@file:OptIn(ExperimentalTestApi::class)

package com.hulkdx.core5x5.uitest.screen.settings

import androidx.compose.ui.test.*
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.uitest.AppUiTest
import kotlin.test.assertEquals

fun launchSettingsScreen(
    app: AppUiTest,
    waitForPreferences: Boolean = true,
    actions: SettingsScreenRobot.() -> Unit,
): SettingsScreenRobot {
    app.awaitNode(hasTestTag("tab:SETTINGS")).performClick()
    app.awaitNode(hasTestTag("settings"))
    if (waitForPreferences) app.awaitNode(hasTestTag("settings-units") and isEnabled())
    return SettingsScreenRobot(app).apply(actions)
}

class SettingsScreenRobot(private val app: AppUiTest) {
    fun openUnits() { app.awaitNode(hasTestTag("settings-units") and isEnabled()).performScrollTo().performClick() }
    fun cancelUnits() {
        app.awaitNode(hasTestTag("settings-cancel")).performScrollTo().performClick()
        app.awaitAbsent("settings-dialog")
    }
    fun retryPreferences() { app.awaitNode(hasText("Try Again")).performScrollTo().performClick() }
    fun chooseUnit(unit: WeightUnit, waitForSave: Boolean = true) {
        app.awaitNode(hasTestTag("settings-unit:${unit.name}") and isEnabled()).performClick()
        if (waitForSave) app.awaitAbsent("settings-dialog")
    }
    fun returnToToday() { app.awaitNode(hasTestTag("tab:TODAY")).performClick() }
    fun openAbout() { app.awaitNode(hasTestTag("settings-about")).performScrollTo().performClick() }
    fun openLicense() { app.awaitNode(hasTestTag("settings-license")).performScrollTo().performClick() }
    fun backToAbout() { app.awaitNode(hasTestTag("settings-back-about")).performScrollTo().performClick() }
    fun openRepository() { app.awaitNode(hasTestTag("settings-repository")).performScrollTo().performClick() }
    fun closeAbout() { app.awaitNode(hasTestTag("settings-close")).performScrollTo().performClick() }
    fun openPrivacy() { app.awaitNode(hasTestTag("settings-privacy")).performScrollTo().performClick() }
    fun verify(assertions: SettingsScreenVerify.() -> Unit) = SettingsScreenVerify(app).apply(assertions)
}

class SettingsScreenVerify(private val app: AppUiTest) {
    fun loadingShown() { app.awaitNode(hasText("Loading preferences…")).assertIsDisplayed() }
    fun loadErrorShown() { app.awaitNode(hasText("Unable to load preferences. Try again.")).assertIsDisplayed() }
    fun unitSelected(unit: WeightUnit) { app.awaitNode(hasTestTag("settings-unit:${unit.name}")).assertIsSelected() }
    fun savingBlocksUnitChangesAndCancellation() {
        app.awaitNode(hasText("Saving…")).performScrollTo().assertIsDisplayed()
        for (unit in WeightUnit.entries) {
            app.ui.onNodeWithTag("settings-unit:${unit.name}").performScrollTo().assertIsNotEnabled()
        }
        app.ui.onNodeWithTag("settings-cancel").performScrollTo().assertIsNotEnabled()
    }
    fun unitsShown(label: String) {
        app.awaitNode(hasTestTag("settings-units") and hasText(label, substring = true)).assertTextContains(label)
    }
    fun restControlsAbsent() {
        app.ui.onNodeWithTag("settings-rest").assertDoesNotExist()
        app.ui.onNodeWithText("Rest Timer").assertDoesNotExist()
        app.ui.onNodeWithText("WORKOUT").assertDoesNotExist()
    }
    fun preferenceSaveErrorShown() { app.awaitNode(hasText("Unable to save preferences. Try again.")).assertIsDisplayed() }
    fun licenseShown() { app.awaitNode(hasTestTag("settings-back-about")).performScrollTo().assertIsDisplayed() }
    fun uriOpened(uri: String) = assertEquals(uri, app.openedUris.lastOrNull())
    fun privacyOpenErrorShown() {
        app.awaitNode(hasText("Unable to open the privacy policy. Tap Privacy Policy to try again.")).performScrollTo().assertIsDisplayed()
    }
    fun privacyOpenErrorCleared() {
        app.ui.onNodeWithText("Unable to open the privacy policy. Tap Privacy Policy to try again.").assertDoesNotExist()
    }
}
