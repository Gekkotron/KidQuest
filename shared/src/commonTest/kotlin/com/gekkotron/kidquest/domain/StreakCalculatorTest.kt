package com.gekkotron.kidquest.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class StreakCalculatorTest {
    private val today = LocalDate(2026, 10, 11)
    private fun d(day: Int) = LocalDate(2026, 10, day)

    @Test fun emptyIsZero() = assertEquals(0, StreakCalculator.streak(emptySet(), today))
    @Test fun todayOnlyIsOne() = assertEquals(1, StreakCalculator.streak(setOf(d(11)), today))
    @Test fun yesterdayOnlyStillCounts() = assertEquals(1, StreakCalculator.streak(setOf(d(10)), today))
    @Test fun twoDaysAgoOnlyIsBroken() = assertEquals(0, StreakCalculator.streak(setOf(d(9)), today))
    @Test fun consecutiveRunEndingToday() = assertEquals(3, StreakCalculator.streak(setOf(d(9), d(10), d(11)), today))
    @Test fun gapStopsCount() = assertEquals(2, StreakCalculator.streak(setOf(d(6), d(7), d(10), d(11)), today))
    @Test fun runEndingYesterday() = assertEquals(2, StreakCalculator.streak(setOf(d(9), d(10)), today))

    @Test
    fun crossesMonthBoundary() {
        val days = setOf(LocalDate(2026, 9, 30), LocalDate(2026, 10, 1))
        assertEquals(2, StreakCalculator.streak(days, LocalDate(2026, 10, 1)))
    }
}
