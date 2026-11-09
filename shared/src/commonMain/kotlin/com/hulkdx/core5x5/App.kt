package com.hulkdx.core5x5

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hulkdx.core5x5.app.AppLifecycleHandler
import com.hulkdx.core5x5.app.AppRoot
import com.hulkdx.core5x5.app.AppearanceViewModel
import com.hulkdx.core5x5.app.StartupPermissions
import com.hulkdx.core5x5.core.preferences.domain.Appearance
import com.hulkdx.core5x5.core.ui.theme.AppHomeTokens
import com.hulkdx.core5x5.core.ui.theme.AppTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(onAppearanceChanged: (darkTheme: Boolean, followSystem: Boolean) -> Unit = { _, _ -> }) {
    val appearanceViewModel: AppearanceViewModel = koinViewModel()
    val savedAppearance by appearanceViewModel.appearance.collectAsStateWithLifecycle()
    val appearance = savedAppearance ?: Appearance.SYSTEM
    val darkTheme = appearance.isDark(isSystemInDarkTheme())
    val lifecycleHandler: AppLifecycleHandler = koinInject()
    val scope = rememberCoroutineScope()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        appearanceViewModel.refresh()
        scope.launch { lifecycleHandler.onResume() }
    }

    SideEffect { onAppearanceChanged(darkTheme, appearance == Appearance.SYSTEM) }

    AppTheme(darkTheme = darkTheme) {
        // Avoid rendering the app in the wrong palette while the saved preference is first read.
        if (savedAppearance == null) {
            Box(Modifier.fillMaxSize().background(AppHomeTokens.Background))
        } else {
            StartupPermissions()
            AppRoot()
        }
    }
}
