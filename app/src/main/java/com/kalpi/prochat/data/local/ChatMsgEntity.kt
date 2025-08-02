package com.kalpi.prochat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val senderId: String,
    val clientTimestamp: Long,
    val status: String,
    val imageUrl: String?,
    val roomId: String,
    // Add the messageType field to the local entity
    val messageType: String
)