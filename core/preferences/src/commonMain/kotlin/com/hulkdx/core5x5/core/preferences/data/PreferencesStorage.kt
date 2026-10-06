package com.hulkdx.core5x5.core.preferences.data

/** One record permits an atomic update of the two preferences independently of workout storage. */
internal interface PreferencesStorage {
    suspend fun read(): String?
    suspend fun write(value: String)
}
