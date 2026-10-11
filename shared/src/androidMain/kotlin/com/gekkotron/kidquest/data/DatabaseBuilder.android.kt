package com.gekkotron.kidquest.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun databaseBuilder(context: Context): RoomDatabase.Builder<KidQuestDatabase> {
    val app = context.applicationContext
    return Room.databaseBuilder<KidQuestDatabase>(app, app.getDatabasePath(DB_NAME).absolutePath)
}
