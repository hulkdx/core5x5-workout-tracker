package com.hulkdx.core5x5.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.core.ui.components.Core5x5BottomNavigation
import com.hulkdx.core5x5.core.ui.components.Core5x5PrimaryButton
import com.hulkdx.core5x5.core.ui.components.Core5x5SecondaryButton
import com.hulkdx.core5x5.core.ui.components.Core5x5SettingRow
import com.hulkdx.core5x5.core.ui.components.Core5x5NavigationItem
import com.hulkdx.core5x5.core.ui.theme.Core5x5Colors
import com.hulkdx.core5x5.core.ui.theme.Core5x5Dimensions
import com.hulkdx.core5x5.core.ui.theme.Core5x5Theme
import com.hulkdx.core5x5.core.ui.theme.Core5x5Typography

/** AppNavigation owns insets and bottom navigation; this screen owns only its content padding. */
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onOpenUnits: () -> Unit,
    onOpenRestDuration: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenLicense: () -> Unit,
    onSelectUnit: (WeightUnit) -> Unit,
    onRestDurationInput: (String) -> Unit,
    onSaveRestDuration: () -> Unit,
    onDismissDialog: () -> Unit,
    onRetryLoad: () -> Unit,
    onOpenRepository: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(
            start = Core5x5Dimensions.ScreenInset,
            end = Core5x5Dimensions.ScreenInset,
            top = Core5x5Dimensions.RestGap,
            bottom = Core5x5Dimensions.ContentPaddingVertical,
        ),
        verticalArrangement = Arrangement.spacedBy(Core5x5Dimensions.RestGap),
    ) {
        Text(
            text = "Settings",
            modifier = Modifier.semantics { heading() },
            style = Core5x5Typography.Title,
            color = Core5x5Colors.PrimaryText,
        )
        val preferences = uiState.preferences
        when {
            uiState.isLoading -> SettingsMessage("Loading preferences…")
            preferences != null -> {
                Core5x5SettingRow(
                    label = "Rest timer",
                    value = formatRestDuration(preferences.restDurationSeconds),
                    enabled = uiState.canChangePreferences,
                    onClick = onOpenRestDuration,
                )
                SettingsDivider()
                Core5x5SettingRow(
                    label = "Units",
                    value = preferences.weightUnit.displayName(),
                    enabled = uiState.canChangePreferences,
                    onClick = onOpenUnits,
                )
                SettingsDivider()
            }
            else -> {
                SettingsMessage("Unable to load preferences. Try again.", error = true)
                Core5x5SecondaryButton(label = "Try Again", onClick = onRetryLoad)
            }
        }
        Core5x5SettingRow(
            label = "About",
            value = uiState.versionName?.let { "v$it" } ?: "Version unavailable",
            onClick = onOpenAbout,
        )
        SettingsDivider()
    }

    when (uiState.dialog) {
        SettingsDialog.UNITS -> SettingsDialogSurface("Units", onDismissDialog) {
            Column(modifier = Modifier.selectableGroup()) {
                WeightUnit.entries.forEach { unit ->
                    val selected = uiState.preferences?.weightUnit == unit
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = Core5x5Dimensions.SettingHeight)
                            .selectable(
                                selected = selected,
                                enabled = !uiState.isSaving,
                                role = Role.RadioButton,
                                onClick = { onSelectUnit(unit) },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = unit.displayName(),
                            modifier = Modifier.weight(1f),
                            style = Core5x5Typography.Body,
                            color = Core5x5Colors.PrimaryText,
                        )
                        if (selected) {
                            Text(
                                text = "✓",
                                modifier = Modifier.clearAndSetSemantics {},
                                style = Core5x5Typography.Body,
                                color = Core5x5Colors.Action,
                            )
                        }
                    }
                }
            }
            SaveStatus(uiState)
            Core5x5SecondaryButton(label = "Cancel", enabled = !uiState.isSaving, onClick = onDismissDialog)
        }
        SettingsDialog.REST_DURATION -> SettingsDialogSurface("Rest duration", onDismissDialog) {
            SettingsMessage("Duration in seconds")
            BasicTextField(
                value = uiState.restDurationInput,
                onValueChange = onRestDurationInput,
                enabled = !uiState.isSaving,
                singleLine = true,
                textStyle = Core5x5Typography.Body.copy(color = Core5x5Colors.PrimaryText),
                cursorBrush = SolidColor(Core5x5Colors.Action),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (uiState.canSaveRestDuration) onSaveRestDuration() }),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Rest duration in seconds" },
                decorationBox = { field ->
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = Core5x5Dimensions.SettingHeight)
                            .border(
                                width = Core5x5Dimensions.BorderStroke,
                                color = if (uiState.hasInvalidRestDuration) Core5x5Colors.Destructive else Core5x5Colors.Border,
                                shape = RoundedCornerShape(Core5x5Dimensions.RadiusMedium),
                            ).padding(Core5x5Dimensions.RestGap),
                        contentAlignment = Alignment.CenterStart,
                    ) { field() }
                },
            )
            if (uiState.hasInvalidRestDuration) {
                SettingsMessage("Enter a positive whole number of seconds.", error = true)
            }
            SettingsMessage("Applies to future timers. A running timer keeps its current time.")
            SaveStatus(uiState)
            Core5x5PrimaryButton(
                label = if (uiState.isSaving) "Saving…" else "Save",
                enabled = uiState.canSaveRestDuration,
                onClick = onSaveRestDuration,
            )
            Core5x5SecondaryButton(label = "Cancel", enabled = !uiState.isSaving, onClick = onDismissDialog)
        }
        SettingsDialog.ABOUT -> SettingsDialogSurface("About Core5x5", onDismissDialog) {
            SettingsMessage("Core5x5: Workout Tracker")
            SettingsMessage(uiState.versionName?.let { "Version $it" } ?: "Version unavailable")
            SettingsMessage("Open-source strength training for Android and iOS.")
            if (uiState.error == SettingsError.OPEN_REPOSITORY) {
                SettingsMessage("Unable to open the repository. Try again.", error = true)
            }
            Core5x5SecondaryButton(label = "Open-source repository", onClick = onOpenRepository)
            Core5x5SecondaryButton(label = "MIT License", onClick = onOpenLicense)
            Core5x5PrimaryButton(label = "Close", onClick = onDismissDialog)
        }
        SettingsDialog.LICENSE -> SettingsDialogSurface("License", onDismissDialog) {
            Text(text = MIT_LICENSE, style = Core5x5Typography.Body, color = Core5x5Colors.PrimaryText)
            Core5x5SecondaryButton(label = "Back to About", onClick = onOpenAbout)
        }
        null -> Unit
    }
}

