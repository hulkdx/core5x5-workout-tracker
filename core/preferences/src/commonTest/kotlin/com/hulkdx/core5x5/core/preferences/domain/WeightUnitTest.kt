package com.hulkdx.core5x5.core.preferences.domain

import kotlin.math.nextDown
import kotlin.math.nextUp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WeightUnitTest {
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
    fun kilogramsUseOneDecimalPositiveHalfUpRoundingAndDropTrailingZero() {
        assertEquals("20 kg", formatWeight(20.0, WeightUnit.KG))
        assertEquals("20 kg", formatWeight(20.04, WeightUnit.KG))
        assertEquals("20.1 kg", formatWeight(20.05, WeightUnit.KG))
        assertEquals("20.2 kg", formatWeight(20.15, WeightUnit.KG))
        assertEquals("0 kg", formatWeight(0.0, WeightUnit.KG))
        assertEquals("0.1 kg", formatWeight(0.05, WeightUnit.KG))
    }

    @Test
    fun poundsRoundDownToWholeFivePoundIncrements() {
        assertEquals("40 lb", formatWeight(20.0, WeightUnit.LB))
        assertEquals("45 lb", formatWeight(22.5, WeightUnit.LB))
        assertEquals("85 lb", formatWeight(40.0, WeightUnit.LB))
        assertEquals("130 lb", formatWeight(60.0, WeightUnit.LB))
        assertEquals("0 lb", formatWeight(0.0, WeightUnit.LB))
        assertEquals("0 lb", formatWeight(WeightUnit.LB.toKilograms(4.999), WeightUnit.LB))
        assertEquals("5 lb", formatWeight(WeightUnit.LB.toKilograms(5.0), WeightUnit.LB))
        assertEquals("5 lb", formatWeight(WeightUnit.LB.toKilograms(9.999), WeightUnit.LB))
        assertEquals("10 lb", formatWeight(WeightUnit.LB.toKilograms(10.0), WeightUnit.LB))
    }

    @Test
    fun exactPoundBoundariesAreNotLostToFloatingPointConversion() {
        for (pounds in listOf(5, 25, 75, 150, 300, 395)) {
            val weightKg = WeightUnit.LB.toKilograms(pounds.toDouble())
            assertEquals("$pounds lb", formatWeight(weightKg, WeightUnit.LB))
            assertEquals("${pounds - 5} lb", formatWeight(weightKg - 1e-9, WeightUnit.LB))
            assertEquals("$pounds lb", formatWeight(weightKg + 1e-9, WeightUnit.LB))
        }
    }

    @Test
    fun adjacentRepresentableWeightsStayOnTheCorrectSideOfEveryFivePoundBoundary() {
        for (pounds in 5..1_000 step 5) {
            val weightKg = WeightUnit.LB.toKilograms(pounds.toDouble())
            assertEquals("${pounds - 5} lb", formatWeight(weightKg.nextDown(), WeightUnit.LB), "$pounds lb: below")
            assertEquals("$pounds lb", formatWeight(weightKg, WeightUnit.LB), "$pounds lb: exact")
            assertEquals("$pounds lb", formatWeight(weightKg.nextUp(), WeightUnit.LB), "$pounds lb: above")
        }
    }

    @Test
    fun invalidWeightsCannotBecomeMisleadingLabels() {
        for (weight in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> { formatWeight(weight, WeightUnit.KG) }
            assertFailsWith<IllegalArgumentException> { WeightUnit.LB.toKilograms(weight) }
        }
    }
}
