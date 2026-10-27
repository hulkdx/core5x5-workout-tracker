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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hulkdx.core5x5.core.navigation.TopLevelTab
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences
import com.hulkdx.core5x5.core.preferences.domain.WeightUnit
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissionStatus
import com.hulkdx.core5x5.core.restalerts.domain.RestNotificationPermission
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
    onOpenAbout: () -> Unit,
    onOpenLicense: () -> Unit,
    onSelectUnit: (WeightUnit) -> Unit,
    onDismissDialog: () -> Unit,
    onRetryLoad: () -> Unit,
    onOpenRepository: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenPreciseTimingSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onRestNotificationsChange: (Boolean, RestNotificationPermission) -> Unit = { _, _ -> },
    onRestSoundChange: (Boolean) -> Unit = {},
) {
    Column(
        modifier = modifier.testTag("settings").fillMaxSize().background(Settings.Background)
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
                SettingsSection("PREFERENCES") {
                    AppSettingRow(
                        label = "Weight Unit",
                        modifier = Modifier.testTag("settings-units"),
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
        NotificationsSection(
            uiState = uiState,
            onEnableChange = onRestNotificationsChange,
            onSoundChange = onRestSoundChange,
            onOpenPreciseTimingSettings = onOpenPreciseTimingSettings,
        )
        Spacer(Modifier.height(Settings.SectionGap))
        SettingsSection("ABOUT") {
            AppSettingRow(
                label = "Version",
                value = uiState.versionName ?: "Unavailable",
            )
            SettingsDivider()
            AppSettingRow(
                label = "About Core5x5",
                modifier = Modifier.testTag("settings-about"),
                value = "",
                onClick = onOpenAbout,
            )
            SettingsDivider()
            AppSettingRow(
                label = "Privacy Policy",
                modifier = Modifier.testTag("settings-privacy"),
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
                        modifier = Modifier.testTag("settings-unit:${unit.name}").fillMaxWidth().heightIn(min = AppDimensions.SettingHeight)
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
            AppSecondaryButton(label = "Cancel", modifier = Modifier.testTag("settings-cancel"), enabled = !uiState.isSaving, onClick = onDismissDialog, palette = AppButtonPalette.Home)
        }
        SettingsDialog.ABOUT -> SettingsDialogSurface("About Core5x5", onDismissDialog) {
            SettingsMessage("Core5x5: Workout Tracker")
            SettingsMessage(uiState.versionName?.let { "Version $it" } ?: "Version unavailable")
            SettingsMessage("Open-source strength training for Android and iOS.")
            if (uiState.error == SettingsError.OPEN_REPOSITORY) {
                SettingsMessage("Unable to open the repository. Try again.", error = true)
            }
            AppSecondaryButton(label = "Open-source repository", modifier = Modifier.testTag("settings-repository"), onClick = onOpenRepository, palette = AppButtonPalette.Home)
            AppSecondaryButton(label = "MIT License", modifier = Modifier.testTag("settings-license"), onClick = onOpenLicense, palette = AppButtonPalette.Home)
            AppPrimaryButton(label = "Close", modifier = Modifier.testTag("settings-close"), onClick = onDismissDialog, palette = AppButtonPalette.Home)
        }
        SettingsDialog.LICENSE -> SettingsDialogSurface("License", onDismissDialog) {
            Text(text = MIT_LICENSE, style = AppTypography.Body, color = Settings.Primary)
            AppSecondaryButton(label = "Back to About", modifier = Modifier.testTag("settings-back-about"), onClick = onOpenAbout, palette = AppButtonPalette.Home)
        }
        null -> Unit
    }
}

@Composable
private fun NotificationsSection(
    uiState: SettingsUiState,
    onEnableChange: (Boolean, RestNotificationPermission) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onOpenPreciseTimingSettings: () -> Unit,
) {
    val preferences = uiState.preferences ?: TrainingPreferences()
    val preciseTimingAllowed = uiState.restAlertPermissionStatus.preciseTimingAllowed

    Spacer(Modifier.height(Settings.SectionGap))
    SettingsSection("NOTIFICATIONS") {
        SettingsSwitchRow(
            label = "Enable",
            checked = uiState.restNotificationsEnabled,
            enabled = uiState.canChangePreferences,
            onCheckedChange = { checked -> onEnableChange(checked, uiState.restAlertPermissionStatus.notificationPermission) },
        )
        SettingsDivider()
        SettingsSwitchRow(
            label = "Sound",
            checked = preferences.restSoundEnabled,
            enabled = uiState.canChangeRestSound,
            onCheckedChange = onSoundChange,
        )
        if (preciseTimingAllowed != null) {
            SettingsDivider()
            AppSettingRow(
                label = "Precise timing",
                value = if (preciseTimingAllowed) "Allowed" else "Not allowed",
                subtitle = "Rest notifications may arrive late without precise timing.",
                modifier = Modifier.testTag("settings-precise-timing"),
                onClick = onOpenPreciseTimingSettings,
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = Settings.RowHeight)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = Settings.CardInset, vertical = Settings.RowPaddingVertical),
        horizontalArrangement = Arrangement.spacedBy(Settings.ValueGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = Settings.Label,
            color = if (enabled) Settings.Primary else Settings.Secondary,
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier.clearAndSetSemantics {},
            colors = SwitchDefaults.colors(
                checkedThumbColor = Settings.Background,
                checkedTrackColor = Settings.Action,
                checkedBorderColor = Settings.Action,
                uncheckedThumbColor = Settings.Secondary,
                uncheckedTrackColor = Settings.Card,
                uncheckedBorderColor = Settings.Border,
                disabledCheckedThumbColor = Settings.Secondary,
                disabledCheckedTrackColor = Settings.Border,
                disabledCheckedBorderColor = Settings.Border,
                disabledUncheckedThumbColor = Settings.Secondary,
                disabledUncheckedTrackColor = Settings.Card,
                disabledUncheckedBorderColor = Settings.Border,
            ),
        )
    }
}

@Composable
private fun SettingsDialogSurface(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.testTag("settings-dialog").fillMaxWidth().padding(
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

@Preview(name = "Settings lb", widthDp = 390, heightDp = 874)
@Composable
private fun SettingsLbPreview() = SettingsPreview(TrainingPreferences(weightUnit = WeightUnit.LB))

@Preview(name = "Precise timing not allowed", widthDp = 390, heightDp = 874)
@Composable
private fun SettingsPreciseTimingPreview() = SettingsPreview(
    permissionStatus = RestAlertPermissionStatus(
        preciseTimingAllowed = false,
    ),
)

@Preview(name = "Precise timing allowed", widthDp = 390, heightDp = 874)
@Composable
private fun SettingsPreciseTimingAllowedPreview() = SettingsPreview(
    permissionStatus = RestAlertPermissionStatus(preciseTimingAllowed = true),
)

@Preview(name = "Notification permission denied", widthDp = 390, heightDp = 874)
@Composable
private fun SettingsNotificationPermissionDeniedPreview() = SettingsPreview(
    permissionStatus = RestAlertPermissionStatus(
        notificationPermission = RestNotificationPermission.DENIED,
        preciseTimingAllowed = false,
    ),
)

@Composable
private fun SettingsPreview(
    preferences: TrainingPreferences = TrainingPreferences(),
    permissionStatus: RestAlertPermissionStatus = RestAlertPermissionStatus(),
) {
    AppTheme {
        Column(
            modifier = Modifier.fillMaxSize().background(Settings.Background)
                .windowInsetsPadding(WindowInsets(top = AppHomeTokens.SystemTopReserve)),
        ) {
            SettingsScreen(
                uiState = SettingsUiState(
                    isLoading = false,
                    preferences = preferences,
                    versionName = "1.0.0",
                    restAlertPermissionStatus = permissionStatus,
                ),
                modifier = Modifier.weight(1f),
                onOpenUnits = {}, onOpenAbout = {}, onOpenLicense = {},
                onSelectUnit = {},
                onDismissDialog = {}, onRetryLoad = {}, onOpenRepository = {}, onOpenPrivacyPolicy = {},
                onOpenPreciseTimingSettings = {},
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
