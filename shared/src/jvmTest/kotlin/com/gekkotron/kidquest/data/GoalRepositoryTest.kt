package com.gekkotron.kidquest.data

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GoalRepositoryTest {
    private val f = Fixture()

    @AfterTest fun tearDown() = f.db.close()

    @Test
    fun observeGoalsReportsSharedProgress() = runTest {
        val lea = f.child("Léa", 1); val tom = f.child("Tom", 2)
        f.goals.create("Cinema", "🎬", 2000, setOf(lea, tom))
        f.points.apply(lea, 500, "+500")
        f.points.apply(tom, -100, "−100")
        val summary = f.goals.observeGoals().first().single()
        assertEquals(listOf("Léa", "Tom"), summary.members.map { it.name })
        assertEquals(500, summary.contributed)
        assertEquals(0.25f, summary.progress)
    }

    @Test
    fun createRejectsInvalidInput() = runTest {
        val lea = f.child()
        assertFailsWith<IllegalArgumentException> { f.goals.create(" ", "🎬", 100, setOf(lea)) }
        assertFailsWith<IllegalArgumentException> { f.goals.create("X", "🎬", 0, setOf(lea)) }
        assertFailsWith<IllegalArgumentException> { f.goals.create("X", "🎬", 100, emptySet()) }
    }

    @Test
    fun defaultSplitOrdersByChildCreation() = runTest {
        val older = f.child("Tom", createdAt = 1)
        val younger = f.child("Léa", createdAt = 2)
        val goal = f.goals.create("Cinema", "🎬", 1001, setOf(younger, older))
        assertEquals(mapOf(older to 501, younger to 500), f.goals.defaultSplit(goal))
    }

    @Test
    fun redeemDeductsAndMarksRedeemed() = runTest {
        val lea = f.child("Léa", 1); val tom = f.child("Tom", 2)
        val goal = f.goals.create("Cinema", "🎬", 1000, setOf(lea, tom))
        f.points.apply(lea, 900, "+900"); f.points.apply(tom, 100, "+100")
        f.goals.redeem(goal, mapOf(lea to 500, tom to 500))
        assertEquals(400, f.balance(lea))
        assertEquals(-400, f.balance(tom)) // may go negative
        val g = f.goals.get(goal)!!
        assertEquals(GoalStatus.REDEEMED, g.status)
        assertEquals(f.clock.nowMs, g.redeemedAt)
    }

    @Test
    fun redeemRejectsInvalidSplitOrUnreachedGoal() = runTest {
        val lea = f.child()
        val goal = f.goals.create("Lego", "🧱", 100, setOf(lea))
        assertFailsWith<IllegalStateException> { f.goals.redeem(goal, mapOf(lea to 100)) } // still ACTIVE
        f.points.apply(lea, 100, "+100")
        assertFailsWith<IllegalArgumentException> { f.goals.redeem(goal, mapOf(lea to 90)) }
        assertEquals(GoalStatus.ACHIEVED, f.status(goal))
        assertEquals(100, f.balance(lea))
    }

    @Test
    fun redeemDropsOtherGoalsOfMembersBackToActive() = runTest {
        val lea = f.child()
        val big = f.goals.create("Lego", "🧱", 1000, setOf(lea))
        val small = f.goals.create("Ice cream", "🍦", 500, setOf(lea))
        f.points.apply(lea, 1000, "+1000")
        assertEquals(GoalStatus.ACHIEVED, f.status(small))
        f.goals.redeem(big, mapOf(lea to 1000))
        assertEquals(GoalStatus.ACTIVE, f.status(small))
    }

    @Test
    fun undoingAnyRedeemEventUndoesWholeRedemption() = runTest {
        val lea = f.child("Léa", 1); val tom = f.child("Tom", 2)
        val goal = f.goals.create("Cinema", "🎬", 1000, setOf(lea, tom))
        f.points.apply(lea, 600, "+600"); f.points.apply(tom, 400, "+400")
        f.goals.redeem(goal, mapOf(lea to 500, tom to 500))
        val redeemEvent = f.points.history(tom).first().first { it.type == PointEventType.REDEEM }
        f.points.undo(redeemEvent.id)
        assertEquals(600, f.balance(lea)); assertEquals(400, f.balance(tom))
        assertEquals(GoalStatus.ACHIEVED, f.status(goal))
        assertNull(f.goals.get(goal)!!.redeemedAt)
    }

    @Test
    fun editingAchievedGoalAboveProgress_revertsToActiveWithoutEvent() = runTest {
        val lea = f.child("Léa", 1); val tom = f.child("Tom", 2)
        val goal = f.goals.create("Cinema", "🎬", 1000, setOf(lea, tom))
        f.points.apply(lea, 600, "+600"); f.points.apply(tom, 400, "+400")
        assertEquals(GoalStatus.ACHIEVED, f.status(goal))
        f.bus.events.test {
            f.goals.update(goal, "Cinema", "🎬", 1500, setOf(lea, tom))
            assertEquals(GoalStatus.ACTIVE, f.status(goal))
            f.goals.update(goal, "Cinema", "🎬", 1000, setOf(lea)) // removing Tom: 600 < 1000
            assertEquals(GoalStatus.ACTIVE, f.status(goal))
            expectNoEvents()
        }
    }

    @Test
    fun restartCreatesFreshActiveCopy() = runTest {
        val lea = f.child()
        val goal = f.goals.create("Lego", "🧱", 100, setOf(lea))
        f.points.apply(lea, 100, "+100")
        f.goals.redeem(goal, mapOf(lea to 100))
        val copy = f.goals.restart(goal)
        val g = f.goals.get(copy)!!
        assertEquals(listOf("Lego", "🧱"), listOf(g.title, g.icon))
        assertEquals(100, g.cost)
        assertEquals(GoalStatus.ACTIVE, g.status)
        assertEquals(listOf(lea), f.goals.memberIds(copy))
    }

    @Test
    fun deleteOnlyForUnredeemedGoals() = runTest {
        val lea = f.child()
        val open = f.goals.create("Lego", "🧱", 100, setOf(lea))
        f.goals.delete(open)
        assertNull(f.goals.get(open))
        assertEquals(emptyList(), f.goals.memberIds(open))

        val redeemed = f.goals.create("Ice", "🍦", 10, setOf(lea))
        f.points.apply(lea, 10, "+10")
        f.goals.redeem(redeemed, mapOf(lea to 10))
        assertFailsWith<IllegalStateException> { f.goals.delete(redeemed) }
    }
}
