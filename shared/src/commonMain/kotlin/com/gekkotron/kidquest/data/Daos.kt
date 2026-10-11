package com.gekkotron.kidquest.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class ChildTotals(val childId: Long, val balance: Int, val lifetime: Int)
data class ChildTimestamp(val childId: Long, val timestamp: Long)

data class GoalWithMembers(
    @Embedded val goal: Goal,
    @Relation(parentColumn = "id", entityColumn = "goalId") val members: List<GoalMember>,
)

@Dao
interface ChildDao {
    @Insert suspend fun insert(child: Child): Long
    @Update suspend fun update(child: Child)
    @Query("SELECT * FROM child WHERE id = :id") suspend fun get(id: Long): Child?
    @Query("SELECT * FROM child WHERE id IN (:ids) ORDER BY createdAt, id") suspend fun getByIds(ids: List<Long>): List<Child>
    @Query("SELECT * FROM child WHERE archived = 0 ORDER BY createdAt, id") fun observeActive(): Flow<List<Child>>
    @Query("SELECT * FROM child ORDER BY archived, createdAt, id") fun observeAll(): Flow<List<Child>>
}

@Dao
interface PointEventDao {
    @Insert suspend fun insert(event: PointEvent): Long
    @Query("SELECT * FROM point_event WHERE id = :id") suspend fun get(id: Long): PointEvent?
    @Query("UPDATE point_event SET undone = :undone WHERE id = :id") suspend fun setUndone(id: Long, undone: Boolean)
    @Query("UPDATE point_event SET undone = :undone WHERE goalId = :goalId AND type = 'REDEEM'")
    suspend fun setRedeemUndone(goalId: Long, undone: Boolean)

    @Query("SELECT COALESCE(SUM(delta), 0) FROM point_event WHERE childId = :childId AND undone = 0")
    suspend fun balance(childId: Long): Int

    @Query("SELECT COALESCE(SUM(delta), 0) FROM point_event WHERE childId = :childId AND undone = 0 AND type = 'AWARD'")
    suspend fun lifetime(childId: Long): Int

    @Query(
        """SELECT childId, COALESCE(SUM(delta), 0) AS balance,
           COALESCE(SUM(CASE WHEN type = 'AWARD' THEN delta ELSE 0 END), 0) AS lifetime
           FROM point_event WHERE undone = 0 GROUP BY childId"""
    )
    fun observeTotals(): Flow<List<ChildTotals>>

    @Query("SELECT childId, timestamp FROM point_event WHERE undone = 0 AND type = 'AWARD'")
    fun observeAwardTimestamps(): Flow<List<ChildTimestamp>>

    @Query("SELECT * FROM point_event WHERE childId = :childId ORDER BY timestamp DESC, id DESC")
    fun observeHistory(childId: Long): Flow<List<PointEvent>>
}

@Dao
interface GoalDao {
    @Insert suspend fun insert(goal: Goal): Long
    @Update suspend fun update(goal: Goal)
    @Query("DELETE FROM goal WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT * FROM goal WHERE id = :id") suspend fun get(id: Long): Goal?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertMembers(members: List<GoalMember>)
    @Query("DELETE FROM goal_member WHERE goalId = :goalId") suspend fun deleteMembers(goalId: Long)
    @Query("SELECT childId FROM goal_member WHERE goalId = :goalId") suspend fun memberIds(goalId: Long): List<Long>

    @Query(
        """SELECT * FROM goal WHERE status IN ('ACTIVE', 'ACHIEVED')
           AND id IN (SELECT goalId FROM goal_member WHERE childId IN (:childIds))"""
    )
    suspend fun openGoalsForChildren(childIds: List<Long>): List<Goal>

    @Transaction
    @Query("SELECT * FROM goal ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<GoalWithMembers>>
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM point_action ORDER BY sortOrder, id") fun observeActions(): Flow<List<PointAction>>
    @Query("SELECT * FROM point_action ORDER BY sortOrder, id") suspend fun actions(): List<PointAction>
    @Insert suspend fun insertAction(action: PointAction): Long
    @Update suspend fun updateActions(vararg actions: PointAction)
    @Delete suspend fun deleteAction(action: PointAction)

    @Query("SELECT * FROM quick_amount ORDER BY sortOrder, id") fun observeQuickAmounts(): Flow<List<QuickAmount>>
    @Query("SELECT * FROM quick_amount ORDER BY sortOrder, id") suspend fun quickAmounts(): List<QuickAmount>
    @Insert suspend fun insertQuickAmount(amount: QuickAmount): Long
    @Update suspend fun updateQuickAmounts(vararg amounts: QuickAmount)
    @Delete suspend fun deleteQuickAmount(amount: QuickAmount)

    @Query("SELECT value FROM app_setting WHERE `key` = :key") fun observeSetting(key: String): Flow<String?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSetting(setting: AppSetting)
    @Query("DELETE FROM app_setting WHERE `key` = :key") suspend fun removeSetting(key: String)
}
