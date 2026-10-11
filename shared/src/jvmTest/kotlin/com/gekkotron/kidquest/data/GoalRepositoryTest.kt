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
    fun observeGoalsReportsPerMemberProgress() = runTest {
        val lea = f.child("Léa", 1); val tom = f.child("Tom", 2)
        f.goals.create("Cinema", "🎬", 1000, setOf(lea, tom))
        f.points.apply(lea, 500, "+500")
        f.points.apply(tom, -100, "−100")
        val summary = f.goals.observeGoals().first().single()
        assertEquals(listOf("Léa", "Tom"), summary.members.map { it.name })
        assertEquals(listOf(MemberProgress(lea, 500, 0.5f), MemberProgress(tom, -100, 0f)), summary.memberProgress)
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
    fun redeemChargesEveryMemberTheFullCost() = runTest {
        val lea = f.child("Léa", 1); val tom = f.child("Tom", 2)
        val goal = f.goals.create("Cinema", "🎬", 1000, setOf(lea, tom))
        f.points.apply(lea, 1200, "+1200"); f.points.apply(tom, 1000, "+1000")
        f.goals.redeem(goal)
        assertEquals(200, f.balance(lea))
        assertEquals(0, f.balance(tom))
        assertEquals(listOf(-1000, -1000), listOf(lea, tom).map { c -> f.points.history(c).first().single { it.type == PointEventType.REDEEM }.delta })
        val g = f.goals.get(goal)!!
        assertEquals(GoalStatus.REDEEMED, g.status)
        assertEquals(f.clock.nowMs, g.redeemedAt)
    }

    @Test
    fun redeemRejectsUnreachedGoal() = runTest {
        val lea = f.child("Léa", 1); val tom = f.child("Tom", 2)
        val goal = f.goals.create("Lego", "🧱", 100, setOf(lea, tom))
        f.points.apply(lea, 300, "+300")
        assertFailsWith<IllegalStateException> { f.goals.redeem(goal) } // still ACTIVE: Tom is at 0
        assertEquals(300, f.balance(lea))
    }

    @Test
    fun startupReevaluationFlipsPooledOnlyAchievedGoalBackWithoutEvent() = runTest {
        val lea = f.child("Léa", 1); val tom = f.child("Tom", 2)
        val goal = f.rawGoal(100, lea, tom)
        f.db.goalDao().update(f.db.goalDao().get(goal)!!.copy(status = GoalStatus.ACHIEVED, achievedAt = 5))
        f.db.pointEventDao().insert(PointEvent(childId = lea, delta = 305, type = PointEventType.AWARD, label = "x", timestamp = 1))
        f.db.pointEventDao().insert(PointEvent(childId = tom, delta = -30, type = PointEventType.AWARD, label = "x", timestamp = 1))
        f.bus.events.test {
            f.goals.reevaluateOpenGoals()
            assertEquals(GoalStatus.ACTIVE, f.status(goal))
            expectNoEvents()
        }
    }

    @Test
    fun startupReevaluationPromotesReachedGoalSilently() = runTest {
        val lea = f.child()
        val goal = f.rawGoal(100, lea)
        f.db.pointEventDao().insert(PointEvent(childId = lea, delta = 100, type = PointEventType.AWARD, label = "x", timestamp = 1))
        f.bus.events.test {
            f.goals.reevaluateOpenGoals()
            assertEquals(GoalStatus.ACHIEVED, f.status(goal))
            expectNoEvents()
        }
    }

    @Test
    fun redeemDropsOtherGoalsOfMembersBackToActive() = runTest {
        val lea = f.child()
        val big = f.goals.create("Lego", "🧱", 1000, setOf(lea))
        val small = f.goals.create("Ice cream", "🍦", 500, setOf(lea))
        f.points.apply(lea, 1000, "+1000")
        assertEquals(GoalStatus.ACHIEVED, f.status(small))
        f.goals.redeem(big)
        assertEquals(GoalStatus.ACTIVE, f.status(small))
    }

    @Test
    fun undoingAnyRedeemEventUndoesWholeRedemption() = runTest {
        val lea = f.child("Léa", 1); val tom = f.child("Tom", 2)
        val goal = f.goals.create("Cinema", "🎬", 1000, setOf(lea, tom))
        f.points.apply(lea, 1600, "+1600"); f.points.apply(tom, 1400, "+1400")
        f.goals.redeem(goal)
        val redeemEvent = f.points.history(tom).first().first { it.type == PointEventType.REDEEM }
        f.points.undo(redeemEvent.id)
        assertEquals(1600, f.balance(lea)); assertEquals(1400, f.balance(tom))
        assertEquals(GoalStatus.ACHIEVED, f.status(goal))
        assertNull(f.goals.get(goal)!!.redeemedAt)
    }

    @Test
    fun editingAchievedGoalAboveProgress_revertsToActiveWithoutEvent() = runTest {
        val lea = f.child("Léa", 1); val tom = f.child("Tom", 2)
        val goal = f.goals.create("Cinema", "🎬", 1000, setOf(lea, tom))
        f.points.apply(lea, 1000, "+1000"); f.points.apply(tom, 1000, "+1000")
        assertEquals(GoalStatus.ACHIEVED, f.status(goal))
        f.bus.events.test {
            f.goals.update(goal, "Cinema", "🎬", 1500, setOf(lea, tom))
            assertEquals(GoalStatus.ACTIVE, f.status(goal))
            f.goals.update(goal, "Cinema", "🎬", 1200, setOf(lea)) // 1000 < 1200
            assertEquals(GoalStatus.ACTIVE, f.status(goal))
            expectNoEvents()
        }
    }

    @Test
    fun restartCreatesFreshActiveCopy() = runTest {
        val lea = f.child()
        val goal = f.goals.create("Lego", "🧱", 100, setOf(lea))
        f.points.apply(lea, 100, "+100")
        f.goals.redeem(goal)
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
        f.goals.redeem(redeemed)
        assertFailsWith<IllegalStateException> { f.goals.delete(redeemed) }
    }
}
