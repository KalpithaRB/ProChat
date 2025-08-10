package com.kalpi.prochat.data.local

import com.kalpi.prochat.data.model.MessageStatus
import com.kalpi.prochat.data.model.ChatMessage
import com.kalpi.prochat.data.model.MessageType
import com.kalpi.prochat.data.local.MessageEntity

/**
 * Extension function to convert a ChatMessage to a ChatMessageEntity for local storage.
 *
 * @param roomId The ID of the chat room.
 * @return The converted ChatMessageEntity.
 */
fun ChatMessage.toEntity(roomId: String):
        MessageEntity {
    return MessageEntity(
        id = id,
        text = text ?: "",
        senderId = senderId,
        imageUrl = imageUrl,
        fileUrl = fileUrl,
        audioUrl = audioUrl,
        fileName = fileName,
        fileType = fileType,
        fileSize = fileSize,
        clientTimestamp = clientTimestamp,
        status = status.name,
        roomId = roomId,
        messageType = messageType.name
    )
}

/**
 * Extension function to convert a ChatMessageEntity back to a ChatMessage.
 *
 * @return The converted ChatMessage.
 */
fun MessageEntity.toChatMessage(): ChatMessage {
    return ChatMessage(
        id = id,
        roomId = roomId,
        senderId = senderId,
        text = text,
        imageUrl = imageUrl,
        fileUrl = fileUrl, // Pass the fileUrl from MessageEntity
        audioUrl = audioUrl, // Pass the audioUrl from MessageEntity
        fileName = fileName, // Pass the fileName from MessageEntity
        fileType = fileType,
        fileSize = fileSize,
        clientTimestamp = clientTimestamp,
        status = MessageStatus.valueOf(status),
        messageType = MessageType.valueOf(messageType)
    )
}