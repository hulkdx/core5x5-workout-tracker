@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.hulkdx.core5x5.uitest.verification

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.test.platform.app.InstrumentationRegistry
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.feature.history.navigation.WorkoutDetailKey
import com.hulkdx.core5x5.feature.workout.domain.Exercise.*
import com.hulkdx.core5x5.feature.workout.domain.Workout
import com.hulkdx.core5x5.feature.workout.navigation.WorkoutCompleteKey
import com.hulkdx.core5x5.uitest.AppUiTest
import com.hulkdx.core5x5.uitest.UiTestOperation
import com.hulkdx.core5x5.uitest.runAppUiTest
import com.hulkdx.core5x5.uitest.screen.history.restoreWorkoutDetail
import com.hulkdx.core5x5.uitest.screen.settings.launchSettingsScreen
import com.hulkdx.core5x5.uitest.screen.today.launchTodayScreen
import com.hulkdx.core5x5.uitest.screen.workout.launchWorkoutScreen
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Native-window captures at the device viewport, without runner overrides. */
class NativeVisualVerificationTest {
    @Test
    fun todayLoadingSelectionAndStartErrors() = runAppUiTest {
        val read = pauseNext(UiTestOperation.LOAD_ACTIVE)
        restart(waitForReady = false)
        awaitStarted(read)
        capture("today-loading")
        release(read)
        awaitNode(hasTestTag("start-workout") and isEnabled())
        ui.runOnIdle { requests.block(UiTestOperation.LOAD_ACTIVE) }
        restart(waitForReady = false)
        val today = launchTodayScreen(this) {}
        today.verify { errorShown("Unable to load your workout.") }
        capture("today-load-error")
        allowRequests(UiTestOperation.LOAD_ACTIVE)
        today.retryLoad()
        awaitNode(hasTestTag("start-workout") and isEnabled())
        capture("today-ready")
        failNext(UiTestOperation.LOAD_PRESCRIPTION)
        today.pressSwitch()
        today.verify { errorShown("Unable to switch workouts. Try again.") }
        capture("today-switch-error")
        today.switchWorkout(Workout.B)
        failNext(UiTestOperation.START)
        today.startWorkout(waitForStart = false)
        today.verify { errorShown("Unable to start your workout. Try again.") }
        capture("today-start-error")
    }

