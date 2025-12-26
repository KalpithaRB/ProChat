package com.kalpi.prochat.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId

@Keep
data class ChatRoom(
    @DocumentId
    val documentId: String = "", // ✨ This will hold the Firestore document ID
    val title: String = "",
    val type: String = "",
    val createdAt: Long = 0L,
    val createdBy: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String? = null,
    val lastTimestamp: Long = 0L,
    val lastReadTimestamp: Long = 0L,
    val unreadCount: Int = 0,
    val muted: Boolean = false,
    val isDeleted: Boolean = false // <-- It's important to have this in your data model.
)
// Add a new data class for a member in a group chat.
// This will be used for the 'members' subcollection.
//    val participants: List<FullChatMember> = emptyList(),
@Keep
data class Member(
    @DocumentId
    val userId: String = "",
    val role: String = "member",  // Can be "admin" or "member"
    val joinedAt: Long = 0L,
    val isMuted: Boolean = false,
    val lastActiveAt: Long? = null
)