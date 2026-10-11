package com.gekkotron.kidquest.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

object StreakCalculator {
    /** Consecutive days with an award, ending today or (if today has none yet) yesterday. */
    fun streak(awardDays: Set<LocalDate>, today: LocalDate): Int {
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        var day = when {
            today in awardDays -> today
            yesterday in awardDays -> yesterday
            else -> return 0
        }
        var count = 0
        while (day in awardDays) {
            count++
            day = day.minus(1, DateTimeUnit.DAY)
        }
        return count
    }
}