    @Test
    fun activeSavingRepsSwitchingAndNativeTimerAnchors() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {}
        capture("active-before-first-tap")
        val save = pauseNext(UiTestOperation.COMPLETE_SET)
        workout.tapSet(SQUAT, 1, waitForSave = false)
        awaitStarted(save)
        capture("active-set-saving")
        release(save)
        workout.verify { setCompleted(SQUAT, 1, 5); restTimerShown("03:00") }
        capture("active-after-first-tap")
        val second = pauseNext(UiTestOperation.COMPLETE_SET)
        workout.tapSet(SQUAT, 2, waitForSave = false)
        awaitStarted(second)
        capture("active-rest-set-saving")
        release(second)
        workout.verify { setCompleted(SQUAT, 2, 5) }
        workout.tapSet(SQUAT, 1)
        capture("active-numeric-reps")
        advanceTimeBy(180_000)
        workout.verify { restTimerShown("00:00", expired = true) }
        capture("active-expired-a")
        ui.onNodeWithTag("active-workout-list").performScrollToIndex(0)
        val switching = pauseNext(UiTestOperation.SWITCH)
        workout.pressSwitchExpectingFailure()
        awaitStarted(switching)
        capture("active-switch-saving")
        release(switching)
        workout.verify { workoutShown(Workout.B) }
        ui.onNodeWithTag("active-workout-list").performScrollToIndex(0)
        capture("active-switch-b")
        failNext(UiTestOperation.SWITCH)
        workout.pressSwitchExpectingFailure()
        workout.verify { switchErrorShown() }
        capture("active-switch-error")
        workout.switchWorkout(Workout.A)
        workout.switchWorkout(Workout.B)
        workout.tapSet(DEADLIFT, 1)
        capture("active-rest-b")
        advanceTimeBy(180_000)
        workout.verify { restTimerShown("00:00", expired = true) }
        capture("active-expired-b")
        val finish = pauseNext(UiTestOperation.FINISH)
        workout.pressFinish(waitForSave = false)
        awaitStarted(finish)
        capture("active-finalizing")
        release(finish)
        workout.verify { workoutCompleteScreenShown(1, prescribedSets = 11) }
        capture("complete-partial-b")
    }

    @Test
    fun editorOverflowAndNativeCustomRestWheelStability() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {}
        seedLegacyName(SQUAT, "Legacy paused barbell squat with a long saved exercise name")
        restart()
        workout.resumeWorkout()
        val editor = workout.editExercise(SQUAT) {}
        capture("editor-long-header")
        repeat(7) { editor.increaseSets() }
        editor.openCustomRest()
        capture("custom-rest-positive")
        editor.chooseCustomRest(0, 0)
        awaitNode(hasText("Choose a positive duration, or use Off in the editor.")).performScrollTo().assertIsDisplayed()
        awaitNode(hasTestTag("custom-rest-apply")).performScrollTo().assertIsNotEnabled()
        val zeroApplyTop = ui.onNodeWithTag("custom-rest-apply").fetchSemanticsNode().positionInRoot.y
        capture("custom-rest-zero")
        editor.chooseCustomRest(0, 1)
        awaitNode(hasTestTag("custom-rest-apply") and isEnabled()).performScrollTo()
        assertEquals(zeroApplyTop, ui.onNodeWithTag("custom-rest-apply").fetchSemanticsNode().positionInRoot.y)
        val sheet = ui.onNodeWithTag("custom-rest").fetchSemanticsNode().boundsInRoot
        ui.onNodeWithTag("duration-wheel:Seconds").performTouchInput { swipeUp() }
        ui.waitForIdle()
        assertEquals(sheet, ui.onNodeWithTag("custom-rest").fetchSemanticsNode().boundsInRoot)
        capture("custom-rest-after-wheel-gesture")
        editor.applyCustomRest()
        editor.save()
        workout.verify { setsStayOnOneHorizontallyScrollableRow(SQUAT, 12) }
        capture("active-overflow-last-set")
        workout.tapSet(SQUAT, 12)
        workout.verify { setCompleted(SQUAT, 12, 5, "Legacy paused barbell squat with a long saved exercise name") }
        capture("active-overflow-completed")
    }

    @Test
    fun nativeCustomRestNumericInputAndKeyboard() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {}
        seedLegacyRestDuration(SQUAT, Int.MAX_VALUE.toLong() * 60_000)
        restart()
        workout.resumeWorkout()
        val editor = workout.editExercise(SQUAT) { openCustomRest() }
        awaitNode(hasSetTextAction()).performScrollTo().performClick().assertIsFocused()
        capture("custom-rest-keyboard")
        awaitNode(hasTestTag("custom-rest-cancel")).performScrollTo().assertIsDisplayed()
        capture("custom-rest-keyboard-actions")
        editor.cancelCustomRest()
        editor.cancel()
    }

    @Test
    fun settingsRootErrorsAndNativeScrollingDialogs() = runAppUiTest {
        val settings = launchSettingsScreen(this) {}
        settings.verify { restControlsAbsent() }
        capture("settings-root")
        settings.openUnits()
        capture("settings-units")
        blockPreferenceWrites()
        settings.chooseUnit(WeightUnit.LB, waitForSave = false)
        settings.verify { failedUnitSaveRetainsSelectionForRetry() }
        capture("settings-units-save-error")
        settings.cancelUnits()
        settings.openAbout()
        capture("settings-about")
        failNextUriOpen = true
        settings.openRepository()
        awaitNode(hasText("Unable to open the repository. Try again.")).performScrollTo().assertIsDisplayed()
        capture("settings-about-error")
        settings.openLicense()
        capture("settings-license-top")
        settings.verify { licenseShown() }
        capture("settings-license-scrolled-actions")
        settings.backToAbout()
        settings.closeAbout()
        val read = pauseNext(UiTestOperation.READ_PREFERENCES)
        restart()
        awaitStarted(read)
        launchSettingsScreen(this, waitForPreferences = false) {}
        awaitNode(hasText("Loading preferences…")).assertIsDisplayed()
        capture("settings-loading")
        release(read)
        awaitNode(hasTestTag("settings-units") and isEnabled())
        blockPreferenceReads()
        restart()
        launchSettingsScreen(this, waitForPreferences = false) {}
        awaitNode(hasText("Unable to load preferences. Try again.")).assertIsDisplayed()
        capture("settings-load-error")
    }

    @Test
    fun nativeCompleteAndDetailLoadingMissingErrorsAndMetrics() = runAppUiTest {
        val workout = launchWorkoutScreen(this) {}
        seedLegacyName(SQUAT, "Legacy paused barbell squat with a long saved exercise name")
        restart()
        workout.resumeWorkout()
        workout.editExercise(SQUAT) { repeat(3) { increaseReps() }; save() }
        workout.tapSet(SQUAT, 1)
        workout.tapSet(SQUAT, 1)
        workout.tapSet(SQUAT, 2)
        repeat(8) { workout.tapSet(SQUAT, 2) }
        capture("active-custom-and-reset-reps")
        workout.pressFinish()
        workout.verify { workoutCompleteScreenShown(1) }
        capture("complete-a")
        val saved = assertNotNull(savedWorkout())
        val read = pauseNext(UiTestOperation.LOAD_COMPLETE)
        restart(WorkoutCompleteKey(saved.id), waitForReady = false)
        awaitStarted(read)
        awaitNode(hasText("Loading saved workout…")).assertIsDisplayed()
        capture("complete-loading")
        release(read)
        awaitNode(hasText("Workout Complete"))
        failNext(UiTestOperation.LOAD_COMPLETE)
        restart(WorkoutCompleteKey(saved.id), waitForReady = false)
        awaitNode(hasText("Unable to load the saved workout. Try again.")).assertIsDisplayed()
        capture("complete-error")
        restart(WorkoutCompleteKey(saved.id + 1), waitForReady = false)
        awaitNode(hasText("No saved completed workout was found.")).assertIsDisplayed()
        capture("complete-missing")
        val detail = restoreWorkoutDetail(this, saved.id)
        detail.verify { savedSessionShown() }
        capture("detail-metrics")
        awaitNode(hasText("Legacy paused barbell squat with a long saved exercise name")).performScrollTo().assertIsDisplayed()
        capture("detail-long-name-numeric-reps")
        val loading = pauseNext(UiTestOperation.LOAD_DETAIL)
        restart(WorkoutDetailKey(saved.id), waitForReady = false)
        awaitStarted(loading)
        awaitNode(hasText("Loading saved workout…")).assertIsDisplayed()
        capture("detail-loading")
        release(loading)
        detail.verify { savedSessionShown() }
        failNext(UiTestOperation.LOAD_DETAIL)
        restart(WorkoutDetailKey(saved.id), waitForReady = false)
        detail.verify { loadErrorShown() }
        capture("detail-error")
        restoreWorkoutDetail(this, saved.id + 1).verify { missingShown() }
        capture("detail-missing")
    }
}

