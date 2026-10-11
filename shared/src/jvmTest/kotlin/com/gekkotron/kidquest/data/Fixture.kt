package com.gekkotron.kidquest.data

import kotlin.time.Clock
import kotlin.time.Instant

class FixedClock(var nowMs: Long = 1_000_000) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(nowMs)
}

class Fixture(language: String = "en") {
    val clock = FixedClock()
    val db = testDatabase(language)
    val bus = AppEventBus()
    val updater = GoalStatusUpdater(db.goalDao(), db.pointEventDao(), clock)
    val points = PointRepository(db, updater, bus, clock)

    suspend fun child(name: String = "Léa", createdAt: Long = 1): Long =
        db.childDao().insert(Child(name = name, avatar = "🦊", color = 0xFFE57373, createdAt = createdAt))

    /** Inserts a goal directly (GoalRepository arrives in Task 7). */
    suspend fun rawGoal(cost: Int, vararg members: Long, title: String = "Lego"): Long {
        val id = db.goalDao().insert(Goal(title = title, icon = "🧱", cost = cost, createdAt = clock.nowMs))
        db.goalDao().insertMembers(members.map { GoalMember(id, it) })
        return id
    }

    suspend fun status(goalId: Long): GoalStatus = db.goalDao().get(goalId)!!.status
    suspend fun balance(childId: Long): Int = db.pointEventDao().balance(childId)
}
