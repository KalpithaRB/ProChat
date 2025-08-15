package com.kalpi.prochat.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId

@Keep // Recommended for use with Firebase Firestore
data class ChatRoom(
    @DocumentId val documentId: String = "",
    val roomId: String = "",
    val type: String = "dm",  // New field to distinguish between 'group' and 'dm'
    val title: String = "",   // New field, for the group's name
    val createdBy: String = "", // New field, for the user who created the group
    val createdAt: Long = 0L,   // New field, for the creation timestamp
    val lastMessage: String? = null,
    val lastTimestamp: Long = 0L,
    val lastReadTimestamp: Long = 0L,
    val unreadCount: Int = 0,
    val muted: Boolean = false,
)

// Add a new data class for a member in a group chat.
// This will be used for the 'members' subcollection.
@Keep
data class Member(
    @DocumentId
    val userId: String = "",
    val role: String = "member",  // Can be "admin" or "member"
    val joinedAt: Long = 0L,
    val isMuted: Boolean = false
)