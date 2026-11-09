package com.hulkdx.core5x5.core.preferences.domain

/** Persisted names are part of the preference record's compatibility contract. */
enum class Appearance {
    SYSTEM, LIGHT, DARK;

    fun isDark(systemIsDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemIsDark
        LIGHT -> false
        DARK -> true
    }
}
