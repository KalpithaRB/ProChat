package com.kalpi.prochat.ui.presentations.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalpi.prochat.data.ChatItem
import com.kalpi.prochat.data.model.ChatMessage
import com.kalpi.prochat.data.model.MessageStatus
import com.kalpi.prochat.data.model.MessageType
import com.kalpi.prochat.data.repository.ChatRepository
import com.kalpi.prochat.ui.chat.ChatUiState
import com.kalpi.prochat.utils.formatDateSeparator
import com.kalpi.prochat.utils.isSameDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for the ChatScreen.
 *
 * This ViewModel is responsible for preparing and managing the UI-related data for the ChatScreen.
 * It exposes the chat messages and UI state through [kotlinx.coroutines.flow.StateFlow].
 *
 * For Day 1, it uses a predefined list of dummy messages.
 * In later stages, it will interact with a data source (e.g., Firebase) to fetch and send messages.
 */

class ChatViewModel (
    private val chatRepository: ChatRepository, // Injected
    private val currentRoomId: String,
    val currentUserId: String

): ViewModel() {
    // A simple way to distinguish the "current user" for dummy data styling.
    // In a real app, this would come from an authentication service.
    companion object {
//        const val CURRENT_USER_ID = "currentUser"

        private const val TAG = "ChatViewModel"
        fun getOrCreateUserId(context: Context): String {
            val prefs = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
            var userId = prefs.getString("user_id", null)

            if (userId == null) {
                userId = UUID.randomUUID().toString()
                prefs.edit().putString("user_id", userId).apply()
            }
            return userId
        }
    }


    // Private mutable state flow to hold the current UI state.
    // Starts in a Loading state, then transitions to Content with dummy messages.
    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    /**
     * Publicly exposed [kotlinx.coroutines.flow.StateFlow] of the [ChatUiState].
     * The UI (ChatScreen) will observe this flow for updates.
     */
    val uiState: StateFlow<ChatUiState> = chatRepository.listenToMessages(currentRoomId)
        .map { messages ->
            processMessagesAndUpdateState(messages)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5000),
            initialValue = ChatUiState.Loading
        )

    // State to hold upload progress for messages (MessageID -> Progress Percentage)
    private val _uploadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val uploadProgress: StateFlow<Map<String, Int>> = _uploadProgress.asStateFlow()

    init {
        //i am keeping this screen empty and removed the dummy data
        // and the new msgs will be getting appended as they are sent.
        processMessagesAndUpdateState(emptyList())
    }



    // Sending a message
    // --- Message Sending Logic ---
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val newMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderId = currentUserId,
            text = text.trim(),
            clientTimestamp = System.currentTimeMillis(),
            messageType = MessageType.TEXT,
            status = MessageStatus.SENDING,
            roomId = currentRoomId
        )

        viewModelScope.launch {
            chatRepository.sendMessage(currentRoomId, newMessage)
        }
    }

    // +++++++++++++ NEW FUNCTION FOR IMAGE MESSAGES +++++++++++++
    fun prepareAndSendImageMessage(imageUri: Uri) {
        Log.d("ChatViewModel", "prepareAndSendImageMessage called with URI: $imageUri")

        // 1. Create a temporary ChatMessage object for UI display (optional, but good for UX)
        // This message will show an upload progress indicator.
        val tempImageMessage = ChatMessage(
            id = UUID.randomUUID().toString(), // Client-generated unique ID
            senderId = currentUserId,
            // text = null, // No text for a pure image message initially
            imageUrl = imageUri.toString(), // Store local URI temporarily for local display/placeholder
            clientTimestamp = System.currentTimeMillis(),
            messageType = MessageType.IMAGE,
            status = MessageStatus.SENDING, // Indicate it's in the process of being sent/uploaded
            roomId = currentRoomId
            // We might add an 'uploadProgress' field to ChatMessage later if needed,
            // or manage progress separately. For now, SENDING status can imply progress.
        )

        _uploadProgress.update { currentProgress ->
            currentProgress + (tempImageMessage.id to 0)
        }

        viewModelScope.launch {
            Log.d(TAG, "Calling repository to upload image for message ID: ${tempImageMessage.id}")

            val uploadResult = chatRepository.uploadImageToCloudinaryAndGetUrl(
                imageUri = imageUri,
                uploadPreset = "prochat_unsigned_images",
                messageIdForLog = tempImageMessage.id,
                onProgress = { progressPercentage ->
                    _uploadProgress.update { currentProgress ->
                        currentProgress + (tempImageMessage.id to progressPercentage)
                    }
                }
            )

            if (uploadResult.isSuccess) {
                val cloudinaryUrl = uploadResult.getOrNull()
                if (cloudinaryUrl != null) {
                    Log.d(TAG, "Image upload success. URL: $cloudinaryUrl")
                    val finalImageMessage = tempImageMessage.copy(
                        imageUrl = cloudinaryUrl,
                        status = MessageStatus.SENT
                    )
                    chatRepository.sendMessage(currentRoomId, finalImageMessage)
                } else {
                    Log.e(TAG, "Image upload succeeded but URL was null.")
                }
            } else {
                Log.e(TAG, "Image upload failed: ${uploadResult.exceptionOrNull()?.message}")
            }
        }
    }


    fun retrySendMessage(failedMessage: ChatMessage) {
        viewModelScope.launch {
            chatRepository.sendMessage(currentRoomId, failedMessage.copy(status = MessageStatus.SENDING))
        }
    }

    // --- Helper function to process messages and update UI state (from Day 1) ---
    private fun processMessagesAndUpdateState(messages: List<ChatMessage>): ChatUiState {
        if (messages.isEmpty()) {
            return ChatUiState.Content(emptyList())
        }
        val sortedMessages = messages.sortedBy { it.clientTimestamp }
        val chatItems = mutableListOf<ChatItem>()
        var lastDateHeaderTimestamp: Long? = null

        sortedMessages.forEach { message ->
            if (lastDateHeaderTimestamp == null || !isSameDay(
                    message.clientTimestamp,
                    lastDateHeaderTimestamp!!
                )
            ) {
                chatItems.add(ChatItem.DateSeparator(formatDateSeparator(message.clientTimestamp), message.clientTimestamp))
                lastDateHeaderTimestamp = message.clientTimestamp
            }
            chatItems.add(ChatItem.Message(message))
        }
        return ChatUiState.Content(chatItems)
    }

}