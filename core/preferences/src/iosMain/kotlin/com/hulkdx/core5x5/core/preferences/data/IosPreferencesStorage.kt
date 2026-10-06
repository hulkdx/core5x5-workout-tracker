package com.hulkdx.core5x5.core.preferences.data

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUserDefaults

@OptIn(ExperimentalForeignApi::class)
internal class IosPreferencesStorage : PreferencesStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun read(): String? = defaults.stringForKey(KEY)

    override suspend fun write(value: String) {
        defaults.setObject(value, forKey = KEY)
    }

    private companion object {
        const val KEY = "com.hulkdx.core5x5.training_preferences"
    }
}
