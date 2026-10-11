package com.gekkotron.kidquest.data

import com.gekkotron.kidquest.domain.GoalProgressCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.time.Clock

class GoalRepository(
    private val db: KidQuestDatabase,
    private val updater: GoalStatusUpdater,
    private val bus: AppEventBus,
    private val clock: Clock,
) {
    private val goals = db.goalDao()
    private val children = db.childDao()
    private val points = db.pointEventDao()

    fun observeGoals(): Flow<List<GoalSummary>> =
        combine(goals.observeAll(), children.observeAll(), points.observeTotals()) { all, kids, totals ->
            val kidsById = kids.associateBy { it.id }
            val balances = totals.associate { it.childId to it.balance }
            all.map { gwm ->
                val members = gwm.members.mapNotNull { kidsById[it.childId] }.sortedWith(compareBy({ it.createdAt }, { it.id }))
                val memberBalances = members.map { balances[it.id] ?: 0 }
                GoalSummary(
                    goal = gwm.goal,
                    members = members,
                    memberProgress = members.mapIndexed { i, kid ->
                        MemberProgress(kid.id, memberBalances[i], GoalProgressCalculator.memberProgress(memberBalances[i], gwm.goal.cost))
                    },
                    progress = GoalProgressCalculator.progress(memberBalances, gwm.goal.cost),
                )
            }
        }

    suspend fun get(goalId: Long): Goal? = goals.get(goalId)

    suspend fun memberIds(goalId: Long): List<Long> = goals.memberIds(goalId)

    suspend fun create(title: String, icon: String, cost: Int, memberIds: Set<Long>): Long {
        validate(title, cost, memberIds)
        val (id, event) = db.tx {
            val id = goals.insert(Goal(title = title.trim(), icon = icon, cost = cost, createdAt = clock.now().toEpochMilliseconds()))
            goals.insertMembers(memberIds.map { GoalMember(id, it) })
            id to updater.refreshGoal(id)
        }
        event?.let(bus::emit)
        return id
    }

    suspend fun update(goalId: Long, title: String, icon: String, cost: Int, memberIds: Set<Long>) {
        validate(title, cost, memberIds)
        val event = db.tx {
            val goal = checkNotNull(goals.get(goalId)) { "goal $goalId not found" }
            check(goal.status != GoalStatus.REDEEMED) { "redeemed goals cannot be edited" }
            goals.update(goal.copy(title = title.trim(), icon = icon, cost = cost))
            goals.deleteMembers(goalId)
            goals.insertMembers(memberIds.map { GoalMember(goalId, it) })
            updater.refreshGoal(goalId)
        }
        event?.let(bus::emit)
    }

    suspend fun delete(goalId: Long) {
        db.tx {
            val goal = goals.get(goalId) ?: return@tx
            check(goal.status != GoalStatus.REDEEMED) { "redeemed goals are kept for history" }
            goals.delete(goalId) // goal_member rows cascade
        }
    }

    /** Every member pays the full cost; balances may go negative. */
    suspend fun redeem(goalId: Long) {
        val events = db.tx {
            val goal = checkNotNull(goals.get(goalId)) { "goal $goalId not found" }
            check(goal.status == GoalStatus.ACHIEVED) { "only achieved goals can be redeemed" }
            val members = goals.memberIds(goalId)
            val now = clock.now().toEpochMilliseconds()
            members.forEach { childId ->
                points.insert(
                    PointEvent(childId = childId, delta = -goal.cost, type = PointEventType.REDEEM, goalId = goalId, label = goal.title, timestamp = now)
                )
            }
            goals.update(goal.copy(status = GoalStatus.REDEEMED, redeemedAt = now))
            updater.refreshForChildren(members)
        }
        events.forEach(bus::emit)
    }

    /** Re-evaluates every open goal without emitting events; call once at startup so stored status matches the per-child rule. */
    suspend fun reevaluateOpenGoals() {
        db.tx { updater.refreshAllOpen() }
    }

    suspend fun restart(goalId: Long): Long {
        val goal = checkNotNull(goals.get(goalId)) { "goal $goalId not found" }
        return create(goal.title, goal.icon, goal.cost, goals.memberIds(goalId).toSet())
    }

    private fun validate(title: String, cost: Int, memberIds: Set<Long>) {
        require(title.isNotBlank()) { "title must not be blank" }
        require(cost > 0) { "cost must be > 0" }
        require(memberIds.isNotEmpty()) { "a goal needs at least one child" }
    }
}
