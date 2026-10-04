package com.hulkdx.core5x5

import androidx.compose.runtime.Composable
import com.hulkdx.core5x5.core.ui.theme.Core5x5Theme
import com.hulkdx.core5x5.di.appModule
import com.hulkdx.core5x5.shell.ShellRoute
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
fun App() {
    KoinApplication(configuration = koinConfiguration { modules(appModule) }) {
        Core5x5Theme {
            ShellRoute()
        }
    }
}
