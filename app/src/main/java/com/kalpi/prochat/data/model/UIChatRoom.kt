package com.kalpi.prochat.data.model

import androidx.annotation.Keep

// A new data class specifically for the UI layer
@Keep
data class UiChatRoom(
    val documentId: String = "",
    val title: String = "",
    val participants: List<FullChatMember> = emptyList(), // This will hold the enriched data
    val lastMessage: String? = null,
    val lastTimestamp: Long = 0L,
    val unreadCount: Int = 0,
    val muted: Boolean = false
    // You can add more fields as needed
)

// The FullChatMember model you already have
@Keep
data class FullChatMember(
    val userId: String,
    val name: String,
    val lastActiveAt: Long? = null
)
