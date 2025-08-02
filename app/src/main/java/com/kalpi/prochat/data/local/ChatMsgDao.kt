package com.kalpi.prochat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow


@Dao
interface ChatMessageDao {
    // Updated query to use `clientTimestamp`
    @Query("SELECT * FROM chat_messages WHERE roomId = :roomId ORDER BY clientTimestamp ASC")
    fun getMessagesByRoom(roomId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    @Query("SELECT * FROM chat_messages WHERE status = 'FAILED' AND roomId = :roomId")
    suspend fun getFailedMessages(roomId: String): List<ChatMessageEntity>

    @Query("DELETE FROM chat_messages WHERE roomId = :roomId")
    suspend fun clearRoomMessages(roomId: String)

    // Updated query to use `clientTimestamp`
    @Query("SELECT MIN(clientTimestamp) FROM chat_messages WHERE roomId = :roomId")
    suspend fun getOldestTimestamp(roomId: String): Long?

    // Updated query to use `clientTimestamp`
    @Query("SELECT MAX(clientTimestamp) FROM chat_messages WHERE roomId = :roomId")
    suspend fun getNewestTimestamp(roomId: String): Long?
}