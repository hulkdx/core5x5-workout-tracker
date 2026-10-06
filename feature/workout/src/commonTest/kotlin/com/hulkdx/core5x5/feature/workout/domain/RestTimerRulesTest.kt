package com.hulkdx.core5x5.feature.workout.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

internal class RestTimerRulesTest {
    @Test
    fun idleRestDoesNotReadTheClockOrStartFromAnExtension() {
        val rules = RestTimerRules { error("Idle rest must not read the clock.") }

        assertEquals(RestTimerState.Idle, rules.state(null))
        assertNull(rules.extend(null, 30_000L))
        assertNull(rules.skip())
    }

    @Test
    fun startUsesTheSuppliedDurationAndReadsTheClockOnce() {
        var clockReads = 0
        val rules = RestTimerRules {
            clockReads++
            1_000L
        }

        assertEquals(RestTimer(91_000L), rules.start(90_000L))
        assertEquals(1, clockReads)
    }

    @Test
    fun countdownDerivesElapsedTimeFromTheDeadlineWithoutTicking() {
        val clock = TestClock(1_000L)
        val rules = RestTimerRules(clock::now)
        val timer = rules.start(120_000L)

        assertEquals(RestTimerState.Running(120_000L), rules.state(timer))
        clock.epochMillis += 45_123L
        assertEquals(RestTimerState.Running(74_877L), rules.state(timer))
        assertEquals(RestTimer(121_000L), timer)
    }

    @Test
    fun restExpiresExactlyAtTheDeadlineAndStaysExpiredAfterward() {
        val clock = TestClock(1_000L)
        val rules = RestTimerRules(clock::now)
        val timer = rules.start(30_000L)

        clock.epochMillis = 30_999L
        assertEquals(RestTimerState.Running(1L), rules.state(timer))
        clock.epochMillis = 31_000L
        assertEquals(RestTimerState.Expired, rules.state(timer))
        clock.epochMillis = 90_000L
        assertEquals(RestTimerState.Expired, rules.state(timer))
        assertEquals(RestTimer(31_000L), timer)
    }

    @Test
    fun startingAgainReplacesRunningRestWithTheFullNewDuration() {
        val clock = TestClock(1_000L)
        val rules = RestTimerRules(clock::now)
        var timer: RestTimer? = rules.start(120_000L)

        clock.epochMillis = 61_000L
        assertEquals(RestTimerState.Running(60_000L), rules.state(timer))
        timer = rules.start(90_000L)

        assertEquals(RestTimer(151_000L), timer)
        assertEquals(RestTimerState.Running(90_000L), rules.state(timer))
    }

    @Test
    fun startingAgainReplacesExpiredRestWithTheFullNewDuration() {
        val clock = TestClock(1_000L)
        val rules = RestTimerRules(clock::now)
        var timer: RestTimer? = rules.start(30_000L)

        clock.epochMillis = 41_000L
        assertEquals(RestTimerState.Expired, rules.state(timer))
        timer = rules.start(120_000L)

        assertEquals(RestTimer(161_000L), timer)
        assertEquals(RestTimerState.Running(120_000L), rules.state(timer))
    }

    @Test
    fun skipClearsRunningAndExpiredRestAndIsSafeToRepeat() {
        val clock = TestClock(1_000L)
        val rules = RestTimerRules(clock::now)
        var timer: RestTimer? = rules.start(30_000L)

        assertEquals(RestTimerState.Running(30_000L), rules.state(timer))
        timer = rules.skip()
        assertEquals(RestTimerState.Idle, rules.state(timer))
        timer = rules.skip()
        assertEquals(RestTimerState.Idle, rules.state(timer))

        timer = rules.start(30_000L)
        clock.epochMillis = 31_000L
        assertEquals(RestTimerState.Expired, rules.state(timer))
        timer = rules.skip()
        assertEquals(RestTimerState.Idle, rules.state(timer))
    }

    @Test
    fun extensionPreservesRemainingRestInsteadOfRestartingTheTimer() {
        val clock = TestClock(1_000L)
        val rules = RestTimerRules(clock::now)
        val timer = rules.start(120_000L)

        clock.epochMillis = 61_000L
        val extended = rules.extend(timer, 30_000L)

        assertEquals(RestTimer(151_000L), extended)
        assertEquals(RestTimerState.Running(90_000L), rules.state(extended))
        assertEquals(RestTimer(121_000L), timer)
    }

