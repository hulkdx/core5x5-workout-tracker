@file:OptIn(ExperimentalTestApi::class)

package com.hulkdx.core5x5.uitest.screen.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runComposeUiTest
import com.hulkdx.core5x5.core.ui.theme.AppTheme
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.feature.workout.presentation.CustomRestContent
import com.hulkdx.core5x5.feature.workout.presentation.ExerciseEditScreen
import com.hulkdx.core5x5.feature.workout.presentation.ExerciseEditUiState
import com.hulkdx.core5x5.feature.workout.presentation.restDurationLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomRestContentTest {
    @Test
    fun sheetStaysFixedDuringDragsAndValidationChangesAndApplyStillWorks() = runComposeUiTest {
        var edit by mutableStateOf(ExerciseEditUiState(
            position = 0, exerciseName = "Squat", weightKg = 20.0, unit = WeightUnit.KG,
            sets = 5, reps = 5, restDurationMillis = null, effectiveRestDurationMillis = 180_000,
            showCustomRest = true, customRestSeconds = 0,
        ))
        var dismissals = 0
        setContent {
            AppTheme {
                ExerciseEditScreen(
                    state = edit,
                    onDismiss = { dismissals++; edit = edit.copy(showCustomRest = false) },
                    onSave = {}, onWeightChanged = {}, onSetsChanged = {}, onRepsChanged = {},
                    onRestSelected = {}, onOpenCustomRest = {},
                    onCustomRestChanged = { edit = edit.copy(customRestSeconds = it) },
                    onApplyCustomRest = {
                        edit = edit.copy(showCustomRest = false,
                            restDurationMillis = edit.customRestSeconds * 1_000,
                            effectiveRestDurationMillis = edit.customRestSeconds * 1_000)
                    },
                )
            }
        }
        waitForIdle()
        val sheet = onNodeWithTag("custom-rest")
        val top = sheet.fetchSemanticsNode().boundsInRoot.top
        // A downward drag at zero cannot be consumed by either wheel. The sheet
        // must not take over that drag, nor a drag starting on its title.
        for (target in listOf(onNodeWithText("Custom Rest Timer"),
            onNodeWithTag("duration-wheel:Minutes"), onNodeWithTag("duration-wheel:Seconds"))) {
            target.performTouchInput {
                down(center)
                moveBy(Offset(0f, 200f), delayMillis = 500)
            }
            assertEquals(top, sheet.fetchSemanticsNode().boundsInRoot.top)
            target.performTouchInput { up() }
            waitForIdle()
            assertEquals(top, sheet.fetchSemanticsNode().boundsInRoot.top)
        }
        runOnIdle { assertEquals(0, dismissals) }
        val sheetBounds = sheet.fetchSemanticsNode().boundsInRoot
        val apply = onNodeWithTag("custom-rest-apply")
        val applyBounds = apply.fetchSemanticsNode().boundsInRoot
        val error = onNodeWithText("Choose a positive duration, or use Off in the editor.")
        for (seconds in listOf(1, 0, 1)) {
            onNodeWithTag("duration-value:Seconds:$seconds").performClick()
            waitForIdle()
            assertEquals(sheetBounds, sheet.fetchSemanticsNode().boundsInRoot)
            assertEquals(applyBounds, apply.fetchSemanticsNode().boundsInRoot)
            if (seconds == 0) {
                error.assertExists()
                apply.assertIsNotEnabled()
            } else {
                error.assertDoesNotExist()
                apply.assertIsEnabled()
            }
        }
        onNodeWithTag("custom-rest-apply").assertIsEnabled().performClick()
        sheet.assertDoesNotExist()
        onNodeWithTag("edit-save").assertIsEnabled()
        runOnIdle { assertEquals(1_000L, edit.restDurationMillis) }
    }

    @Test
    fun applyIsBlockedDuringWheelFlingThenUsesTheSettledSelection() = runComposeUiTest {
        var seconds by mutableStateOf(180L)
        var appliedSeconds: Long? = null
        setContent {
            AppTheme {
                CustomRestContent(seconds, { seconds = it }, {}, { appliedSeconds = seconds })
            }
        }
        onNodeWithTag("custom-rest-apply").assertIsEnabled()

        // Preserve the fling instead of letting the test clock silently settle it
        // before inspecting Apply, as the existing editor robot does.
        mainClock.autoAdvance = false
        onNodeWithTag("duration-wheel:Seconds").performTouchInput { swipeUp(durationMillis = 200) }
        mainClock.advanceTimeByFrame()
        onNodeWithTag("custom-rest-apply").assertIsNotEnabled()

        mainClock.autoAdvance = true
        waitForIdle()
        onNodeWithTag("custom-rest-apply").assertIsEnabled()
        runOnIdle { assertTrue(seconds > 180L) }
        onNodeWithText("Selected: ${restDurationLabel(seconds)}").assertExists()
        onNodeWithTag("custom-rest-apply").performClick()
        runOnIdle { assertEquals(seconds, appliedSeconds) }
    }

    @Test
    fun zeroDurationCannotBeAppliedButAValueTapEnablesApply() = runComposeUiTest {
        var seconds by mutableStateOf(0L)
        var appliedSeconds: Long? = null
        setContent {
            AppTheme {
                CustomRestContent(seconds, { seconds = it }, {}, { appliedSeconds = seconds })
            }
        }
        onNodeWithTag("custom-rest-apply").assertIsNotEnabled()
        onNodeWithTag("duration-value:Seconds:1").performClick()
        waitForIdle()
        onNodeWithTag("custom-rest-apply").assertIsEnabled().performClick()
        runOnIdle { assertEquals(1L, appliedSeconds) }
    }
}
