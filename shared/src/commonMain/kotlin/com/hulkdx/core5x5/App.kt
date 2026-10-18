package com.hulkdx.core5x5

import androidx.compose.runtime.Composable
import com.hulkdx.core5x5.core.ui.theme.AppTheme
import com.hulkdx.core5x5.di.appModule
import com.hulkdx.core5x5.app.AppRoot
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
fun App() {
    KoinApplication(configuration = koinConfiguration { modules(appModule) }) {
        AppTheme {
            AppRoot()
        }
    }
}
