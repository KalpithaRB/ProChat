package com.kalpi.prochat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    /**
     * Retrieves all messages for a specific chat room as a Flow.
     * Orders messages by `clientTimestamp` and then by `id` for stable sorting.
     */
    @Query("SELECT * FROM chat_messages WHERE roomId = :roomId ORDER BY clientTimestamp ASC, id ASC")
    fun getMessagesByRoom(roomId: String): Flow<List<MessageEntity>>

    /**
     * Inserts a message into the database. If a message with the same primary key already exists,
     * it will be replaced. This is essential for syncing data from the network.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    /**
     * Updates an existing message in the database.
     */
    @Update
    suspend fun updateMessage(message: MessageEntity)

    /**
     * Updates the status of a message using its ID. This is more efficient than updating the entire object.
     */
    @Query("UPDATE chat_messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    /**
     * Fetches a list of messages that failed to send.
     */
    @Query("SELECT * FROM chat_messages WHERE status = 'FAILED' AND roomId = :roomId")
    suspend fun getFailedMessages(roomId: String): List<MessageEntity>

    /**
     * Deletes all messages associated with a specific chat room.
     */
    @Query("DELETE FROM chat_messages WHERE roomId = :roomId")
    suspend fun clearRoomMessages(roomId: String)

    /**
     * Gets the timestamp of the oldest message in a given chat room.
     */
    @Query("SELECT MIN(clientTimestamp) FROM chat_messages WHERE roomId = :roomId")
    suspend fun getOldestTimestamp(roomId: String): Long?

    /**
     * Gets the timestamp of the newest message in a given chat room.
     * This is crucial for fetching new messages from Firebase.
     */
    @Query("SELECT MAX(clientTimestamp) FROM chat_messages WHERE roomId = :roomId")
    suspend fun getNewestTimestamp(roomId: String): Long?
}