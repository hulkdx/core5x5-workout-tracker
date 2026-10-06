package com.hulkdx.core5x5.core.preferences.domain

import kotlin.math.floor

enum class WeightUnit(val symbol: String, private val kilogramsPerUnit: Double) {
    KG("kg", 1.0),
    // International avoirdupois pound: https://www.nist.gov/system/files/documents/calibrations/sp250-31.pdf
    LB("lb", 0.45359237);

    fun fromKilograms(weightKg: Double): Double {
        require(weightKg.isFinite() && weightKg >= 0.0)
        return weightKg / kilogramsPerUnit
    }

    fun toKilograms(weight: Double): Double {
        require(weight.isFinite() && weight >= 0.0)
        return weight * kilogramsPerUnit
    }
}

/** Display only: nearest tenth, positive ties up, no trailing .0, with an explicit unit suffix. */
fun formatWeight(weightKg: Double, unit: WeightUnit): String {
    val displayed = unit.fromKilograms(weightKg)
    require(displayed < Long.MAX_VALUE.toDouble() / 10.0)
    val tenths = floor(displayed * 10.0 + 0.5).toLong()
    val number = if (tenths % 10L == 0L) {
        (tenths / 10L).toString()
    } else {
        "${tenths / 10L}.${tenths % 10L}"
    }
    return "$number ${unit.symbol}"
}
