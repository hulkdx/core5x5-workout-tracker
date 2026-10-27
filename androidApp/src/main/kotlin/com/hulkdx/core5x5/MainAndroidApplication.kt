package com.hulkdx.core5x5

import android.app.Application
import com.hulkdx.core5x5.core.restalerts.data.AndroidRestAlertChannels
import com.hulkdx.core5x5.di.androidRestAlertModule
import com.hulkdx.core5x5.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainAndroidApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidRestAlertChannels.create(this)
        startKoin {
            androidContext(this@MainAndroidApplication)
            modules(appModule, androidRestAlertModule)
        }
    }
}
