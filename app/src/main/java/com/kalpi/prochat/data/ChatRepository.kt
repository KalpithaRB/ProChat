package com.kalpi.prochat.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.ListenerRegistration // For registration
import com.google.firebase.firestore.Query //for ordering
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import android.util.Log // For logging
class ChatRepository(private val db: FirebaseFirestore) {

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
             Log.e("ChatRepository", "Error sending message", e)
            Result.failure(e)
        }
    }


    fun getChatMessages(roomId: String): Flow<List<ChatMessage>> {
        // Ensure db is your injected instance, which it will be with your class constructor
        return callbackFlow {
            val messagesCollection = db.collection("chatrooms")
                .document(roomId)
                .collection("messages")
                .orderBy("clientTimestamp", Query.Direction.ASCENDING) // Order by clientTimestamp

            val listenerRegistration = messagesCollection.addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("ChatRepository", "Listen failed for room $roomId.", error)
                    close(error) // Close the flow with an exception
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val messages = snapshots.documents.mapNotNull { document ->
                        try {
                            // Assuming ChatMessage has an id field that can be set.
                            // If your ChatMessage class doesn't have an 'id' property to be populated
                            // from document.id, you might need to adjust how you create it
                            // or ensure the 'id' within the Firestore document matches ChatMessage.id
                            document.toObject(ChatMessage::class.java)?.copy(id = document.id)
                        } catch (parseError: Exception) {
                            Log.e("ChatRepository", "Error parsing message: ${document.id} in room $roomId", parseError)
                            null
                        }
                    }
                    Log.d("ChatRepository", "Room $roomId: New messages batch received, count: ${messages.size}")
                    trySend(messages).isSuccess // Offer the new list to the Flow
                } else {
                    Log.d("ChatRepository", "Room $roomId: Received null snapshots.")
                    // trySend(emptyList()).isSuccess // Optionally send empty list if snapshots are null but no error
                }
            }
            awaitClose {
                Log.d("ChatRepository", "Closing chat messages listener for room: $roomId")
                listenerRegistration.remove()
            }
        }
    }

}