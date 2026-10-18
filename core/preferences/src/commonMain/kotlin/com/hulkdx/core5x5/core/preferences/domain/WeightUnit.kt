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

/** Display only: kg uses nearest tenth; lb rounds down to a whole multiple of five. */
fun formatWeight(weightKg: Double, unit: WeightUnit): String {
    val displayed = unit.fromKilograms(weightKg)
    require(displayed < Long.MAX_VALUE.toDouble() / 10.0)
    if (unit == WeightUnit.LB) {
        var pounds = floor(displayed / 5.0).toLong() * 5L
        // Compare kg boundaries so binary conversion error cannot move an exact 5 lb step.
        if (unit.toKilograms(pounds.toDouble()) > weightKg) {
            pounds -= 5L
        } else if (unit.toKilograms((pounds + 5L).toDouble()) <= weightKg) {
            pounds += 5L
        }
        return "$pounds ${unit.symbol}"
    }
    val tenths = floor(displayed * 10.0 + 0.5).toLong()
    val number = if (tenths % 10L == 0L) {
        (tenths / 10L).toString()
    } else {
        "${tenths / 10L}.${tenths % 10L}"
    }
    return "$number ${unit.symbol}"
}
