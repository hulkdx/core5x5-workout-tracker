package com.hulkdx.core5x5.feature.settings.presentation

import com.hulkdx.core5x5.core.preferences.domain.WeightUnit

internal fun WeightUnit.displayName(): String = when (this) {
    WeightUnit.KG -> "Kilograms (kg)"
    WeightUnit.LB -> "Pounds (lb)"
}

internal fun formatRestDuration(seconds: Long): String = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
