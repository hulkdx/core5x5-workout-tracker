package com.hulkdx.core5x5.feature.settings.di

import com.hulkdx.core5x5.feature.settings.domain.AppMetadata
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSBundle

@OptIn(ExperimentalForeignApi::class)
internal actual val settingsPlatformModule: Module = module {
    single<AppMetadata> {
        AppMetadata { NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String }
    }
}
