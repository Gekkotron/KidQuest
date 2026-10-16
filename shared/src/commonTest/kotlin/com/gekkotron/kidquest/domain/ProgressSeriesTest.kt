package com.gekkotron.kidquest.domain

import com.gekkotron.kidquest.data.PointEvent
import com.gekkotron.kidquest.data.PointEventType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class ProgressSeriesTest {
    private val tz = TimeZone.UTC
    private val today = LocalDate(2026, 10, 16)

    private fun event(date: LocalDate, delta: Int, type: PointEventType = PointEventType.AWARD, undone: Boolean = false): PointEvent {
        val ts = LocalDateTime(date, kotlinx.datetime.LocalTime(12, 0)).toInstant(tz).toEpochMilliseconds()
        return PointEvent(childId = 1, delta = delta, type = type, label = "x", timestamp = ts, undone = undone)
    }

    @Test fun lengthMatchesWindowAndEndsOnToday() {
        val s = ProgressSeries.daily(emptyList(), today, tz, days = 7)
        assertEquals(7, s.size)
        assertEquals(today, s.last().date)
        assertEquals(LocalDate(2026, 10, 10), s.first().date)
    }

    @Test fun fillsEmptyDaysWithZero() {
        val s = ProgressSeries.daily(emptyList(), today, tz, days = 3)
        assertEquals(listOf(0, 0, 0), s.map { it.delta })
    }

    @Test fun sumsAwardsAndPenaltiesPerDay() {
        val s = ProgressSeries.daily(
            listOf(
                event(today, 10),
                event(today, 5),
                event(today, -2, PointEventType.PENALTY),
                event(LocalDate(2026, 10, 15), 7),
            ),
            today, tz, days = 3,
        )
        assertEquals(listOf(0, 7, 13), s.map { it.delta })
    }

    @Test fun ignoresUndoneEvents() {
        val s = ProgressSeries.daily(
            listOf(event(today, 10), event(today, 20, undone = true)),
            today, tz, days = 1,
        )
        assertEquals(listOf(10), s.map { it.delta })
    }

    @Test fun eventsOutsideWindowAreDropped() {
        val s = ProgressSeries.daily(
            listOf(event(LocalDate(2026, 10, 1), 100), event(today, 3)),
            today, tz, days = 7,
        )
        assertEquals(3, s.last().delta)
        assertEquals(3, s.sumOf { it.delta })
    }

    @Test fun componentsSplitByType() {
        val s = ProgressSeries.daily(
            listOf(
                event(today, 50),
                event(today, -20, PointEventType.PENALTY),
                event(today, -30, PointEventType.PENALTY),
                event(today, -100, PointEventType.REDEEM),
            ),
            today, tz, days = 1,
        ).single()
        assertEquals(50, s.award)
        assertEquals(50, s.penalty)
        assertEquals(100, s.redeem)
        assertEquals(-100, s.delta)
    }
}
