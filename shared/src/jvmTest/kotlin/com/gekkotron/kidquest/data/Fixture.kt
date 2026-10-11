package com.gekkotron.kidquest.data

import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

class FixedClock(var nowMs: Long = 1_000_000) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(nowMs)
}

class Fixture(language: String = "en") {
    val clock = FixedClock()
    val timeZone = TimeZone.UTC
    val db = testDatabase(language)
    val bus = AppEventBus()
    val updater = GoalStatusUpdater(db.goalDao(), db.pointEventDao(), clock)
    val points = PointRepository(db, updater, bus, clock)
    val goals = GoalRepository(db, updater, bus, clock)
    val children = ChildRepository(db, clock) { timeZone }
    val settings = SettingsRepository(db)

    suspend fun child(name: String = "Léa", createdAt: Long = 1): Long =
        db.childDao().insert(Child(name = name, avatar = "🦊", color = 0xFFE57373, createdAt = createdAt))

    suspend fun rawGoal(cost: Int, vararg members: Long, title: String = "Lego"): Long {
        val id = db.goalDao().insert(Goal(title = title, icon = "🧱", cost = cost, createdAt = clock.nowMs))
        db.goalDao().insertMembers(members.map { GoalMember(id, it) })
        return id
    }

    suspend fun status(goalId: Long): GoalStatus = db.goalDao().get(goalId)!!.status
    suspend fun balance(childId: Long): Int = db.pointEventDao().balance(childId)
}
