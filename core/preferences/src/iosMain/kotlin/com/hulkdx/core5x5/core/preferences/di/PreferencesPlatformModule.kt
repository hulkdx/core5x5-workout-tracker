package com.hulkdx.core5x5.core.preferences.di

import com.hulkdx.core5x5.core.preferences.data.IosPreferencesStorage
import com.hulkdx.core5x5.core.preferences.data.IosStartupPermissionPreferences
import com.hulkdx.core5x5.core.preferences.data.PreferencesStorage
import com.hulkdx.core5x5.core.preferences.domain.StartupPermissionPreferences
import org.koin.core.module.Module
import org.koin.dsl.module

actual val preferencesPlatformModule: Module = module {
    single<PreferencesStorage> { IosPreferencesStorage() }
    single<StartupPermissionPreferences> { IosStartupPermissionPreferences() }
}
