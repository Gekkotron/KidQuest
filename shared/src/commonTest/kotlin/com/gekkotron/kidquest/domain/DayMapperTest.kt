package com.gekkotron.kidquest.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class DayMapperTest {
    private val paris = TimeZone.of("Europe/Paris")
    private fun ms(y: Int, mo: Int, d: Int, h: Int, mi: Int) =
        LocalDateTime(y, mo, d, h, mi).toInstant(paris).toEpochMilliseconds()

    @Test
    fun beforeAndAfterMidnightAreTwoDays() {
        assertEquals(LocalDate(2026, 10, 10), DayMapper.toLocalDate(ms(2026, 10, 10, 23, 59), paris))
        assertEquals(LocalDate(2026, 10, 11), DayMapper.toLocalDate(ms(2026, 10, 11, 0, 1), paris))
    }

    @Test
    fun dstFallBackDayStaysOneDay() {
        // 2026-10-25: Paris clocks go back 03:00 → 02:00
        assertEquals(LocalDate(2026, 10, 25), DayMapper.toLocalDate(ms(2026, 10, 25, 0, 30), paris))
        assertEquals(LocalDate(2026, 10, 25), DayMapper.toLocalDate(ms(2026, 10, 25, 23, 30), paris))
        assertEquals(LocalDate(2026, 10, 26), DayMapper.toLocalDate(ms(2026, 10, 26, 0, 10), paris))
    }

    @Test
    fun dstSpringForwardDayStaysOneDay() {
        // 2026-03-29: Paris clocks go forward 02:00 → 03:00
        assertEquals(LocalDate(2026, 3, 29), DayMapper.toLocalDate(ms(2026, 3, 29, 1, 30), paris))
        assertEquals(LocalDate(2026, 3, 29), DayMapper.toLocalDate(ms(2026, 3, 29, 23, 59), paris))
    }
}
