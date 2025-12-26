package com.kalpi.prochat.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class TypingStatus(
    val userId: String? = null,
    val isTyping: Boolean = false,
    @ServerTimestamp
    val updatedAt: Date? = null
)