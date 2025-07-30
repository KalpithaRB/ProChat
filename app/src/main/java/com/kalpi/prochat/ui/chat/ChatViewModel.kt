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

    init {
        //i am keeping this screen empty and removed the dummy data
        // and the new msgs will be getting appended as they are sent.
        processMessagesAndUpdateState(emptyList())
    }

    /**
     * Simulates loading messages.
     * In a real app, this would involve fetching data from a repository or data source.
     * For Day 1, it populates the state with a predefined list of dummy messages.
     */


    // Sending a message
    // --- Message Sending Logic ---
    fun sendMessage(text: String) {
        if (text.isBlank()) return // Don't send empty messages

        val newMessage = ChatMessage(
            id = UUID.randomUUID().toString(), // Client-generated unique ID
            senderId = CURRENT_USER_ID,
            text = text.trim(),
            clientTimestamp = System.currentTimeMillis(),
            messageType = MessageType.USER,
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

    fun retrySendMessage(failedMessage: ChatMessage) {
        if (failedMessage.status != MessageStatus.FAILED) return // Only retry failed messages

        // Optimistically update status to SENDING again
        val messagesWithRetry = _tempMessageStore.value.map {
            if (it.id == failedMessage.id) {
                it.copy(status = MessageStatus.SENDING)
            } else {
                it
            }
        }
        _tempMessageStore.value = messagesWithRetry
        processMessagesAndUpdateState(messagesWithRetry)

        viewModelScope.launch {
            // Use the original failedMessage object, as its ID is what matters for Firestore document path
            val result = chatRepository.sendMessage(failedMessage.roomId, failedMessage.copy(status = MessageStatus.SENDING))
            val finalStatus = if (result.isSuccess) MessageStatus.SENT else MessageStatus.FAILED

            val messagesAfterRetryAttempt = _tempMessageStore.value.map {
                if (it.id == failedMessage.id) {
                    it.copy(status = finalStatus)
                } else {
                    it
                }
            }
            _tempMessageStore.value = messagesAfterRetryAttempt
            processMessagesAndUpdateState(messagesAfterRetryAttempt)
        }
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