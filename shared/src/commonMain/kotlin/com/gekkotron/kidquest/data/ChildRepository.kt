package com.gekkotron.kidquest.data

import com.gekkotron.kidquest.domain.DayMapper
import com.gekkotron.kidquest.domain.LevelCalculator
import com.gekkotron.kidquest.domain.StreakCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

class ChildRepository(
    private val db: KidQuestDatabase,
    private val clock: Clock,
    private val timeZone: () -> TimeZone,
) {
    private val children = db.childDao()
    private val points = db.pointEventDao()

    suspend fun add(name: String, avatar: String, color: Long): Long {
        require(name.isNotBlank()) { "name must not be blank" }
        return children.insert(Child(name = name.trim(), avatar = avatar, color = color, createdAt = clock.now().toEpochMilliseconds()))
    }

    suspend fun update(child: Child) {
        require(child.name.isNotBlank()) { "name must not be blank" }
        children.update(child.copy(name = child.name.trim()))
    }

    suspend fun setArchived(childId: Long, archived: Boolean) {
        val child = children.get(childId) ?: return
        children.update(child.copy(archived = archived))
    }

    fun observeAll(): Flow<List<Child>> = children.observeAll()

    /** "Today" is evaluated on each emission, so streaks refresh whenever data changes. */
    fun observeSummaries(includeArchived: Boolean = false): Flow<List<ChildSummary>> {
        val source = if (includeArchived) children.observeAll() else children.observeActive()
        return combine(source, points.observeTotals(), points.observeAwardTimestamps()) { kids, totals, awards ->
            val tz = timeZone()
            val today = DayMapper.toLocalDate(clock.now().toEpochMilliseconds(), tz)
            val totalsById = totals.associateBy { it.childId }
            val daysById = awards.groupBy({ it.childId }, { DayMapper.toLocalDate(it.timestamp, tz) }).mapValues { it.value.toSet() }
            kids.map { child ->
                val t = totalsById[child.id]
                val lifetime = t?.lifetime ?: 0
                ChildSummary(
                    child = child,
                    balance = t?.balance ?: 0,
                    lifetime = lifetime,
                    level = LevelCalculator.levelFor(lifetime),
                    levelProgress = LevelCalculator.progress(lifetime),
                    streak = StreakCalculator.streak(daysById[child.id].orEmpty(), today),
                )
            }
        }
    }
}
