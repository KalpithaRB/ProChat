package com.kalpi.prochat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "chat_messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val roomId: String,
    val senderId: String,
    val text: String?,
    val imageUrl: String?, // For image messages
    val fileUrl: String?, // URL for any non-image attachment
    val audioUrl: String?, // For audio messages
    val fileName: String?,
    val fileType: String?,
    val fileSize: Long?,
    val clientTimestamp: Long, // <-- ADDED THIS FIELD
    val messageType: String,
    val status: String
)