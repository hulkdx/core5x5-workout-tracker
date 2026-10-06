package com.hulkdx.core5x5.feature.settings.di

import com.hulkdx.core5x5.feature.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    viewModel { SettingsViewModel() }
}
