package com.gekkotron.kidquest.data

data class ChildSummary(
    val child: Child,
    val balance: Int,
    val lifetime: Int,
    val level: Int,
    val levelProgress: Float,
    val streak: Int,
)

data class GoalSummary(
    val goal: Goal,
    /** Ordered by child creation (oldest first). */
    val members: List<Child>,
    val contributed: Int,
    val progress: Float,
)
