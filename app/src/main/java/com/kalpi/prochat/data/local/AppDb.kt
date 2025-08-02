package com.kalpi.prochat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(
    entities = [ChatMessageEntity::class],
    // Increment the version number to 3
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
}