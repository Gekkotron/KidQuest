package com.gekkotron.kidquest.domain

object GoalProgressCalculator {
    /** Sum of member balances, each clamped at 0 so a negative balance never drags a shared goal down. */
    fun contributed(memberBalances: Collection<Int>): Int = memberBalances.sumOf { maxOf(0, it) }

    fun progress(memberBalances: Collection<Int>, cost: Int): Float {
        require(cost > 0) { "cost must be > 0" }
        return (contributed(memberBalances).toFloat() / cost).coerceIn(0f, 1f)
    }

    fun isReached(memberBalances: Collection<Int>, cost: Int): Boolean =
        contributed(memberBalances) >= cost
}
