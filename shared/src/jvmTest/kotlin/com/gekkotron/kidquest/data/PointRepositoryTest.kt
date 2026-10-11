package com.gekkotron.kidquest.data

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PointRepositoryTest {
    private val f = Fixture()

    @AfterTest fun tearDown() = f.db.close()

    @Test
    fun awardAndPenaltyAreTypedBySign() = runTest {
        val lea = f.child()
        val a = f.points.apply(lea, 20, "+20")
        val p = f.points.apply(lea, -50, "−50")
        assertEquals(PointEventType.AWARD, f.db.pointEventDao().get(a)!!.type)
        assertEquals(PointEventType.PENALTY, f.db.pointEventDao().get(p)!!.type)
        assertEquals(-30, f.balance(lea))
        assertEquals(20, f.db.pointEventDao().lifetime(lea))
    }

    @Test
    fun zeroDeltaIsRejected() = runTest {
        val lea = f.child()
        assertFailsWith<IllegalArgumentException> { f.points.apply(lea, 0, "nothing") }
    }

    @Test
    fun undoThenRestore() = runTest {
        val lea = f.child()
        val id = f.points.apply(lea, 50, "Wipe the table", actionId = 1)
        f.points.undo(id)
        assertEquals(0, f.balance(lea))
        assertTrue(f.points.history(lea).first().single().undone)
        assertTrue(f.points.restore(id))
        assertEquals(50, f.balance(lea))
    }

    @Test
    fun reachingGoalEmitsGoalAchievedOnlyOnTransition() = runTest {
        val lea = f.child()
        val goal = f.rawGoal(100, lea)
        f.bus.events.test {
            f.points.apply(lea, 60, "+60")
            f.points.apply(lea, 50, "+50")
            assertEquals(AppEvent.GoalAchieved(goal, "Lego", "🧱"), awaitItem())
            awaitItem() // LevelUp to 2 (lifetime 110)
            f.points.apply(lea, 10, "+10")
            expectNoEvents()
        }
        assertEquals(GoalStatus.ACHIEVED, f.status(goal))
    }

    @Test
    fun droppingBelowCostRevertsToActiveSilently() = runTest {
        val lea = f.child()
        val goal = f.rawGoal(100, lea)
        f.points.apply(lea, 100, "+100")
        f.bus.events.test {
            f.points.apply(lea, -10, "−10")
            expectNoEvents()
        }
        assertEquals(GoalStatus.ACTIVE, f.status(goal))
        assertEquals(null, f.db.goalDao().get(goal)!!.achievedAt)
    }

    @Test
    fun levelUpEmittedWhenCrossingThreshold() = runTest {
        val lea = f.child(name = "Léa")
        f.bus.events.test {
            f.points.apply(lea, 90, "+90")
            expectNoEvents()
            f.points.apply(lea, 20, "+20")
            assertEquals(AppEvent.LevelUp(lea, "Léa", 2), awaitItem())
        }
    }

    @Test
    fun penaltyNeverCostsALevel() = runTest {
        val lea = f.child()
        f.points.apply(lea, 150, "+150")
        f.points.apply(lea, -100, "−100")
        assertEquals(150, f.db.pointEventDao().lifetime(lea))
    }

    @Test
    fun undoLongAfterAward_restoresBalanceAndGoalStatus() = runTest {
        val lea = f.child()
        val goal = f.rawGoal(100, lea)
        val id = f.points.apply(lea, 100, "+100")
        assertEquals(GoalStatus.ACHIEVED, f.status(goal))
        f.clock.nowMs += 6 * 60 * 60 * 1000 // six hours later, snackbar long gone
        f.points.undo(id)
        assertEquals(0, f.balance(lea))
        assertEquals(GoalStatus.ACTIVE, f.status(goal))
    }

    @Test
    fun undoTwiceIsIdempotent() = runTest {
        val lea = f.child()
        val id = f.points.apply(lea, 20, "+20")
        f.points.undo(id)
        f.points.undo(id)
        assertEquals(0, f.balance(lea))
    }

    @Test
    fun redeemEventCannotBeRestored() = runTest {
        val lea = f.child()
        val redeemId = f.db.pointEventDao().insert(
            PointEvent(childId = lea, delta = -100, type = PointEventType.REDEEM, goalId = 99, label = "Lego", timestamp = 1, undone = true)
        )
        assertFalse(f.points.restore(redeemId))
    }
}
