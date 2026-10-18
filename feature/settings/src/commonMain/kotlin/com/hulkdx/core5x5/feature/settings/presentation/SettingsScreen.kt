package com.hulkdx.core5x5.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clip
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
import com.hulkdx.core5x5.core.navigation.TopLevelTab
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.core.ui.components.AppBottomNavigation
import com.hulkdx.core5x5.core.ui.components.AppButtonPalette
import com.hulkdx.core5x5.core.ui.components.AppPrimaryButton
import com.hulkdx.core5x5.core.ui.components.AppSecondaryButton
import com.hulkdx.core5x5.core.ui.components.AppSettingRow
import com.hulkdx.core5x5.core.ui.theme.AppColors
import com.hulkdx.core5x5.core.ui.theme.AppDimensions
import com.hulkdx.core5x5.core.ui.theme.AppHomeTokens
import com.hulkdx.core5x5.core.ui.theme.AppSettingsTokens as Settings
import com.hulkdx.core5x5.core.ui.theme.AppTheme
import com.hulkdx.core5x5.core.ui.theme.AppTypography

/** The route's TopLevelScreenLayout owns insets and bottom navigation. */
@Composable
fun SettingsScreen(
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
    onOpenPrivacyPolicy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().background(Settings.Background)
            .verticalScroll(rememberScrollState()).padding(
                horizontal = Settings.Inset,
                vertical = Settings.TopGap,
            ),
    ) {
        Text(
            text = "Settings",
            modifier = Modifier.semantics { heading() },
            style = Settings.Title,
            color = Settings.Primary,
        )
        Spacer(Modifier.height(Settings.TitleGap))
        val preferences = uiState.preferences
        when {
            uiState.isLoading -> SettingsMessage("Loading preferences…")
            preferences != null -> {
                SettingsSection("WORKOUT") {
                    AppSettingRow(
                        label = "Rest Timer",
                        value = formatRestDuration(preferences.restDurationSeconds),
                        enabled = uiState.canChangePreferences,
                        onClick = onOpenRestDuration,
                    )
                }
                Spacer(Modifier.height(Settings.SectionGap))
                SettingsSection("PREFERENCES") {
                    AppSettingRow(
                        label = "Weight Unit",
                        value = preferences.weightUnit.displayName(),
                        enabled = uiState.canChangePreferences,
                        onClick = onOpenUnits,
                    )
                }
            }
            else -> {
                SettingsMessage("Unable to load preferences. Try again.", error = true)
                Spacer(Modifier.height(Settings.HeadingGap))
                AppSecondaryButton(label = "Try Again", onClick = onRetryLoad, palette = AppButtonPalette.Home)
            }
        }
        Spacer(Modifier.height(Settings.SectionGap))
        SettingsSection("ABOUT") {
            AppSettingRow(
                label = "Version",
                value = uiState.versionName ?: "Unavailable",
            )
            SettingsDivider()
            AppSettingRow(
                label = "About Core5x5",
                value = "",
                onClick = onOpenAbout,
            )
            SettingsDivider()
            AppSettingRow(
                label = "Privacy Policy",
                value = "",
                onClick = onOpenPrivacyPolicy,
            )
        }
        if (uiState.error == SettingsError.OPEN_PRIVACY_POLICY) {
            Spacer(Modifier.height(Settings.HeadingGap))
            SettingsMessage("Unable to open the privacy policy. Tap Privacy Policy to try again.", error = true)
        }
        Spacer(Modifier.height(Settings.BottomPadding))
    }

    when (uiState.dialog) {
        SettingsDialog.UNITS -> SettingsDialogSurface("Units", onDismissDialog) {
            Column(modifier = Modifier.selectableGroup()) {
                WeightUnit.entries.forEach { unit ->
                    val selected = uiState.preferences?.weightUnit == unit
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = AppDimensions.SettingHeight)
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
                            style = AppTypography.Body,
                            color = Settings.Primary,
                        )
                        if (selected) {
                            Text(
                                text = "✓",
                                modifier = Modifier.clearAndSetSemantics {},
                                style = AppTypography.Body,
                                color = Settings.Action,
                            )
                        }
                    }
                }
            }
            SaveStatus(uiState)
            AppSecondaryButton(label = "Cancel", enabled = !uiState.isSaving, onClick = onDismissDialog, palette = AppButtonPalette.Home)
        }
        SettingsDialog.REST_DURATION -> SettingsDialogSurface("Rest duration", onDismissDialog) {
            SettingsMessage("Duration in seconds")
            BasicTextField(
                value = uiState.restDurationInput,
                onValueChange = onRestDurationInput,
                enabled = !uiState.isSaving,
                singleLine = true,
                textStyle = AppTypography.Body.copy(color = Settings.Primary),
                cursorBrush = SolidColor(Settings.Action),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (uiState.canSaveRestDuration) onSaveRestDuration() }),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Rest duration in seconds" },
                decorationBox = { field ->
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = AppDimensions.SettingHeight)
                            .border(
                                width = AppDimensions.BorderStroke,
                                color = if (uiState.hasInvalidRestDuration) AppColors.Destructive else Settings.Border,
                                shape = RoundedCornerShape(AppDimensions.RadiusMedium),
                            ).padding(AppDimensions.RestGap),
                        contentAlignment = Alignment.CenterStart,
                    ) { field() }
                },
            )
            if (uiState.hasInvalidRestDuration) {
                SettingsMessage("Enter a positive whole number of seconds.", error = true)
            }
            SettingsMessage("Applies to future timers. A running timer keeps its current time.")
            SaveStatus(uiState)
            AppPrimaryButton(
                palette = AppButtonPalette.Home,
                label = if (uiState.isSaving) "Saving…" else "Save",
                enabled = uiState.canSaveRestDuration,
                onClick = onSaveRestDuration,
            )
            AppSecondaryButton(label = "Cancel", enabled = !uiState.isSaving, onClick = onDismissDialog, palette = AppButtonPalette.Home)
        }
        SettingsDialog.ABOUT -> SettingsDialogSurface("About Core5x5", onDismissDialog) {
            SettingsMessage("Core5x5: Workout Tracker")
            SettingsMessage(uiState.versionName?.let { "Version $it" } ?: "Version unavailable")
            SettingsMessage("Open-source strength training for Android and iOS.")
            if (uiState.error == SettingsError.OPEN_REPOSITORY) {
                SettingsMessage("Unable to open the repository. Try again.", error = true)
            }
            AppSecondaryButton(label = "Open-source repository", onClick = onOpenRepository, palette = AppButtonPalette.Home)
            AppSecondaryButton(label = "MIT License", onClick = onOpenLicense, palette = AppButtonPalette.Home)
            AppPrimaryButton(label = "Close", onClick = onDismissDialog, palette = AppButtonPalette.Home)
        }
        SettingsDialog.LICENSE -> SettingsDialogSurface("License", onDismissDialog) {
            Text(text = MIT_LICENSE, style = AppTypography.Body, color = Settings.Primary)
            AppSecondaryButton(label = "Back to About", onClick = onOpenAbout, palette = AppButtonPalette.Home)
        }
        null -> Unit
    }
}

