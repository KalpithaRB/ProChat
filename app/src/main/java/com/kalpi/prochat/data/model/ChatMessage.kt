package com.kalpi.prochat.data.model

import com.google.firebase.firestore.ServerTimestamp // For server-side timestamping
import java.util.Date // Keep this if you still want a client-side estimate

/**
 * Represents a single chat message within the application.
 *
 * @property id Unique identifier for the message.
 * @property senderId Identifier for the user who sent the message. Used to determine
 *                    if the message is from the current user or another user for UI purposes.
 * @property imageUrl URL of the image if this is an image message.
 * @property serverTimestamp Firebase server timestamp.
 * @property clientTimestamp Client-side timestamp.
 * @property messageType The type of message (e.g., TEXT, IMAGE, SYSTEM).
 * @property status Current status of the message (SENDING, SENT, FAILED).
 * @property roomId ID of the chatroom this message belongs to.
 */
data class ChatMessage(
    val id: String = "", // Client-generated UUID, will be used as document ID
    val senderId: String = "",
    val text: String? = null,
    val imageUrl: String? = null, //for image messages
    @ServerTimestamp val serverTimestamp: Date? = null, // Firebase server timestamp
    val clientTimestamp: Long = System.currentTimeMillis(), // Client-side estimate, for immediate display & sorting before server ack
    val messageType: MessageType = MessageType.TEXT,
    val status: MessageStatus = MessageStatus.SENDING, // Default to SENDING for new messages
    val roomId: String = "" // To know which chatroom this message belongs to
) {
    // Add a no-argument constructor for Firebase deserialization if needed,
    // though with default values for all properties, it might not be strictly necessary.
    constructor() : this(
    id = "",
    senderId = "",
    text = null, // <<< MODIFIED
    imageUrl = null, // <<< NEW
    serverTimestamp = null,
    clientTimestamp = 0L,
    messageType = MessageType.TEXT, // <<< MODIFIED
    status = MessageStatus.SENDING,
    roomId = ""

    )
}

enum class MessageStatus {
    SENDING,
    SENT,
    FAILED,
    DELIVERED,
    READ
    // SEEN // For Day 6
}

/**
 * Defines the type of a chat message.
 */
enum class MessageType {
    TEXT,  // <<< MODIFIED: Was USER, now explicitly TEXT
    IMAGE, // <<< NEW: For image messages
    /** A system-generated message (e.g., "User X has joined the chat"). */
    SYSTEM, USER
}