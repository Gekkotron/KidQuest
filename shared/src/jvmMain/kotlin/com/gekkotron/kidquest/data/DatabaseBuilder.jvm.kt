package com.gekkotron.kidquest.data

import androidx.room.Room
import androidx.room.RoomDatabase

fun inMemoryDatabaseBuilder(): RoomDatabase.Builder<KidQuestDatabase> = Room.inMemoryDatabaseBuilder<KidQuestDatabase>()

fun fileDatabaseBuilder(path: String): RoomDatabase.Builder<KidQuestDatabase> = Room.databaseBuilder<KidQuestDatabase>(name = path)
