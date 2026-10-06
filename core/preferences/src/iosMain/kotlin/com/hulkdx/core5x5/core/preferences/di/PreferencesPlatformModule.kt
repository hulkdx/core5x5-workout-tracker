package com.hulkdx.core5x5.core.preferences.di

import com.hulkdx.core5x5.core.preferences.data.IosPreferencesStorage
import com.hulkdx.core5x5.core.preferences.data.PreferencesStorage
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val preferencesPlatformModule: Module = module {
    single<PreferencesStorage> { IosPreferencesStorage() }
}
