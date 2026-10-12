package com.gekkotron.kidquest.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val BACKUP_SCHEMA_VERSION = 1

@Serializable
data class BackupEnvelope(
    val schemaVersion: Int,
    val exportedAt: Long,
    val data: BackupData,
)

@Serializable
data class BackupData(
    val children: List<Child>,
    val goals: List<Goal>,
    val goalMembers: List<GoalMember>,
    val pointEvents: List<PointEvent>,
    val pointActions: List<PointAction>,
    val quickAmounts: List<QuickAmount>,
    val appSettings: List<AppSetting>,
)

@Dao
interface BackupDao {
    @Query("SELECT * FROM child ORDER BY createdAt, id") suspend fun allChildren(): List<Child>
    @Query("SELECT * FROM goal ORDER BY createdAt, id") suspend fun allGoals(): List<Goal>
    @Query("SELECT * FROM goal_member") suspend fun allGoalMembers(): List<GoalMember>
    @Query("SELECT * FROM point_event ORDER BY id") suspend fun allPointEvents(): List<PointEvent>
    @Query("SELECT * FROM point_action ORDER BY sortOrder, id") suspend fun allPointActions(): List<PointAction>
    @Query("SELECT * FROM quick_amount ORDER BY sortOrder, id") suspend fun allQuickAmounts(): List<QuickAmount>
    @Query("SELECT * FROM app_setting") suspend fun allSettings(): List<AppSetting>

    @Query("DELETE FROM app_setting") suspend fun clearSettings()
    @Query("DELETE FROM quick_amount") suspend fun clearQuickAmounts()
    @Query("DELETE FROM point_action") suspend fun clearPointActions()
    @Query("DELETE FROM point_event") suspend fun clearPointEvents()
    @Query("DELETE FROM goal_member") suspend fun clearGoalMembers()
    @Query("DELETE FROM goal") suspend fun clearGoals()
    @Query("DELETE FROM child") suspend fun clearChildren()

    @Insert suspend fun insertChildren(rows: List<Child>)
    @Insert suspend fun insertGoals(rows: List<Goal>)
    @Insert suspend fun insertGoalMembers(rows: List<GoalMember>)
    @Insert suspend fun insertPointEvents(rows: List<PointEvent>)
    @Insert suspend fun insertPointActions(rows: List<PointAction>)
    @Insert suspend fun insertQuickAmounts(rows: List<QuickAmount>)
    @Insert suspend fun insertSettings(rows: List<AppSetting>)
}

class BackupRepository(private val db: KidQuestDatabase) {
    private val dao = db.backupDao()
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun export(exportedAt: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()): String {
        val envelope = BackupEnvelope(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            exportedAt = exportedAt,
            data = BackupData(
                children = dao.allChildren(),
                goals = dao.allGoals(),
                goalMembers = dao.allGoalMembers(),
                pointEvents = dao.allPointEvents(),
                pointActions = dao.allPointActions(),
                quickAmounts = dao.allQuickAmounts(),
                appSettings = dao.allSettings(),
            ),
        )
        return json.encodeToString(envelope)
    }

    suspend fun import(serialized: String) {
        val envelope = json.decodeFromString<BackupEnvelope>(serialized)
        require(envelope.schemaVersion == BACKUP_SCHEMA_VERSION) {
            "unsupported backup schema version ${envelope.schemaVersion}"
        }
        db.tx {
            dao.clearSettings()
            dao.clearQuickAmounts()
            dao.clearPointActions()
            dao.clearPointEvents()
            dao.clearGoalMembers()
            dao.clearGoals()
            dao.clearChildren()
            dao.insertChildren(envelope.data.children)
            dao.insertGoals(envelope.data.goals)
            dao.insertGoalMembers(envelope.data.goalMembers)
            dao.insertPointEvents(envelope.data.pointEvents)
            dao.insertPointActions(envelope.data.pointActions)
            dao.insertQuickAmounts(envelope.data.quickAmounts)
            dao.insertSettings(envelope.data.appSettings)
        }
    }
}
