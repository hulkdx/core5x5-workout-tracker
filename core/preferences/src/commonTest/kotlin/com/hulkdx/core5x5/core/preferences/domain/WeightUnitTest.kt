package com.hulkdx.core5x5.core.preferences.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class WeightUnitTest {
    @Test
    fun internationalPoundsConvertInBothDirectionsWithoutDisplayRounding() {
        assertEquals(0.45359237, WeightUnit.LB.toKilograms(1.0))
        assertEquals(1.0, WeightUnit.LB.fromKilograms(0.45359237))
        assertEquals(44.09245243697551, WeightUnit.LB.fromKilograms(20.0), 1e-12)
        for (kg in listOf(0.0, 20.0, 60.125, 125.5)) {
            assertEquals(kg, WeightUnit.KG.fromKilograms(kg))
            assertEquals(kg, WeightUnit.LB.toKilograms(WeightUnit.LB.fromKilograms(kg)), 1e-10)
        }
    }

    @Test
    fun displayUsesOneDecimalPositiveHalfUpRoundingAndDropsTrailingZero() {
        assertEquals("20 kg", formatWeight(20.0, WeightUnit.KG))
        assertEquals("20 kg", formatWeight(20.04, WeightUnit.KG))
        assertEquals("20.1 kg", formatWeight(20.05, WeightUnit.KG))
        assertEquals("20.2 kg", formatWeight(20.15, WeightUnit.KG))
        assertEquals("0 kg", formatWeight(0.0, WeightUnit.KG))
        assertEquals("0.1 kg", formatWeight(0.05, WeightUnit.KG))
        assertEquals("44.1 lb", formatWeight(20.0, WeightUnit.LB))
        assertEquals("1 lb", formatWeight(0.45359237, WeightUnit.LB))
    }

    @Test
    fun invalidWeightsCannotBecomeMisleadingLabels() {
        for (weight in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> { formatWeight(weight, WeightUnit.KG) }
            assertFailsWith<IllegalArgumentException> { WeightUnit.LB.toKilograms(weight) }
        }
    }
}
