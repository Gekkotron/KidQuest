package com.gekkotron.kidquest.data

import com.gekkotron.kidquest.domain.GoalProgressCalculator
import kotlin.time.Clock

/** Keeps goal status in sync with balances. Call inside a transaction; emit the returned events after commit. */
class GoalStatusUpdater(
    private val goalDao: GoalDao,
    private val pointDao: PointEventDao,
    private val clock: Clock,
) {
    suspend fun refreshForChildren(childIds: Collection<Long>): List<AppEvent> {
        if (childIds.isEmpty()) return emptyList()
        return goalDao.openGoalsForChildren(childIds.distinct()).mapNotNull { refresh(it) }
    }

    suspend fun refreshAllOpen(): List<AppEvent> = goalDao.openGoals().mapNotNull { refresh(it) }

    suspend fun refreshGoal(goalId: Long): AppEvent? = goalDao.get(goalId)?.let { refresh(it) }

    private suspend fun refresh(goal: Goal): AppEvent? {
        if (goal.status == GoalStatus.REDEEMED) return null
        val balances = goalDao.memberIds(goal.id).map { pointDao.balance(it) }
        val reached = balances.isNotEmpty() && GoalProgressCalculator.isReached(balances, goal.cost)
        return when {
            goal.status == GoalStatus.ACTIVE && reached -> {
                goalDao.update(goal.copy(status = GoalStatus.ACHIEVED, achievedAt = clock.now().toEpochMilliseconds()))
                AppEvent.GoalAchieved(goal.id, goal.title, goal.icon)
            }
            goal.status == GoalStatus.ACHIEVED && !reached -> {
                goalDao.update(goal.copy(status = GoalStatus.ACTIVE, achievedAt = null))
                null
            }
            else -> null
        }
    }
}
