package com.gekkotron.kidquest.domain

object GoalProgressCalculator {
    /** One member's progress toward the per-child [cost]; a negative balance counts as 0. */
    fun memberProgress(balance: Int, cost: Int): Float {
        require(cost > 0) { "cost must be > 0" }
        return (maxOf(0, balance).toFloat() / cost).coerceIn(0f, 1f)
    }

    /** Mean of the member progresses, for display. It is 1 only when every member has reached the cost. */
    fun progress(memberBalances: Collection<Int>, cost: Int): Float {
        require(cost > 0) { "cost must be > 0" }
        if (memberBalances.isEmpty()) return 0f
        return memberBalances.map { memberProgress(it, cost) }.average().toFloat()
    }

    /** Every member must hold at least the full [cost]. */
    fun isReached(memberBalances: Collection<Int>, cost: Int): Boolean =
        memberBalances.isNotEmpty() && memberBalances.all { it >= cost }
}
