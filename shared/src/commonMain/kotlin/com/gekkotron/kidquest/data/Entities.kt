package com.gekkotron.kidquest.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PointEventType { AWARD, PENALTY, REDEEM }
enum class GoalStatus { ACTIVE, ACHIEVED, REDEEMED }

@Entity(tableName = "child")
data class Child(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatar: String,
    val color: Long,
    val createdAt: Long,
    val archived: Boolean = false,
)

@Entity(tableName = "point_action")
data class PointAction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val points: Int,
    val icon: String,
    val sortOrder: Int,
)

@Entity(tableName = "quick_amount")
data class QuickAmount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val points: Int,
    val sortOrder: Int,
)

/** Source of truth for every point calculation. Never deleted; undo flips [undone]. */
@Entity(tableName = "point_event", indices = [Index("childId"), Index("goalId")])
data class PointEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val delta: Int,
    val type: PointEventType,
    val actionId: Long? = null,
    val goalId: Long? = null,
    val label: String,
    val timestamp: Long,
    val undone: Boolean = false,
)

@Entity(tableName = "goal")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val icon: String,
    val cost: Int,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val createdAt: Long,
    val achievedAt: Long? = null,
    val redeemedAt: Long? = null,
)

@Entity(
    tableName = "goal_member",
    primaryKeys = ["goalId", "childId"],
    foreignKeys = [ForeignKey(Goal::class, ["id"], ["goalId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("childId")],
)
data class GoalMember(val goalId: Long, val childId: Long)

@Entity(tableName = "app_setting")
data class AppSetting(@PrimaryKey val key: String, val value: String)
