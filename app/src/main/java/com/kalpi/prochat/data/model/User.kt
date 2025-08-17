package com.kalpi.prochat.data.model

import androidx.annotation.Keep


data class User(
    val userId: String = "",
    val name: String = "User", // Can be a default name
    val createdAt: Long = System.currentTimeMillis()
)

@Keep
data class UiMember(
    val userId: String,
    val name: String,
    val role: String, // from the Member data class
    val isCurrentUser: Boolean,
    val isAdmin: Boolean,
    val isMuted: Boolean,
    val joinedAt: Long,
    val isOnline: Boolean = false
)