@Composable
private fun SettingsDialogSurface(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(
                horizontal = Core5x5Dimensions.ScreenInset,
                vertical = Core5x5Dimensions.ContentPaddingVertical,
            ).imePadding(),
            shape = RoundedCornerShape(Core5x5Dimensions.RadiusLarge),
            color = Core5x5Colors.Elevated,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(Core5x5Dimensions.ScreenInset),
                verticalArrangement = Arrangement.spacedBy(Core5x5Dimensions.RestGap),
            ) {
                Text(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    style = Core5x5Typography.Heading,
                    color = Core5x5Colors.PrimaryText,
                )
                content()
            }
        }
    }
}

@Composable
private fun SettingsMessage(text: String, error: Boolean = false) {
    Text(
        text = text,
        modifier = if (error) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier,
        style = Core5x5Typography.Body,
        color = if (error) Core5x5Colors.Destructive else Core5x5Colors.SecondaryText,
    )
}

@Composable
private fun SaveStatus(uiState: SettingsUiState) {
    if (uiState.error == SettingsError.SAVE) SettingsMessage("Unable to save preferences. Try again.", error = true)
    if (uiState.isSaving) SettingsMessage("Saving…")
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(thickness = Core5x5Dimensions.BorderStroke, color = Core5x5Colors.Border)
}

@Preview(name = "Settings kg", widthDp = 390, heightDp = 844)
@Composable
private fun SettingsScreenPreview() = SettingsPreview()

@Preview(name = "Settings lb, narrow", widthDp = 320, heightDp = 640)
@Composable
private fun SettingsNarrowPreview() = SettingsPreview(TrainingPreferences(weightUnit = WeightUnit.LB, restDurationSeconds = 3_601))

@Preview(name = "Settings larger text", widthDp = 390, heightDp = 844)
@Composable
private fun SettingsLargeTextPreview() {
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1.5f)) {
        SettingsPreview()
    }
}

@Composable
private fun SettingsPreview(preferences: TrainingPreferences = TrainingPreferences()) {
    Core5x5Theme {
        Column(
            modifier = Modifier.fillMaxSize().background(Core5x5Colors.Background)
                .windowInsetsPadding(WindowInsets(top = Core5x5Dimensions.ReferenceSystemTopReserve)),
        ) {
            SettingsScreen(
                uiState = SettingsUiState(isLoading = false, preferences = preferences, versionName = "1.0"),
                modifier = Modifier.weight(1f),
                onOpenUnits = {}, onOpenRestDuration = {}, onOpenAbout = {}, onOpenLicense = {},
                onSelectUnit = {}, onRestDurationInput = {}, onSaveRestDuration = {},
                onDismissDialog = {}, onRetryLoad = {}, onOpenRepository = {},
            )
            Core5x5BottomNavigation(
                selectedItem = Core5x5NavigationItem.SETTINGS,
                onItemSelected = {},
                historyEnabled = false,
            )
        }
    }
}
