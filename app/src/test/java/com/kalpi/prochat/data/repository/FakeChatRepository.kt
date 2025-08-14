package com.kalpi.prochat.data.repository

import android.net.Uri
import android.util.Log
import com.kalpi.prochat.data.model.ChatMessage
import com.kalpi.prochat.data.model.MessageStatus
import com.kalpi.prochat.data.repository.ChatRepository // This assumes your ViewModel uses the concrete class directly
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.coroutines.resume

/**
 * A fake implementation of ChatRepository for testing purposes.
 * Stores messages in memory and simulates asynchronous operations.
 */
class FakeChatRepository : ChatRepository{

    // Use a backing property to hold the in-memory messages
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())

    // This simulates the Flow<List<ChatMessage>> returned by the real repository
    // It filters for the correct roomId (though not strictly necessary for this fake)
    override fun listenToMessages(roomId: String): Flow<List<ChatMessage>> {
        return _messages.map { allMessages ->
            allMessages.filter { it.roomId == roomId }
                .sortedBy { it.clientTimestamp }
        }
    }

//    override suspend fun sendMessage(roomId: String, message: ChatMessage): Result<Unit> {
//        // Your real repo sets the status to SENDING initially. We'll simulate that.
//        val pendingMessage = message.copy(status = MessageStatus.SENDING)
//
//        // Step 1: Add the pending message to our in-memory list
//        _messages.update { currentMessages ->
//            val existing = currentMessages.indexOfFirst { it.id == message.id }
//            if (existing != -1) {
//                currentMessages.toMutableList().apply { set(existing, pendingMessage) }
//            } else {
//                currentMessages + pendingMessage
//            }
//        }
//
//        // Simulate network delay for the Firestore write
//        delay(10)
//
//        // Step 2: Simulate a successful network update to the SENT status
//        _messages.update { currentMessages ->
//            currentMessages.map { msg ->
//                if (msg.id == message.id) {
//                    msg.copy(status = MessageStatus.SENT)
//                } else {
//                    msg
//                }
//            }
//        }
//
//        // Return a success result to match the real repository
//        return Result.success(Unit)
//    }

    override suspend fun sendMessage(roomId: String, message: ChatMessage): Result<Unit> {
        // The repository only stores successfully sent messages
        // No need to simulate SENDING → SENT transition since ViewModel handles that
        val sentMessage = message.copy(
            status = MessageStatus.SENT,
            //serverTimestamp = System.currentTimeMillis() // Add server timestamp if needed
        )

        _messages.update { currentMessages ->
            // Remove any existing message with the same ID and add the new sent message
            val filteredMessages = currentMessages.filter { it.id != message.id }
            filteredMessages + sentMessage
        }

        return Result.success(Unit)
    }

    override suspend fun getFailedMessages(roomId: String): List<ChatMessage> {
        // Return a list of messages from our in-memory store that are marked as FAILED
        return _messages.value.filter { it.roomId == roomId && it.status == MessageStatus.FAILED }
    }

    override suspend fun uploadFileToCloudinaryAndGetUrl(
        fileUri: Uri,
        uploadPreset: String,
        messageIdForLog: String,
        onProgress: (progress: Int) -> Unit
    ): Result<String> {
        // Simulate progress updates
        onProgress(0)
        delay(10)
        onProgress(50)
        delay(10)
        onProgress(100)
        delay(10)

        // Simulate a successful upload with a fake URL
        return Result.success("https://fakeurl.com/${UUID.randomUUID()}.jpg")
    }

    override suspend fun updateMessageStatus(roomId: String, messageId: String, newStatus: MessageStatus): Result<Unit> {
        _messages.update { currentMessages ->
            currentMessages.map { msg ->
                if (msg.id == messageId) {
                    msg.copy(status = newStatus)
                } else {
                    msg
                }
            }
        }
        return Result.success(Unit)
    }

    // These two methods are not used by the fake repo's internal logic but are needed
    // if the ViewModel calls them. We'll add them to avoid compile errors.
    override suspend fun markMessageAsDelivered(chatRoomId: String, messageId: String) {
        updateMessageStatus(chatRoomId, messageId, MessageStatus.DELIVERED)
    }

    override suspend fun markMessageAsRead(chatRoomId: String, messageId: String) {
        updateMessageStatus(chatRoomId, messageId, MessageStatus.READ)
    }

    // Helper functions to manage the in-memory data for tests
    fun addMessage(message: ChatMessage) {
        _messages.update { it + message }
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }
}