package com.gekkotron.kidquest.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChildRepositoryTest {
    private val f = Fixture()

    @AfterTest fun tearDown() = f.db.close()

    private fun at(day: Int, hour: Int) = LocalDateTime(2026, 10, day, hour, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()

    @Test
    fun summaryComputesBalanceLevelAndStreak() = runTest {
        val lea = f.children.add("Léa", "🦊", 0xFFE57373)
        f.clock.nowMs = at(10, 18); f.points.apply(lea, 100, "+100")
        f.clock.nowMs = at(11, 9); f.points.apply(lea, 50, "+50"); f.points.apply(lea, -30, "−30")
        val s = f.children.observeSummaries().first().single()
        assertEquals(120, s.balance)
        assertEquals(150, s.lifetime)
        assertEquals(2, s.level)
        assertEquals(2, s.streak)
    }

    @Test
    fun penaltyAloneDoesNotKeepStreak() = runTest {
        val lea = f.children.add("Léa", "🦊", 0xFFE57373)
        f.clock.nowMs = at(11, 9); f.points.apply(lea, -10, "−10")
        assertEquals(0, f.children.observeSummaries().first().single().streak)
    }

    @Test
    fun childWithoutEventsHasZeroes() = runTest {
        f.children.add("Tom", "🐻", 0xFF64B5F6)
        val s = f.children.observeSummaries().first().single()
        assertEquals(listOf(0, 0, 1, 0), listOf(s.balance, s.lifetime, s.level, s.streak))
    }

    @Test
    fun archivedChildHiddenButStillMustReachSharedGoalCost() = runTest {
        val lea = f.children.add("Léa", "🦊", 0xFFE57373)
        val tom = f.children.add("Tom", "🐻", 0xFF64B5F6)
        val goal = f.goals.create("Cinema", "🎬", 100, setOf(lea, tom))
        f.points.apply(tom, 100, "+100")
        f.children.setArchived(tom, true)
        assertEquals(listOf("Léa"), f.children.observeSummaries().first().map { it.child.name })
        assertEquals(2, f.children.observeSummaries(includeArchived = true).first().size)
        f.points.apply(lea, 60, "+60")
        assertEquals(GoalStatus.ACTIVE, f.status(goal))
        f.points.apply(lea, 40, "+40")
        assertEquals(GoalStatus.ACHIEVED, f.status(goal))
    }

    @Test
    fun blankNameRejectedAndNameTrimmed() = runTest {
        assertFailsWith<IllegalArgumentException> { f.children.add("   ", "🦊", 0) }
        val id = f.children.add("  Léa ", "🦊", 0)
        assertEquals("Léa", f.db.childDao().get(id)!!.name)
    }
}