@Composable
private fun SettingsDialogSurface(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(
                horizontal = AppDimensions.ScreenInset,
                vertical = AppDimensions.ContentPaddingVertical,
            ).imePadding(),
            shape = RoundedCornerShape(AppDimensions.RadiusLarge),
            color = Settings.Card,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(AppDimensions.ScreenInset),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.RestGap),
            ) {
                Text(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    style = AppTypography.Heading,
                    color = Settings.Primary,
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
        style = AppTypography.Body,
        color = if (error) AppColors.Destructive else Settings.Secondary,
    )
}

@Composable
private fun SaveStatus(uiState: SettingsUiState) {
    if (uiState.error == SettingsError.SAVE) SettingsMessage("Unable to save preferences. Try again.", error = true)
    if (uiState.isSaving) SettingsMessage("Saving…")
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Settings.HeadingGap)) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = Settings.BorderStroke).semantics { heading() },
            style = Settings.Section,
            color = Settings.Secondary,
        )
        val shape = RoundedCornerShape(Settings.CardRadius)
        Column(
            modifier = Modifier.fillMaxWidth().clip(shape)
                .background(Settings.Card).border(Settings.BorderStroke, Settings.Border, shape),
        ) { content() }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = Settings.CardInset),
        thickness = Settings.BorderStroke,
        color = Settings.Border,
    )
}

@Preview(name = "Settings kg", widthDp = 390, heightDp = 874)
@Composable
private fun SettingsScreenPreview() = SettingsPreview()

@Preview(name = "Settings lb, narrow", widthDp = 320, heightDp = 640)
@Composable
private fun SettingsNarrowPreview() = SettingsPreview(TrainingPreferences(weightUnit = WeightUnit.LB, restDurationSeconds = 3_601))

@Preview(name = "Settings larger text", widthDp = 390, heightDp = 874)
@Composable
private fun SettingsLargeTextPreview() {
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
        SettingsPreview()
    }
}

@Composable
private fun SettingsPreview(preferences: TrainingPreferences = TrainingPreferences()) {
    AppTheme {
        Column(
            modifier = Modifier.fillMaxSize().background(Settings.Background)
                .windowInsetsPadding(WindowInsets(top = AppHomeTokens.SystemTopReserve)),
        ) {
            SettingsScreen(
                uiState = SettingsUiState(isLoading = false, preferences = preferences, versionName = "1.0.0"),
                modifier = Modifier.weight(1f),
                onOpenUnits = {}, onOpenRestDuration = {}, onOpenAbout = {}, onOpenLicense = {},
                onSelectUnit = {}, onRestDurationInput = {}, onSaveRestDuration = {},
                onDismissDialog = {}, onRetryLoad = {}, onOpenRepository = {}, onOpenPrivacyPolicy = {},
            )
            Box(
                modifier = Modifier.fillMaxWidth().background(AppHomeTokens.Card)
                    .windowInsetsPadding(WindowInsets(bottom = AppHomeTokens.SystemBottomReserve)),
            ) {
                AppBottomNavigation(
                    selectedItem = TopLevelTab.SETTINGS,
                    onItemSelected = {},
                    useHomeDesign = true,
                )
            }
        }
    }
}
