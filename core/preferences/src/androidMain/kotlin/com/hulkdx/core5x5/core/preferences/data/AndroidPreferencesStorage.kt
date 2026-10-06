package com.hulkdx.core5x5.core.preferences.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AndroidPreferencesStorage(context: Context) : PreferencesStorage {
    private val preferences = context.applicationContext.getSharedPreferences("core5x5_preferences", Context.MODE_PRIVATE)

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
