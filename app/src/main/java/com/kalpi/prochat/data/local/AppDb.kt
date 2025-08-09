package com.kalpi.prochat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [MessageEntity::class], // Corrected entity reference
    version = 4, // Incrementing the version to handle the new fields
    exportSchema = false
)
// If you had a Date converter for serverTimestamp, you'd add it here
// @TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
}