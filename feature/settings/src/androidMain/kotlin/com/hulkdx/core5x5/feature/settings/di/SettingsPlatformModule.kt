package com.hulkdx.core5x5.feature.settings.di

import android.content.Context
import com.hulkdx.core5x5.feature.settings.domain.AppMetadata
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val settingsPlatformModule: Module = module {
    single<AppMetadata> {
        val context: Context = get()
        AppMetadata {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }
    }
}
