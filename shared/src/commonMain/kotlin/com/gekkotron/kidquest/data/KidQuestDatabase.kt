package com.gekkotron.kidquest.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

const val DB_NAME = "kidquest.db"

@Database(
    entities = [Child::class, PointAction::class, QuickAmount::class, PointEvent::class, Goal::class, GoalMember::class, AppSetting::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(KidQuestDatabaseConstructor::class)
abstract class KidQuestDatabase : RoomDatabase() {
    abstract fun childDao(): ChildDao
    abstract fun pointEventDao(): PointEventDao
    abstract fun goalDao(): GoalDao
    abstract fun settingsDao(): SettingsDao
}

@Suppress("KotlinNoActualForExpect")
expect object KidQuestDatabaseConstructor : RoomDatabaseConstructor<KidQuestDatabase> {
    override fun initialize(): KidQuestDatabase
}

fun buildDatabase(builder: RoomDatabase.Builder<KidQuestDatabase>, seedLanguage: String): KidQuestDatabase =
    builder
        .addCallback(SeedCallback(seedLanguage))
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
