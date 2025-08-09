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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.coroutines.resume
import com.kalpi.prochat.data.local.ChatMessageDao
import com.kalpi.prochat.data.local.MessageEntity
import com.kalpi.prochat.data.model.MessageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatRepository(
    private val db: FirebaseFirestore,
    private val chatMessageDao: ChatMessageDao
) {

    companion object {
        const val DEFAULT_ROOM_ID = "kalpis_chat_room" // Placeholder
        private const val TAG = "ChatRepository"
        private const val CHATROOMS_COLLECTION = "chatrooms"
        private const val MESSAGES_COLLECTION = "messages"
    }

    /**function to convert our Firestore ChatMessage to a Room MessageEntity */
    private fun ChatMessage.toMessageEntity(): MessageEntity {
        return MessageEntity(
            id = this.id,
            roomId = this.roomId,
            senderId = this.senderId,
            text = this.text,
            imageUrl = this.imageUrl,
            fileUrl = this.fileUrl,
            audioUrl = this.audioUrl,
            fileName = this.fileName,
            fileType = this.fileType,
            fileSize = this.fileSize,
            clientTimestamp = this.clientTimestamp,
            messageType = this.messageType.name,
            status = this.status.name
        )
    }

    /** New function to convert a Room MessageEntity back to a ChatMessage */
    private fun MessageEntity.toChatMessage(): ChatMessage {
        return ChatMessage(
            id = this.id,
            roomId = this.roomId,
            senderId = this.senderId,
            text = this.text,
            imageUrl = this.imageUrl,
            fileUrl = this.fileUrl,
            audioUrl = this.audioUrl,
            fileName = this.fileName,
            fileType = this.fileType,
            fileSize = this.fileSize,
            clientTimestamp = this.clientTimestamp,
            messageType = MessageType.valueOf(this.messageType),
            status = MessageStatus.valueOf(this.status)
        )
    }


    suspend fun sendMessage(roomId: String, message: ChatMessage) {
        withContext(Dispatchers.IO) {
            // 1. Save message to local database with a SENDING status
            chatMessageDao.insertMessage(message.toMessageEntity())
            Log.d(TAG, "Message saved to local database with status: ${message.status.name}")

            // 2. Try to send the message to Firestore
            try {
                db.collection(CHATROOMS_COLLECTION)
                    .document(roomId)
                    .collection(MESSAGES_COLLECTION)
                    .document(message.id)
                    .set(message)
                    .await()

                // 3. Update the chatroom documents for EACH participant
                val chatRoomDocRef = db.collection(CHATROOMS_COLLECTION).document(roomId)
                val chatRoomDocument = chatRoomDocRef.get().await()
                val participants = chatRoomDocument.get("participants") as? List<String> ?: emptyList()
                val userChatroomsCollection = db.collection("user_chat_rooms")

                for (participantId in participants) {
                    val updates = mutableMapOf<String, Any>(
                        "lastMessage" to (message.text ?: ""),
                        "lastTimestamp" to message.clientTimestamp
                    )

                    if (participantId == message.senderId) {
                        updates["lastReadTimestamp"] = message.clientTimestamp
                    }

                    userChatroomsCollection.document(participantId)
                        .collection("rooms")
                        .document(roomId)
                        .update(updates)
                        .await()
                }

                // 4. If all remote writes are successful, update the status to SENT
                chatMessageDao.updateMessageStatus(message.id, MessageStatus.SENT.name)
                Log.d(TAG, "Message successfully sent to Firestore and local status updated to SENT.")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message to Firestore: ${message.id}", e)
                // If anything fails, the message remains with its current status (e.g., SENDING or FAILED)
                // so it can be retried later.
                chatMessageDao.updateMessageStatus(message.id, MessageStatus.FAILED.name)
            }
        }
    }

    suspend fun getFailedMessages(roomId: String): List<ChatMessage> {
        return withContext(Dispatchers.IO) {
            chatMessageDao.getFailedMessages(roomId).map { it.toChatMessage() }
        }
    }

    suspend fun uploadFileToCloudinaryAndGetUrl(
        fileUri: Uri,
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
            Log.d(TAG, "Starting Cloudinary upload for message: $messageIdForLog, URI: $fileUri")

            val request = MediaManager.get().upload(fileUri)
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
    fun listenToMessages(roomId: String): Flow<List<ChatMessage>> {
        // 1. Flow from the local Room database.
        val localMessagesFlow = chatMessageDao.getMessagesByRoom(roomId).map { entities ->
            entities.map { it.toChatMessage() }
        }

        // 2. Flow from Firestore using callbackFlow. This replaces .asFlow().
        val remoteFlow = callbackFlow<List<ChatMessage>> {
            val messagesCollection = db.collection(CHATROOMS_COLLECTION)
                .document(roomId)
                .collection(MESSAGES_COLLECTION)
                .orderBy("clientTimestamp", Query.Direction.ASCENDING)

            val subscription = messagesCollection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching from Firestore", error)
                    close(error) // Close the flow with an error
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val firestoreMessages = snapshot.documents.mapNotNull { document ->
                        document.toObject(ChatMessage::class.java)
                    }
                    trySend(firestoreMessages).isSuccess // Emit the messages to the flow
                }
            }
            // Suspend until the flow is closed, then remove the listener
            awaitClose { subscription.remove() }
        }.onEach { remoteMessages ->
            // When new data arrives from Firestore, write it to the local Room database.
            remoteMessages.forEach { message ->
                chatMessageDao.insertMessage(message.toMessageEntity())
            }
        }

        // 3. Merge the flows and ensure a single source of truth from Room.
        return merge(localMessagesFlow, remoteFlow).flatMapLatest {
            localMessagesFlow
        }.distinctUntilChanged()
    }

    suspend fun updateMessageStatus(roomId: String, messageId: String, newStatus: MessageStatus): Result<Unit> {
        // Update Both local and remote
        chatMessageDao.updateMessageStatus(messageId, newStatus.name)
        return try {
            db.collection(CHATROOMS_COLLECTION)
                .document(roomId)
                .collection(MESSAGES_COLLECTION)
                .document(messageId)
                .update("status", newStatus.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating message status for message $messageId", e)
            Result.failure(e)
        }
    }

    suspend fun markMessageAsDelivered(chatRoomId: String, messageId: String) {
        // Update local status first
        chatMessageDao.updateMessageStatus(messageId, MessageStatus.DELIVERED.name)
        try {
            db.collection("chat_rooms")
                .document(chatRoomId)
                .collection("messages")
                .document(messageId)
                .update("status", MessageStatus.DELIVERED)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error marking message as delivered in Firestore", e)
        }
    }

    suspend fun markMessageAsRead(chatRoomId: String, messageId: String) {
        // Update local status first
        chatMessageDao.updateMessageStatus(messageId, MessageStatus.READ.name)
        try {
            db.collection("chat_rooms")
                .document(chatRoomId)
                .collection("messages")
                .document(messageId)
                .update("status", MessageStatus.READ)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error marking message as read in Firestore", e)
        }
    }
}
