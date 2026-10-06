package com.hulkdx.core5x5.feature.settings.di

import com.hulkdx.core5x5.feature.settings.presentation.SettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    includes(settingsPlatformModule)
    viewModel { SettingsViewModel(repository = get(), appMetadata = get()) }
}

internal expect val settingsPlatformModule: Module
