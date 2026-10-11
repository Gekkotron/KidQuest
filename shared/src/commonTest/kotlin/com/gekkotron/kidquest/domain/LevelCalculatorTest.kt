package com.gekkotron.kidquest.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class LevelCalculatorTest {
    @Test
    fun thresholdsFollowCurve() {
        assertEquals(0, LevelCalculator.threshold(1))
        assertEquals(100, LevelCalculator.threshold(2))
        assertEquals(282, LevelCalculator.threshold(3))
        assertEquals(519, LevelCalculator.threshold(4))
        assertEquals(800, LevelCalculator.threshold(5))
        assertEquals(2700, LevelCalculator.threshold(10)) // perfect square: no float drift
    }

    @Test
    fun levelAtExactBoundaries() {
        assertEquals(1, LevelCalculator.levelFor(0))
        assertEquals(1, LevelCalculator.levelFor(99))
        assertEquals(2, LevelCalculator.levelFor(100))
        assertEquals(2, LevelCalculator.levelFor(281))
        assertEquals(3, LevelCalculator.levelFor(282))
        assertEquals(10, LevelCalculator.levelFor(2700))
    }

    @Test
    fun negativeLifetimeIsLevelOne() {
        assertEquals(1, LevelCalculator.levelFor(-50))
        assertEquals(0f, LevelCalculator.progress(-50))
    }

    @Test
    fun progressWithinLevel() {
        assertEquals(0f, LevelCalculator.progress(0))
        assertEquals(0.5f, LevelCalculator.progress(50))
        assertEquals(0f, LevelCalculator.progress(100))
        // level 2 spans 100..282 (182 points); 191 is exactly halfway
        assertEquals(0.5f, LevelCalculator.progress(191))
    }
}
