package com.hulkdx.core5x5.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hulkdx.core5x5.core.common.Logger
import com.hulkdx.core5x5.core.common.SystemSettingsLauncher
import com.hulkdx.core5x5.core.common.runCatchingSuspend
import com.hulkdx.core5x5.core.navigation.TopLevelTab
import com.hulkdx.core5x5.core.restalerts.domain.RestNotificationPermission
import com.hulkdx.core5x5.core.restalerts.domain.RestNotificationPermission.GRANTED
import com.hulkdx.core5x5.core.ui.components.TopLevelScreenLayout
import com.hulkdx.core5x5.feature.settings.permissions.rememberNotificationPermissionRequester
import kotlinx.coroutines.launch
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsRoute(onTabSelected: (TopLevelTab) -> Unit, modifier: Modifier = Modifier) {
    val viewModel: SettingsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val koin = getKoin()
    val scope = rememberCoroutineScope()
    val requestNotificationPermission = rememberNotificationPermissionRequester()

    TopLevelScreenLayout(selectedTab = TopLevelTab.SETTINGS, onTabSelected = onTabSelected, modifier = modifier) {
        SettingsScreen(
            uiState = uiState,
            onRestNotificationsChange = { checked, notificationPermission ->
                viewModel.setRestNotificationsEnabled(checked)
                if (checked && notificationPermission != GRANTED) {
                    requestNotificationPermission()
                }
            },
            onRestSoundChange = viewModel::setRestSoundEnabled,
            onOpenPreciseTimingSettings = {
                scope.launch {
                    runCatchingSuspend {
                        // Resolve this Android-only dependency when its row is clicked.
                        koin.get<SystemSettingsLauncher>().openPreciseTimingSettings()
                    }.onFailure { error ->
                        Logger.warn("Settings", "Could not open precise timing settings", error)
                    }
                }
            },
            onOpenUnits = viewModel::openUnits,
            onOpenAbout = viewModel::openAbout,
            onOpenLicense = viewModel::openLicense,
            onSelectUnit = viewModel::selectWeightUnit,
            onDismissDialog = viewModel::dismissDialog,
            onRetryLoad = viewModel::loadPreferences,
            onOpenRepository = {
                try {
                    uriHandler.openUri("https://github.com/hulkdx/core5x5-workout-tracker")
                } catch (_: Exception) {
                    viewModel.onRepositoryOpenFailed()
                }
            },
            onOpenPrivacyPolicy = {
                try {
                    uriHandler.openUri("https://sabajafarzadeh.com/core5x5/privacy/")
                    viewModel.onPrivacyPolicyOpened()
                } catch (_: Exception) {
                    viewModel.onPrivacyPolicyOpenFailed()
                }
            },
        )
    }
}
