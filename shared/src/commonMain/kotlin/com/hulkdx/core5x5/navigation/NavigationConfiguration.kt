package com.hulkdx.core5x5.navigation

import androidx.savedstate.serialization.SavedStateConfiguration
import com.hulkdx.core5x5.feature.history.navigation.historyNavigationSerializers
import com.hulkdx.core5x5.feature.settings.navigation.settingsNavigationSerializers
import com.hulkdx.core5x5.feature.workout.navigation.workoutNavigationSerializers
import kotlinx.serialization.modules.plus

val navigationSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = workoutNavigationSerializers + historyNavigationSerializers + settingsNavigationSerializers
}
