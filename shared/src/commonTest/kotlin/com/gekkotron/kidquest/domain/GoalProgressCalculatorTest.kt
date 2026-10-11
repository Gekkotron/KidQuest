package com.gekkotron.kidquest.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GoalProgressCalculatorTest {
    @Test fun memberProgressClamped() {
        assertEquals(0.5f, GoalProgressCalculator.memberProgress(500, 1000))
        assertEquals(1f, GoalProgressCalculator.memberProgress(5000, 1000))
        assertEquals(0f, GoalProgressCalculator.memberProgress(-50, 1000))
    }

    @Test fun soloHalfway() = assertEquals(0.5f, GoalProgressCalculator.progress(listOf(500), 1000))
    @Test fun sharedIsMeanOfMemberProgress() = assertEquals(0.75f, GoalProgressCalculator.progress(listOf(1000, 500), 1000))
    @Test fun negativeBalanceCountsAsZero() = assertEquals(0.5f, GoalProgressCalculator.progress(listOf(1000, -200), 1000))
    @Test fun clampedToOne() = assertEquals(1f, GoalProgressCalculator.progress(listOf(5000), 1000))
    @Test fun allNegativeIsZero() = assertEquals(0f, GoalProgressCalculator.progress(listOf(-10, -5), 100))
    @Test fun progressIsOneOnlyWhenEveryoneIsThere() {
        assertEquals(1f, GoalProgressCalculator.progress(listOf(100, 300), 100))
        assertTrue(GoalProgressCalculator.progress(listOf(100, 99), 100) < 1f)
    }

    @Test fun soloReached() {
        assertTrue(GoalProgressCalculator.isReached(listOf(1000), 1000))
        assertFalse(GoalProgressCalculator.isReached(listOf(999), 1000))
    }

    @Test fun sharedReachedOnlyWhenEveryMemberHasFullCost() {
        assertTrue(GoalProgressCalculator.isReached(listOf(1000, 1500), 1000))
        assertFalse(GoalProgressCalculator.isReached(listOf(1000, 999), 1000))
        assertFalse(GoalProgressCalculator.isReached(listOf(600, 400), 1000), "pooling no longer counts")
    }

    @Test fun oneNegativeMemberBlocksTheGoal() =
        assertFalse(GoalProgressCalculator.isReached(listOf(305, -30), 100))

    @Test fun noMembersIsNeverReached() = assertFalse(GoalProgressCalculator.isReached(emptyList(), 100))
}
