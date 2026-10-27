package com.hulkdx.core5x5

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.hulkdx.core5x5.app.AppLifecycleHandler
import com.hulkdx.core5x5.app.AppRoot
import com.hulkdx.core5x5.app.StartupPermissions
import com.hulkdx.core5x5.core.ui.theme.AppTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun App() {
    StartupPermissions()

    val lifecycleHandler: AppLifecycleHandler = koinInject()
    val scope = rememberCoroutineScope()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        scope.launch { lifecycleHandler.onResume() }
    }

    AppTheme {
        AppRoot()
    }
}
