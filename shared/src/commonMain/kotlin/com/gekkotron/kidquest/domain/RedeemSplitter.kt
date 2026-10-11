package com.gekkotron.kidquest.domain

object RedeemSplitter {
    /** Equal shares; the remainder goes one point at a time to members in the given order (oldest child first). */
    fun equal(cost: Int, orderedMemberIds: List<Long>): Map<Long, Int> {
        require(orderedMemberIds.isNotEmpty()) { "goal has no members" }
        val base = cost / orderedMemberIds.size
        val remainder = cost % orderedMemberIds.size
        return orderedMemberIds.mapIndexed { i, id -> id to base + if (i < remainder) 1 else 0 }.toMap()
    }

    fun isValid(cost: Int, memberIds: Set<Long>, split: Map<Long, Int>): Boolean =
        split.keys == memberIds && split.values.all { it >= 0 } && split.values.sum() == cost
}
