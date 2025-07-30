package com.kalpi.prochat.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ChatRepository(private val db: FirebaseFirestore) {

    // For Day 2: We'll assume a fixed roomId for now for simplicity in ViewModel
    // In a real app, roomId would come from navigation or chatroom selection
    companion object {
        const val DEFAULT_ROOM_ID = "default_chat_room" // Placeholder
    }

    suspend fun sendMessage(roomId: String, message: ChatMessage): Result<Unit> {
        return try {
            db.collection("chatrooms")
                .document(roomId)
                .collection("messages")
                .document(message.id) // Use the client-generated message.id as the document ID
                .set(message)
                .await() // Make it a suspend function call
            Result.success(Unit)
        } catch (e: Exception) {
            // Log.e("ChatRepository", "Error sending message", e) // Good practice to log
            Result.failure(e)
        }
    }

    // Placeholder for Day 3
    // fun getMessagesFlow(roomId: String): Flow<List<ChatMessage>> { ... }
}