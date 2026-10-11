package com.gekkotron.kidquest.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseTest {
    private val db = testDatabase()

    @AfterTest fun tearDown() = db.close()

    @Test
    fun seedsActionsAndQuickAmounts() = runTest {
        val actions = db.settingsDao().observeActions().first()
        assertEquals(listOf(50, 25, 100, 100, 20), actions.map { it.points })
        assertEquals("Wipe the table", actions.first().label)
        assertEquals(listOf(10, 20, 100, -10), db.settingsDao().observeQuickAmounts().first().map { it.points })
        assertEquals(emptyList(), db.childDao().observeActive().first())
    }

    @Test
    fun frenchSeedWhenSystemIsFrench() = runTest {
        val fr = testDatabase("fr-FR")
        assertEquals("Essuyer la table", fr.settingsDao().observeActions().first().first().label)
        fr.close()
    }

    @Test
    fun balanceAndLifetimeIgnoreUndoneEvents() = runTest {
        val childId = db.childDao().insert(Child(name = "Léa", avatar = "🦊", color = 0xFFE57373, createdAt = 1))
        val events = db.pointEventDao()
        events.insert(PointEvent(childId = childId, delta = 50, type = PointEventType.AWARD, label = "a", timestamp = 1))
        events.insert(PointEvent(childId = childId, delta = -20, type = PointEventType.PENALTY, label = "p", timestamp = 2))
        events.insert(PointEvent(childId = childId, delta = 30, type = PointEventType.AWARD, label = "u", timestamp = 3, undone = true))

        assertEquals(30, events.balance(childId))
        assertEquals(50, events.lifetime(childId))
        assertEquals(listOf(ChildTotals(childId, 30, 50)), events.observeTotals().first())
    }

    @Test
    fun goalMembersLoadWithGoal() = runTest {
        val goals = db.goalDao()
        val goalId = goals.insert(Goal(title = "Cinema", icon = "🎬", cost = 2000, createdAt = 1))
        goals.insertMembers(listOf(GoalMember(goalId, 1), GoalMember(goalId, 2)))
        val loaded = goals.observeAll().first().single()
        assertEquals(setOf(1L, 2L), loaded.members.map { it.childId }.toSet())
    }
}
