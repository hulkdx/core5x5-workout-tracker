package com.hulkdx.core5x5.feature.settings.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.hulkdx.core5x5.core.navigation.CoreNavigator
import com.hulkdx.core5x5.feature.settings.presentation.SettingsRoute

fun EntryProviderScope<NavKey>.settingsEntries(navigator: CoreNavigator) {
    entry<SettingsKey>(clazzContentKey = { "settings" }) {
        SettingsRoute(onTabSelected = navigator::selectTab)
    }
}
