package com.hulkdx.core5x5.feature.settings.domain

import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences

internal fun parseRestDurationSeconds(input: String): Long? {
    val text = input.trim()
    if (text.isEmpty() || text.any { it !in '0'..'9' }) return null
    return text.toLongOrNull()?.takeIf { it in 1..TrainingPreferences.MAX_REST_DURATION_SECONDS }
}
