package com.gekkotron.kidquest.data

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun databaseBuilder(): RoomDatabase.Builder<KidQuestDatabase> {
    val dir = requireNotNull(
        NSFileManager.defaultManager.URLForDirectory(NSDocumentDirectory, NSUserDomainMask, null, true, null)?.path
    )
    return Room.databaseBuilder<KidQuestDatabase>(name = "$dir/$DB_NAME")
}
