package com.hulkdx.core5x5.feature.settings.presentation

import com.hulkdx.core5x5.core.preferences.domain.WeightUnit

fun WeightUnit.displayName(): String = when (this) {
    WeightUnit.KG -> "Kilograms (kg)"
    WeightUnit.LB -> "Pounds (lb)"
}
