package com.hulkdx.core5x5.feature.settings.domain

import com.hulkdx.core5x5.core.preferences.domain.TrainingPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class RestDurationInputTest {
    @Test
    fun positiveWholeSecondsIncludeValuesLongerThanOneHour() {
        for (seconds in listOf(1L, 180L, 3_601L, 86_400L, TrainingPreferences.MAX_REST_DURATION_SECONDS)) {
            assertEquals(seconds, parseRestDurationSeconds(seconds.toString()))
        }
        assertEquals(91L, parseRestDurationSeconds(" 0091 "))
    }

    @Test
    fun emptyFractionalNegativeAndOverflowingInputsAreRejected() {
        for (input in listOf("", " ", "0", "-1", "+1", "1.5", "1e3", "NaN", "9223372036854776", "9223372036854775808")) {
            assertNull(parseRestDurationSeconds(input), input)
        }
    }
}
