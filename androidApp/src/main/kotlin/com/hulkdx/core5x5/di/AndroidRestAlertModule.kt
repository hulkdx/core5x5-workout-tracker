package com.hulkdx.core5x5.di

import com.hulkdx.core5x5.core.common.AndroidSystemSettingsLauncher
import com.hulkdx.core5x5.core.common.SystemSettingsLauncher
import com.hulkdx.core5x5.core.restalerts.data.AndroidRestAlertPermissions
import com.hulkdx.core5x5.core.restalerts.domain.RestAlertPermissions
import org.koin.dsl.module

val androidRestAlertModule = module {
    includes(androidRestAlertDeliveryModule)
    single<RestAlertPermissions> { AndroidRestAlertPermissions(context = get()) }
    single<SystemSettingsLauncher> { AndroidSystemSettingsLauncher(context = get()) }
}
