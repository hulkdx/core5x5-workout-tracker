package com.hulkdx.core5x5.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hulkdx.core5x5.core.navigation.TopLevelTab
import com.hulkdx.core5x5.core.ui.components.TopLevelScreenLayout
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsRoute(onTabSelected: (TopLevelTab) -> Unit, modifier: Modifier = Modifier) {
    val viewModel: SettingsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    TopLevelScreenLayout(selectedTab = TopLevelTab.SETTINGS, onTabSelected = onTabSelected, modifier = modifier) {
        SettingsScreen(
            uiState = uiState,
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
