package com.example.myapplication

import androidx.compose.runtime.Composable
import com.example.myapplication.core.ui.theme.Core5x5Theme
import com.example.myapplication.di.appModule
import com.example.myapplication.shell.ShellRoute
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
