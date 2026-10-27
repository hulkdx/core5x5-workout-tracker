package com.hulkdx.core5x5.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hulkdx.core5x5.core.common.SystemSettingsLauncher
import com.hulkdx.core5x5.core.common.runCatchingSuspend
import com.hulkdx.core5x5.core.preferences.data.AndroidStartupPermissionPreferences
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissions
import com.hulkdx.core5x5.core.restalerts.domain.RestNotificationPermission
import com.hulkdx.core5x5.core.ui.components.AppButtonPalette
import com.hulkdx.core5x5.core.ui.components.AppPrimaryButton
import com.hulkdx.core5x5.core.ui.components.AppSecondaryButton
import com.hulkdx.core5x5.core.ui.theme.AppColors
import com.hulkdx.core5x5.core.ui.theme.AppDimensions
import com.hulkdx.core5x5.core.ui.theme.AppSettingsTokens
import com.hulkdx.core5x5.core.ui.theme.AppTypography
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun PreciseTimingExplanationDialog() {
    val permissions: RestAlertPermissions = koinInject()
    val permissionStatus by permissions.status.collectAsStateWithLifecycle()
    val notificationsAllowed = permissionStatus.notificationPermission == RestNotificationPermission.GRANTED
    val needsTimingAccess = notificationsAllowed && permissionStatus.preciseTimingAllowed == false
    if (!needsTimingAccess) return

    val preferences: AndroidStartupPermissionPreferences = koinInject()
    // Restore a visible dialog on rotation; saved history suppresses later instances.
    var showExplanation by rememberSaveable {
        mutableStateOf(!preferences.preciseTimingExplanationShown)
    }
    if (!showExplanation) return

    LaunchedEffect(Unit) {
        preferences.markPreciseTimingExplanationShown()
    }

    val settingsLauncher: SystemSettingsLauncher = koinInject()
    val scope = rememberCoroutineScope()
    var openSettingsFailed by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { showExplanation = false },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(
                horizontal = AppDimensions.ScreenInset,
                vertical = AppDimensions.ContentPaddingVertical,
            ),
            shape = RoundedCornerShape(AppDimensions.RadiusLarge),
            color = AppSettingsTokens.Card,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(AppDimensions.ScreenInset),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.RestGap),
            ) {
                Text(
                    text = "Rest notifications",
                    modifier = Modifier.semantics { heading() },
                    style = AppTypography.Heading,
                    color = AppSettingsTokens.Primary,
                )
                Text(
                    text = "Background rest notifications may be delayed unless Alarms & reminders access " +
                        "is enabled. You can enable it in Settings.",
                    style = AppTypography.Body,
                    color = AppSettingsTokens.Secondary,
                )
                AppPrimaryButton(
                    label = "Open settings",
                    onClick = {
                        scope.launch {
                            runCatchingSuspend { settingsLauncher.openPreciseTimingSettings() }
                                .onSuccess { showExplanation = false }
                                .onFailure { openSettingsFailed = true }
                        }
                    },
                    palette = AppButtonPalette.Home,
                )
                if (openSettingsFailed) {
                    Text(
                        text = "Unable to open system settings. Try again.",
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = AppTypography.Body,
                        color = AppColors.Destructive,
                    )
                }
                AppSecondaryButton(
                    label = "Not now",
                    onClick = { showExplanation = false },
                    palette = AppButtonPalette.Home,
                )
            }
        }
    }
}
