package com.hulkdx.core5x5.core.preferences.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidPreferencesStorage(context: Context, name: String = "core5x5_preferences") : PreferencesStorage {
    private val preferences = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        preferences.getString("training_preferences", null)
    }

    override suspend fun write(value: String) {
        withContext(Dispatchers.IO) {
            check(preferences.edit().putString("training_preferences", value).commit()) {
                "Unable to save training preferences"
            }
        }
    }
}
