package com.kalpi.prochat.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId

@Keep // Recommended for use with Firebase Firestore
data class ChatRoom(
    @DocumentId
    val roomId: String = "",
    val name: String = "",
    val lastMessage: String? = null,
    val lastTimestamp: Long = 0L,
    val lastReadTimestamp: Long = 0L, // New field to track what the user has seen
    val unreadCount: Int = 0 // This will be calculated, not stored directly
)