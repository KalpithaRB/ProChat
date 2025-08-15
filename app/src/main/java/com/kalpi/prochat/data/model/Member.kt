package com.kalpi.prochat.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Member(
    val userId: String = "",
    val role: String = "member", // "member" or "admin"
    @ServerTimestamp
    val joinedAt: Date? = null
)
