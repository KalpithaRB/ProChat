package com.kalpi.prochat.data.model


data class User(
    val userId: String = "",
    val name: String = "User", // Can be a default name
    val createdAt: Long = System.currentTimeMillis()
)
