package com.kalpi.prochat


import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// Data class to represent the request body for saving an FCM token
data class FcmTokenRequest(
    val userId: String,
    val fcmToken: String
)

// Data class to represent the request body for sending a chat message
data class SendMessageRequest(
    val senderId: String,
    val roomId: String,
    val messageText: String
)

// Data class to represent the server's response
data class ServerResponse(
    val success: Boolean,
    val message: String
)

interface ApiService {
    @POST("/api/save-fcm-token")
    suspend fun saveFcmToken(@Body request: FcmTokenRequest): Response<ServerResponse>

    @POST("/api/send-chat-message")
    suspend fun sendChatMessage(@Body request: SendMessageRequest): Response<ServerResponse>
}