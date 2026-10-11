package com.gekkotron.kidquest.domain

import kotlin.math.floor
import kotlin.math.sqrt

/** Levels are driven by lifetime AWARD points. Level 1 starts at 0. */
object LevelCalculator {

    /** Points needed to reach [level]: floor(100 · (level−1)^1.5). Computed as 100·k·√k to stay exact on perfect squares. */
    fun threshold(level: Int): Int {
        require(level >= 1) { "level must be >= 1" }
        val k = (level - 1).toDouble()
        return floor(100.0 * k * sqrt(k)).toInt()
    }

    fun levelFor(lifetime: Int): Int {
        var level = 1
        while (threshold(level + 1) <= lifetime) level++
        return level
    }

    /** Fraction [0, 1) of the way from the current level to the next. */
    fun progress(lifetime: Int): Float {
        if (lifetime <= 0) return 0f
        val level = levelFor(lifetime)
        val start = threshold(level)
        val end = threshold(level + 1)
        return (lifetime - start).toFloat() / (end - start).toFloat()
    }
}
