package com.kalpi.prochat.data

/**
 * Represents a single chat message within the application.
 *
 * @property id Unique identifier for the message.
 * @property senderId Identifier for the user who sent the message. Used to determine
 *                    if the message is from the current user or another user for UI purposes.
 * @property text The actual content of the message.
 * @property timestamp The time the message was sent, represented as milliseconds since epoch.
 * @property messageType The type of message (e.g., user message, system notification).
 */
data class ChatMessage(
    val id: String,
    val senderId: String,
    val text: String,
    val timestamp: Long,
    val messageType: MessageType = MessageType.USER
)

/**
 * Defines the type of a chat message.
 */
enum class MessageType {
    /** A standard message sent by a user. */
    USER,
    /** A system-generated message (e.g., "User X has joined the chat"). */
    SYSTEM
}