private fun AppUiTest.capture(scene: String) {
    ui.waitForIdle()
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.waitForIdleSync()
    val context = instrumentation.targetContext
    val profile = InstrumentationRegistry.getArguments().getString("captureProfile", "native")
    // AGP retrieves this directory before uninstalling the isolated test APK.
    val output = requireNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")) {
        "Pass an app-writable additionalTestOutputDir to retain native captures"
    }
    val directory = File(output, profile).apply { check(mkdirs() || isDirectory) }
    // Compose idleness does not wait for the native Dialog/IME window animations.
    // Require the displayed pixels to settle, rather than accepting a fading window.
    var previous: Bitmap? = null
    var settled: Bitmap? = null
    var stableSince = SystemClock.uptimeMillis()
    try {
        ui.waitUntil(timeoutMillis = 5_000) {
            val frame = assertNotNull(instrumentation.uiAutomation.takeScreenshot())
            val stable = previous?.sameAs(frame) == true
            if (!stable) stableSince = SystemClock.uptimeMillis()
            previous?.recycle()
            previous = frame
            // The loading spinner is intentionally continuous; its native window is already idle.
            if (scene == "today-loading" || stable && SystemClock.uptimeMillis() - stableSince >= 200) {
                settled = frame
                previous = null
                true
            } else false
        }
    } finally {
        previous?.recycle()
    }
    val bitmap = assertNotNull(settled)
    File(directory, "$scene.png").outputStream().use {
        assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
    }
    val config = context.resources.configuration
    File(directory, "environment.txt").writeText(
        "pixels=${bitmap.width}x${bitmap.height}\n" +
            "configurationDp=${config.screenWidthDp}x${config.screenHeightDp}\n" +
            "densityDpi=${config.densityDpi}\nfontScale=${config.fontScale}\n",
    )
    bitmap.recycle()
}
