package com.kalpi.prochat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalpi.prochat.data.ChatRepository
import com.kalpi.prochat.data.ChatItem
import com.kalpi.prochat.data.ChatMessage
import com.kalpi.prochat.data.MessageStatus
import com.kalpi.prochat.data.MessageType
import com.kalpi.prochat.ui.chat.ChatUiState
import com.kalpi.prochat.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import java.util.UUID // For generating unique IDs for dummy messages
import java.text.SimpleDateFormat // Keep for MessageBubble timestamp
import java.util.Calendar
import android.net.Uri // <<< ADD THIS IMPORT if not already there
import android.util.Log // <<< ADD THIS IMPORT
import java.util.Locale         // Keep for MessageBubble timestamp
import java.util.Date


/**
 * ViewModel for the ChatScreen.
 *
 * This ViewModel is responsible for preparing and managing the UI-related data for the ChatScreen.
 * It exposes the chat messages and UI state through [StateFlow].
 *
 * For Day 1, it uses a predefined list of dummy messages.
 * In later stages, it will interact with a data source (e.g., Firebase) to fetch and send messages.
 */

class ChatViewModel (
    private val chatRepository: ChatRepository, // Injected
    private val currentRoomId: String

): ViewModel() {
    // A simple way to distinguish the "current user" for dummy data styling.
    // In a real app, this would come from an authentication service.
    companion object {
        const val CURRENT_USER_ID = "currentUser"
        private const val TAG = "ChatViewModel"
        const val OTHER_USER_ID = "otherUser"
    }

    // Private mutable state flow to hold the current UI state.
    // Starts in a Loading state, then transitions to Content with dummy messages.
    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    /**
     * Publicly exposed [StateFlow] of the [ChatUiState].
     * The UI (ChatScreen) will observe this flow for updates.
     */
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private val _tempMessageStore = MutableStateFlow<List<ChatMessage>>(emptyList())

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
        if (text.isBlank()) return // Don't send empty messages

        val newMessage = ChatMessage(
            id = UUID.randomUUID().toString(), // Client-generated unique ID
            senderId = CURRENT_USER_ID,
            text = text.trim(),
            clientTimestamp = System.currentTimeMillis(),
            messageType = MessageType.TEXT,
            status = MessageStatus.SENDING, // Initial status
            roomId = currentRoomId
        )

        // Add to local list immediately for UI update
        val updatedMessages = _tempMessageStore.value + newMessage
        _tempMessageStore.value = updatedMessages
        processMessagesAndUpdateState(updatedMessages)


        viewModelScope.launch {
            val result = chatRepository.sendMessage(currentRoomId, newMessage)
            val finalStatus = if (result.isSuccess) MessageStatus.SENT else MessageStatus.FAILED

            // Update the status of the message in our local store
            val messagesAfterSendAttempt = _tempMessageStore.value.map {
                if (it.id == newMessage.id) {
                    it.copy(status = finalStatus) // Assuming ChatMessage.status is var or copy creates new
                } else {
                    it
                }
            }
            _tempMessageStore.value = messagesAfterSendAttempt
            processMessagesAndUpdateState(messagesAfterSendAttempt)
        }
    }

    // +++++++++++++ NEW FUNCTION FOR IMAGE MESSAGES +++++++++++++
    fun prepareAndSendImageMessage(imageUri: Uri) {
        Log.d("ChatViewModel", "prepareAndSendImageMessage called with URI: $imageUri")

        // 1. Create a temporary ChatMessage object for UI display (optional, but good for UX)
        // This message will show an upload progress indicator.
        val tempImageMessage = ChatMessage(
            id = UUID.randomUUID().toString(), // Client-generated unique ID
            senderId = CURRENT_USER_ID,
            // text = null, // No text for a pure image message initially
            imageUrl = imageUri.toString(), // Store local URI temporarily for local display/placeholder
            clientTimestamp = System.currentTimeMillis(),
            messageType = MessageType.IMAGE,
            status = MessageStatus.SENDING, // Indicate it's in the process of being sent/uploaded
            roomId = currentRoomId
            // We might add an 'uploadProgress' field to ChatMessage later if needed,
            // or manage progress separately. For now, SENDING status can imply progress.
        )

        // Add to local list immediately for UI update
        val updatedMessages = _tempMessageStore.value + tempImageMessage
        _tempMessageStore.value = updatedMessages
        processMessagesAndUpdateState(updatedMessages)

        // Initialize progress for this messageId
        _uploadProgress.update { currentProgress ->
            currentProgress + (tempImageMessage.id to 0) // Start at 0%
        }

        viewModelScope.launch {

            // For now, let's log that we would start the upload
            Log.d("ChatViewModel", "Calling repository to upload image for message ID: ${tempImageMessage.id}")

            // 1. Upload image to Cloudinary
            val uploadResult = chatRepository.uploadImageToCloudinaryAndGetUrl(
                imageUri = imageUri,
                uploadPreset = "prochat_unsigned_images", // <<< YOUR UPLOAD PRESET NAME HERE
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
                    Log.d("ChatViewModel", "Image upload success for message ID: ${tempImageMessage.id}. URL: $cloudinaryUrl")

                    // 2. Create the final message object with the Cloudinary URL
                    val finalImageMessage = tempImageMessage.copy(
                        imageUrl = cloudinaryUrl, // Now has the remote URL
                        // Status is still SENDING because we haven't sent to Firestore yet,
                        // or you can consider it SENT if upload is the main part.
                        // Let's keep it SENDING until Firestore confirms.
                        // Alternatively, you could introduce an UPLOADING status.
                        // For simplicity, SENDING covers both upload and Firestore write.
                    )

                    // 3. Send the message metadata (with Cloudinary URL) to Firestore
                    Log.d("ChatViewModel", "Sending image message to Firestore. Message ID: ${finalImageMessage.id}")
                    val sendToFirestoreResult = chatRepository.sendMessage(currentRoomId, finalImageMessage)

                    if (sendToFirestoreResult.isSuccess) {
                        Log.d("ChatViewModel", "Image message successfully sent to Firestore. Message ID: ${finalImageMessage.id}")
                        updateMessageStatusInStore(tempImageMessage.id, MessageStatus.SENT, cloudinaryUrl)
                    } else {
                        Log.e("ChatViewModel", "Failed to send image message to Firestore. Message ID: ${finalImageMessage.id}", sendToFirestoreResult.exceptionOrNull())
                        updateMessageStatusInStore(tempImageMessage.id, MessageStatus.FAILED, cloudinaryUrl) // Keep uploaded URL, but mark as failed to send
                    }
                } else {
                    // This case should ideally not happen if uploadResult.isSuccess is true
                    // and getOrNull returns a non-null string, but good to handle.
                    Log.e("ChatViewModel", "Image upload succeeded but Cloudinary URL was null. Message ID: ${tempImageMessage.id}")
                    updateMessageStatusInStore(tempImageMessage.id, MessageStatus.FAILED, imageUri.toString()) // Revert to local URI on failure
                }
            }else { // Upload to Cloudinary failed
                Log.e("ChatViewModel", "Image upload failed for message ID: ${tempImageMessage.id}", uploadResult.exceptionOrNull())
                updateMessageStatusInStore(tempImageMessage.id, MessageStatus.FAILED, imageUri.toString()) // Revert or keep local URI
            }
        }
    }

    fun retrySendMessage(failedMessage: ChatMessage) {
        if (failedMessage.status != MessageStatus.FAILED) return // Only retry failed messages

        updateMessageStatusInStore(failedMessage.id, MessageStatus.SENDING, failedMessage.imageUrl) // Keep existing imageUrl during retry attempt


        viewModelScope.launch {
            if (failedMessage.messageType == MessageType.TEXT) {
                // ... (your existing text retry logic) ...
                val result = chatRepository.sendMessage(failedMessage.roomId, failedMessage.copy(status = MessageStatus.SENDING))
                val finalStatus = if (result.isSuccess) MessageStatus.SENT else MessageStatus.FAILED
                updateMessageStatusInStore(failedMessage.id, finalStatus)

            } else if (failedMessage.messageType == MessageType.IMAGE) {
                // For image retry, we need to check if it failed during upload or during Firestore send.
                // If failedMessage.imageUrl is a local content:// URI, it means upload failed.
                // If it's an http:// or https:// URL, it means upload succeeded but Firestore send failed.

                if (failedMessage.imageUrl != null && (failedMessage.imageUrl.startsWith("http://") || failedMessage.imageUrl.startsWith("https://"))) {
                    // Upload was successful, retry sending to Firestore
                    Log.d("ChatViewModel", "Retrying send to Firestore for image message: ${failedMessage.id}")
                    val finalImageMessage = failedMessage.copy(status = MessageStatus.SENDING)
                    val sendToFirestoreResult = chatRepository.sendMessage(currentRoomId, finalImageMessage)
                    if (sendToFirestoreResult.isSuccess) {
                        Log.d("ChatViewModel", "Image message retry to Firestore successful. ID: ${finalImageMessage.id}")
                        updateMessageStatusInStore(failedMessage.id, MessageStatus.SENT, finalImageMessage.imageUrl)
                    } else {
                        Log.e("ChatViewModel", "Image message retry to Firestore failed. ID: ${finalImageMessage.id}", sendToFirestoreResult.exceptionOrNull())
                        updateMessageStatusInStore(failedMessage.id, MessageStatus.FAILED, finalImageMessage.imageUrl)
                    }
                } else if (failedMessage.imageUrl != null) { // Assumed to be a local URI, so upload failed
                    Log.d("ChatViewModel", "Retrying upload for image message: ${failedMessage.id}")
                    val imageUriToRetry = try { Uri.parse(failedMessage.imageUrl) } catch (e: Exception) { null }

                    if (imageUriToRetry != null) {
                        // Re-call the entire upload and send sequence for this image URI
                        // Effectively, it's like calling prepareAndSendImageMessage again,
                        // but we need to ensure we are updating the *existing* message ID.

                        // For simplicity in retry, we'll directly call the upload again.
                        // A more robust solution might involve a separate "retryUploadAndSend" in repository.
                        val uploadResult = chatRepository.uploadImageToCloudinaryAndGetUrl(
                            imageUri = imageUriToRetry,
                            uploadPreset = "prochat_unsigned_images", // YOUR PRESET
                            messageIdForLog = failedMessage.id,
                            onProgress = { progressPercentage ->
                                _uploadProgress.update { currentProgress ->
                                    currentProgress + (failedMessage.id to progressPercentage)
                                }
                            }
                        )

                        if (uploadResult.isSuccess) {
                            val cloudinaryUrl = uploadResult.getOrNull()
                            if (cloudinaryUrl != null) {
                                val finalImageMessage = failedMessage.copy(imageUrl = cloudinaryUrl, status = MessageStatus.SENDING)
                                val sendToFirestoreResult = chatRepository.sendMessage(currentRoomId, finalImageMessage)
                                if (sendToFirestoreResult.isSuccess) {
                                    updateMessageStatusInStore(failedMessage.id, MessageStatus.SENT, cloudinaryUrl)
                                } else {
                                    updateMessageStatusInStore(failedMessage.id, MessageStatus.FAILED, cloudinaryUrl)
                                }
                            } else {
                                updateMessageStatusInStore(failedMessage.id, MessageStatus.FAILED, imageUriToRetry.toString())
                            }
                        } else {
                            updateMessageStatusInStore(failedMessage.id, MessageStatus.FAILED, imageUriToRetry.toString())
                        }
                    } else {
                        Log.e("ChatViewModel", "Could not parse image URI for retry: ${failedMessage.imageUrl}")
                        updateMessageStatusInStore(failedMessage.id, MessageStatus.FAILED, failedMessage.imageUrl) // Mark as failed
                    }
                } else {
                    Log.e("ChatViewModel", "Cannot retry image message, imageUrl is null. ID: ${failedMessage.id}")
                    updateMessageStatusInStore(failedMessage.id, MessageStatus.FAILED, failedMessage.imageUrl)
                }
            }
        }
    }

    // Helper to update message status and optionally URL in the local store
    private fun updateMessageStatusInStore(messageId: String, newStatus: MessageStatus, newImageUrl: String? = null) {
        val messagesAfterUpdate = _tempMessageStore.value.map {
            if (it.id == messageId) {
                it.copy(
                    status = newStatus,
                    imageUrl = newImageUrl ?: it.imageUrl // Update image URL if provided, else keep original
                )
            } else {
                it
            }
        }
        _tempMessageStore.value = messagesAfterUpdate
        processMessagesAndUpdateState(messagesAfterUpdate)
    }

    // --- Helper function to process messages and update UI state (from Day 1) ---
    private fun processMessagesAndUpdateState(messages: List<ChatMessage>) {
        if (messages.isEmpty() && uiState.value is ChatUiState.Loading) { // Handle initial empty state
            _uiState.value = ChatUiState.Content(emptyList()) // Or keep Loading if you prefer
            return
        }

        val sortedMessages = messages.sortedBy { it.clientTimestamp } // Sort by client timestamp for now
        val chatItems = mutableListOf<ChatItem>()
        var lastDateHeaderTimestamp: Long? = null

        sortedMessages.forEach { message ->
            val messageCalendar = Calendar.getInstance().apply { timeInMillis = message.clientTimestamp }
            if (lastDateHeaderTimestamp == null || !isSameDay(message.clientTimestamp, lastDateHeaderTimestamp!!)) {
                chatItems.add(ChatItem.DateSeparator(formatDateSeparator(message.clientTimestamp), message.clientTimestamp))
                lastDateHeaderTimestamp = message.clientTimestamp
            }
            chatItems.add(ChatItem.Message(message))
        }
        _uiState.value = ChatUiState.Content(chatItems)
    }
    // --- Date Formatting Helpers (from Day 1) ---
    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun formatDateSeparator(timestamp: Long): String {
        val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            isSameDay(timestamp, today.timeInMillis) -> "Today"
            isSameDay(timestamp, yesterday.timeInMillis) -> "Yesterday"
            else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}