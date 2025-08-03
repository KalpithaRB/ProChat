package com.kalpi.prochat.data.repository

import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kalpi.prochat.data.model.ChatMessage
import com.kalpi.prochat.data.model.MessageStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.coroutines.resume

class ChatRepository(private val db: FirebaseFirestore) {

    companion object {
        const val DEFAULT_ROOM_ID = "kalpis_chat_room" // Placeholder
        private const val TAG = "ChatRepository"
        private const val CHATROOMS_COLLECTION = "chatrooms"
        private const val MESSAGES_COLLECTION = "messages"
    }

    suspend fun sendMessage(roomId: String, message: ChatMessage): Result<Unit> {
        return try {
            // Step 1: Save the message to the chatroom's message subcollection
            val messageDocRef = db.collection(CHATROOMS_COLLECTION)
                .document(roomId)
                .collection(MESSAGES_COLLECTION)
                .document(message.id)
            messageDocRef.set(message).await()
            messageDocRef.update("status", MessageStatus.SENT.name).await() // Update status after successful write

            // Step 2: Fetch the list of participants from the main chatroom document
            val chatRoomDocRef = db.collection(CHATROOMS_COLLECTION).document(roomId)
            val chatRoomDocument = chatRoomDocRef.get().await()

            if (!chatRoomDocument.exists()) {
                Log.e(TAG, "Chat room document does not exist for ID: $roomId")
                return Result.failure(Exception("Chat room not found"))
            }

            val participants = chatRoomDocument.get("participants") as? List<String>
                ?: emptyList()

            // Step 3: Update the chatroom document for EACH participant
            val userChatroomsCollection = db.collection("user_chat_rooms") // Use the correct collection name
            for (participantId in participants) {
                userChatroomsCollection.document(participantId)
                    .collection("rooms")
                    .document(roomId)
                    .update(
                        mapOf(
                            "lastMessage" to message.text,
                            "lastTimestamp" to message.clientTimestamp
                        )
                    )
                    .await()
            }

            Log.d(TAG, "Message sent and chatroom documents updated for all participants in room: $roomId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message or updating chatroom documents", e)
            Result.failure(e)
        }
    }


    // Placeholder for Day 3
    // fun getMessagesFlow(roomId: String): Flow<List<ChatMessage>> { ... }

    suspend fun uploadImageToCloudinaryAndGetUrl(
        imageUri: Uri,
        uploadPreset: String, // e.g., "prochat_unsigned_images"
        messageIdForLog: String, // For better logging
        onProgress: (progress: Int) -> Unit
    ): Result<String> {
        // +++ START DIAGNOSTIC LOG +++
        try {
            val currentConfig = MediaManager.get().cloudinary.config
            Log.d(TAG, "Cloudinary Runtime Config: cloud_name='${currentConfig.cloudName}', " +
                    "api_key='${currentConfig.apiKey}', api_secret_is_set='${!currentConfig.apiSecret.isNullOrBlank()}'")
            if (currentConfig.apiKey != null) {
                Log.w(TAG, "WARNING: API Key is set in runtime config despite intending unsigned upload. This might be the issue.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Cloudinary runtime config", e)
        }
        // +++ END DIAGNOSTIC LOG +++

        return suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "Starting Cloudinary upload for message: $messageIdForLog, URI: $imageUri")

            val request = MediaManager.get().upload(imageUri)
                .unsigned(uploadPreset) // Use your unsigned upload preset
                .option(
                    "public_id",
                    "chatrooms/user_uploads/${UUID.randomUUID()}"
                ) // Optional: Set a public_id for better organization
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) {
                        Log.d(
                            TAG,
                            "Cloudinary upload started. Request ID: $requestId, Message: $messageIdForLog"
                        )
                        // You could emit a progress state here if implementing progress updates
                    }

                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                        val progress = ((bytes.toDouble() / totalBytes.toDouble()) * 100).toInt()
                        Log.d(
                            TAG,
                            "Cloudinary upload progress: $progress%. Request ID: $requestId, Message: $messageIdForLog"
                        )
                        onProgress(progress) // Call the passed-in lambda
                    }

                    override fun onSuccess(
                        requestId: String?,
                        resultData: MutableMap<Any?, Any?>?
                    ) {
                        val url = resultData?.get("secure_url") as? String
                        if (url != null) {
                            Log.d(
                                TAG,
                                "Cloudinary upload success. URL: $url, Request ID: $requestId, Message: $messageIdForLog"
                            )
                            if (continuation.isActive) {
                                continuation.resume(Result.success(url))
                            }
                        } else {
                            Log.e(
                                TAG,
                                "Cloudinary upload success but URL is null. ResultData: $resultData, Request ID: $requestId, Message: $messageIdForLog"
                            )
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(Exception("Cloudinary upload succeeded but URL was null.")))
                            }
                        }
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        Log.e(
                            TAG,
                            "Cloudinary upload error. Code: ${error?.code}, Desc: ${error?.description}, Request ID: $requestId, Message: $messageIdForLog"
                        )
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(Exception("Cloudinary upload failed: ${error?.description} (Code: ${error?.code})")))
                        }
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                        Log.w(
                            TAG,
                            "Cloudinary upload rescheduled. Code: ${error?.code}, Desc: ${error?.description}, Request ID: $requestId, Message: $messageIdForLog"
                        )
                        // This typically means a network issue and it will retry.
                        // For a simple implementation, we might treat this as still pending or eventually an error if it keeps happening.
                        // You could choose to not resume the coroutine here and let it retry,
                        // or resume with failure if you don't want to wait for reschedule.
                        // For now, let's treat it as an error to simplify.
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(Exception("Cloudinary upload rescheduled, considered error for now: ${error?.description}")))
                        }
                    }
                })

            // Ensure the coroutine is cancelled if the upload request is cancelled
            continuation.invokeOnCancellation {
                // Cloudinary's Android SDK might not have a direct way to cancel
                // an inflight request via the requestId from this scope easily.
                // The MediaManager.cancelRequest(requestId) exists but might be tricky to call here.
                // For now, logging cancellation. The callbacks should handle isActive checks.
                Log.d(TAG, "Cloudinary upload coroutine cancelled for message $messageIdForLog.")
            }

            request.dispatch() // This starts the upload
            Log.d(TAG, "Cloudinary upload request dispatched for message: $messageIdForLog")
        }
    }

    /**
     * Listens for real-time messages from Firestore and emits them as a Flow.
     * This uses the existing ChatMessage data model without modification.
     */
    fun listenToMessages(roomId: String): Flow<List<ChatMessage>> = callbackFlow {
        val messagesCollection = db.collection(CHATROOMS_COLLECTION)
            .document(roomId)
            .collection(MESSAGES_COLLECTION)
            .orderBy(
                "clientTimestamp",
                Query.Direction.ASCENDING
            ) // Order by client-side timestamp for consistency

        val subscription = messagesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error) // Close the flow with the error
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val messages = snapshot.documents.mapNotNull { document ->
                    // Your ChatMessage class is directly used for deserialization
                    document.toObject(ChatMessage::class.java)
                }
                trySend(messages).isSuccess // Emit the new list of messages
            }
        }

        // The awaitClose block is important to cancel the listener when the Flow is no longer needed
        awaitClose {
            subscription.remove()
        }
    }
}