package com.hulkdx.core5x5.di

import com.hulkdx.core5x5.app.AppLifecycleHandler
import com.hulkdx.core5x5.core.preferences.di.preferencesModule
import com.hulkdx.core5x5.feature.history.di.historyModule
import com.hulkdx.core5x5.feature.settings.di.settingsModule
import com.hulkdx.core5x5.feature.workout.di.restTimerModule
import com.hulkdx.core5x5.feature.workout.di.workoutModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.dsl.module
import org.koin.dsl.onClose

val appModule = module {
    includes(preferencesModule, workoutModule, restTimerModule, historyModule, settingsModule)
    single<CoroutineScope> {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    } onClose { scope -> scope?.cancel() }
    single {
        AppLifecycleHandler(
            restAlertPermissions = get(),
            restAlertObserver = get(),
        )
    }
}
