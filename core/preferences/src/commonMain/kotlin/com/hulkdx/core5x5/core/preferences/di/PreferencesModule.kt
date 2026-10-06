package com.hulkdx.core5x5.core.preferences.di

import com.hulkdx.core5x5.core.preferences.data.PersistedTrainingPreferencesRepository
import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferencesRepository
import org.koin.core.module.Module
import org.koin.dsl.module

val preferencesModule = module {
    includes(preferencesPlatformModule)
    single<TrainingPreferencesRepository> { PersistedTrainingPreferencesRepository(get()) }
}

internal expect val preferencesPlatformModule: Module
