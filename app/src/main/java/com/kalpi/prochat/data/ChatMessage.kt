package com.kalpi.prochat.data

import com.google.firebase.firestore.ServerTimestamp // For server-side timestamping
import java.util.Date // Keep this if you still want a client-side estimate

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
    val id: String = "", // Client-generated UUID, will be used as document ID
    val senderId: String = "",
    val text: String = "",
    @ServerTimestamp val serverTimestamp: Date? = null, // Firebase server timestamp
    val clientTimestamp: Long = System.currentTimeMillis(), // Client-side estimate, for immediate display & sorting before server ack
    val messageType: MessageType = MessageType.USER,
    val status: MessageStatus = MessageStatus.SENDING, // Default to SENDING for new messages
    val roomId: String = "" // To know which chatroom this message belongs to
) {
    // Add a no-argument constructor for Firebase deserialization if needed,
    // though with default values for all properties, it might not be strictly necessary.
    constructor() : this("", "", "", null, 0L, MessageType.USER, MessageStatus.SENDING, "")
}

enum class MessageStatus {
    SENDING,
    SENT,
    FAILED
    // SEEN // For Day 6
}

/**
 * Defines the type of a chat message.
 */
enum class MessageType {
    /** A standard message sent by a user. */
    USER,
    /** A system-generated message (e.g., "User X has joined the chat"). */
    SYSTEM
}