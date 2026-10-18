package com.hulkdx.core5x5.feature.settings.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
@SerialName("settings")
data object SettingsKey : NavKey

val settingsNavigationSerializers = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(SettingsKey::class, SettingsKey.serializer())
    }
}
