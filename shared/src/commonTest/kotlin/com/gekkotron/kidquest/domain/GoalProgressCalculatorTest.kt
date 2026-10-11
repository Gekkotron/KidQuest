package com.gekkotron.kidquest.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GoalProgressCalculatorTest {
    @Test fun soloHalfway() = assertEquals(0.5f, GoalProgressCalculator.progress(listOf(500), 1000))
    @Test fun sharedSumsMembers() = assertEquals(0.75f, GoalProgressCalculator.progress(listOf(500, 250), 1000))
    @Test fun negativeBalanceCountsAsZero() = assertEquals(0.3f, GoalProgressCalculator.progress(listOf(300, -200), 1000))
    @Test fun clampedToOne() = assertEquals(1f, GoalProgressCalculator.progress(listOf(5000), 1000))
    @Test fun allNegativeIsZero() = assertEquals(0f, GoalProgressCalculator.progress(listOf(-10, -5), 100))

    @Test
    fun reachedAtExactCost() {
        assertTrue(GoalProgressCalculator.isReached(listOf(600, 400), 1000))
        assertFalse(GoalProgressCalculator.isReached(listOf(600, 399), 1000))
    }

    @Test
    fun negativeMemberDoesNotDragOthersDown() {
        // 1000 + max(0,-300) = 1000 → reached
        assertTrue(GoalProgressCalculator.isReached(listOf(1000, -300), 1000))
        assertEquals(1000, GoalProgressCalculator.contributed(listOf(1000, -300)))
    }
}
