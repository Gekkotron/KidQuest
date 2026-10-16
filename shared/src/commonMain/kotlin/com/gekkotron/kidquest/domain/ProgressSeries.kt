package com.gekkotron.kidquest.domain

import com.gekkotron.kidquest.data.PointEvent
import com.gekkotron.kidquest.data.PointEventType
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus

/**
 * Component magnitudes (always ≥ 0) for one day.
 * [award] stacks above the baseline; [penalty] then [redeem] stack below it.
 */
data class DailyProgress(
    val date: LocalDate,
    val award: Int = 0,
    val penalty: Int = 0,
    val redeem: Int = 0,
) {
    val delta: Int get() = award - penalty - redeem
}

object ProgressSeries {
    /**
     * Points per day for the [days] days ending on [today] (inclusive), split by event type.
     * Undone events are ignored; empty days are kept as zeroed slots so the chart has a stable x-axis.
     */
    fun daily(history: List<PointEvent>, today: LocalDate, timeZone: TimeZone, days: Int = 14): List<DailyProgress> {
        require(days > 0) { "days must be > 0" }
        val acc = HashMap<LocalDate, IntArray>(days)
        history.asSequence().filter { !it.undone }.forEach { e ->
            val d = DayMapper.toLocalDate(e.timestamp, timeZone)
            val slot = acc.getOrPut(d) { IntArray(3) }
            when (e.type) {
                PointEventType.AWARD -> slot[0] += e.delta
                PointEventType.PENALTY -> slot[1] += -e.delta
                PointEventType.REDEEM -> slot[2] += -e.delta
            }
        }
        return List(days) { i ->
            val date = today.minus(DatePeriod(days = days - 1 - i))
            val s = acc[date]
            DailyProgress(date, s?.get(0) ?: 0, s?.get(1) ?: 0, s?.get(2) ?: 0)
        }
    }
}
