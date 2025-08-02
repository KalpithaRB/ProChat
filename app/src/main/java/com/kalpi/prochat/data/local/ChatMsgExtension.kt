package com.kalpi.prochat.data.local

import com.kalpi.prochat.data.model.MessageStatus
import com.kalpi.prochat.data.model.ChatMessage
import com.kalpi.prochat.data.model.MessageType

/**
 * Extension function to convert a ChatMessage to a ChatMessageEntity for local storage.
 *
 * @param roomId The ID of the chat room.
 * @return The converted ChatMessageEntity.
 */
fun ChatMessage.toEntity(roomId: String): ChatMessageEntity {
    return ChatMessageEntity(
        id = id,
        text = text ?: "",
        senderId = senderId,
        clientTimestamp = clientTimestamp,
        status = status.name,
        imageUrl = imageUrl,
        roomId = roomId,
        messageType = messageType.name
    )
}

/**
 * Extension function to convert a ChatMessageEntity back to a ChatMessage.
 *
 * @return The converted ChatMessage.
 */
fun ChatMessageEntity.toChatMessage(): ChatMessage {
    return ChatMessage(
        id = id,
        text = text,
        senderId = senderId,
        clientTimestamp = clientTimestamp,
        status = MessageStatus.valueOf(status),
        imageUrl = imageUrl,
        messageType = MessageType.valueOf(messageType)
    )
}