    @Test
    fun repeatedExtensionsAccumulateOnTheDeadline() {
        val clock = TestClock(1_000L)
        val rules = RestTimerRules(clock::now)
        var timer: RestTimer? = rules.start(60_000L)

        timer = rules.extend(timer, 30_000L)
        clock.epochMillis += 10_000L
        timer = rules.extend(timer, 30_000L)

        assertEquals(RestTimer(121_000L), timer)
        assertEquals(RestTimerState.Running(110_000L), rules.state(timer))
    }

    @Test
    fun extendingAtTheDeadlineStartsTheExtensionFromNow() {
        val clock = TestClock(1_000L)
        val rules = RestTimerRules(clock::now)
        val timer = rules.start(30_000L)

        clock.epochMillis = 31_000L
        val extended = rules.extend(timer, 30_000L)

        assertEquals(RestTimer(61_000L), extended)
        assertEquals(RestTimerState.Running(30_000L), rules.state(extended))
    }

    @Test
    fun extendingLongAfterExpiryGivesTheFullExtension() {
        val clock = TestClock(1_000L)
        val rules = RestTimerRules(clock::now)
        val timer = rules.start(30_000L)

        clock.epochMillis = 101_000L
        val extended = rules.extend(timer, 30_000L)

        assertEquals(RestTimer(131_000L), extended)
        assertEquals(RestTimerState.Running(30_000L), rules.state(extended))
    }

    @Test
    fun clockJumpsRecomputeRemainingRestWithoutMovingTheDeadline() {
        val clock = TestClock(10_000L)
        val rules = RestTimerRules(clock::now)
        val timer = rules.start(60_000L)

        clock.epochMillis = 5_000L
        assertEquals(RestTimerState.Running(65_000L), rules.state(timer))
        clock.epochMillis = 100_000L
        assertEquals(RestTimerState.Expired, rules.state(timer))
        clock.epochMillis = 20_000L
        assertEquals(RestTimerState.Running(50_000L), rules.state(timer))
        assertEquals(RestTimer(70_000L), timer)
    }

    @Test
    fun independentTimerValuesDoNotShareOrMutateDeadlines() {
        val clock = TestClock(1_000L)
        val rules = RestTimerRules(clock::now)
        val first = rules.start(30_000L)

        clock.epochMillis = 11_000L
        val second = rules.start(120_000L)
        val extended = rules.extend(second, 30_000L)

        assertEquals(RestTimerState.Running(20_000L), rules.state(first))
        assertEquals(RestTimerState.Running(120_000L), rules.state(second))
        assertEquals(RestTimerState.Running(150_000L), rules.state(extended))
    }

    @Test
    fun nonPositiveDurationsAreRejectedBeforeReadingTheClock() {
        val rules = RestTimerRules { error("Invalid durations must not read the clock.") }
        val timer = RestTimer(1_000L)

        for (duration in listOf(0L, -1L, Long.MIN_VALUE)) {
            assertFailsWith<IllegalArgumentException> { rules.start(duration) }
            assertFailsWith<IllegalArgumentException> { rules.extend(timer, duration) }
            assertFailsWith<IllegalArgumentException> { rules.extend(null, duration) }
        }
    }

    @Test
    fun deadlineOverflowIsRejectedWithoutWrappingOrMutatingRest() {
        val clock = TestClock(Long.MAX_VALUE - 10L)
        val rules = RestTimerRules(clock::now)
        val timer = rules.start(10L)

        assertEquals(RestTimer(Long.MAX_VALUE), timer)
        assertFailsWith<IllegalArgumentException> { rules.start(11L) }
        assertFailsWith<IllegalArgumentException> { rules.extend(timer, 1L) }
        assertEquals(RestTimer(Long.MAX_VALUE), timer)
        assertEquals(RestTimerState.Running(10L), rules.state(timer))
    }

    @Test
    fun aVeryLargeBackwardClockJumpSaturatesRemainingTimeInsteadOfWrapping() {
        val clock = TestClock(Long.MAX_VALUE - 1L)
        val rules = RestTimerRules(clock::now)
        val timer = rules.start(1L)

        clock.epochMillis = Long.MIN_VALUE

        assertEquals(RestTimerState.Running(Long.MAX_VALUE), rules.state(timer))
    }

    private class TestClock(var epochMillis: Long) {
        fun now(): Long = epochMillis
    }
}
