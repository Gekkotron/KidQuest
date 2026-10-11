package com.gekkotron.kidquest.data

import com.gekkotron.kidquest.domain.LevelCalculator
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

class PointRepository(
    private val db: KidQuestDatabase,
    private val updater: GoalStatusUpdater,
    private val bus: AppEventBus,
    private val clock: Clock,
) {
    private val points = db.pointEventDao()
    private val goals = db.goalDao()
    private val children = db.childDao()

    /** Awards (delta > 0) or penalises (delta < 0) a child. Returns the new event id. */
    suspend fun apply(childId: Long, delta: Int, label: String, actionId: Long? = null): Long {
        require(delta != 0) { "delta must not be 0" }
        val type = if (delta > 0) PointEventType.AWARD else PointEventType.PENALTY
        val (id, events) = db.tx {
            val lifetimeBefore = points.lifetime(childId)
            val id = points.insert(
                PointEvent(
                    childId = childId, delta = delta, type = type, actionId = actionId,
                    label = label, timestamp = clock.now().toEpochMilliseconds(),
                )
            )
            val events = updater.refreshForChildren(listOf(childId)).toMutableList()
            if (type == PointEventType.AWARD) {
                val newLevel = LevelCalculator.levelFor(lifetimeBefore + delta)
                if (newLevel > LevelCalculator.levelFor(lifetimeBefore)) {
                    events += AppEvent.LevelUp(childId, children.get(childId)?.name.orEmpty(), newLevel)
                }
            }
            id to events
        }
        events.forEach(bus::emit)
        return id
    }

    /** Undoes one event. Undoing any REDEEM event undoes the whole redemption and puts the goal back to ACHIEVED. */
    suspend fun undo(eventId: Long) {
        val events = db.tx {
            val event = points.get(eventId) ?: return@tx emptyList()
            if (event.undone) return@tx emptyList()
            val goalId = event.goalId
            if (event.type == PointEventType.REDEEM && goalId != null) {
                points.setRedeemUndone(goalId, true)
                goals.get(goalId)?.let { goals.update(it.copy(status = GoalStatus.ACHIEVED, redeemedAt = null)) }
                updater.refreshForChildren(goals.memberIds(goalId))
            } else {
                points.setUndone(eventId, true)
                updater.refreshForChildren(listOf(event.childId))
            }
        }
        events.forEach(bus::emit)
    }

    /** Re-applies an undone AWARD/PENALTY. Returns false for REDEEM events or unknown ids. */
    suspend fun restore(eventId: Long): Boolean {
        var restored = false
        val events = db.tx {
            val event = points.get(eventId) ?: return@tx emptyList()
            if (event.type == PointEventType.REDEEM || !event.undone) return@tx emptyList()
            points.setUndone(eventId, false)
            restored = true
            updater.refreshForChildren(listOf(event.childId))
        }
        events.forEach(bus::emit)
        return restored
    }

    fun history(childId: Long): Flow<List<PointEvent>> = points.observeHistory(childId)
}
