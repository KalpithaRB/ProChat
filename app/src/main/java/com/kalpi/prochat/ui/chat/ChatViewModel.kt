package com.kalpi.prochat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalpi.prochat.data.ChatRepository
import com.kalpi.prochat.data.ChatItem
import com.kalpi.prochat.data.ChatMessage
import com.kalpi.prochat.data.MessageStatus
import com.kalpi.prochat.data.MessageType

import com.kalpi.prochat.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch // For error handling on the flow
import kotlinx.coroutines.flow.onStart // Optional: for indicating initial loading
import kotlinx.coroutines.Job // To manage the listener job
import kotlinx.coroutines.flow.update
import java.util.UUID // For generating unique IDs for dummy messages
import java.text.SimpleDateFormat // Keep for MessageBubble timestamp
import java.util.Calendar
import java.util.Locale         // Keep for MessageBubble timestamp
import java.util.Date
import android.util.Log


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
    private var messageListenerJob: Job? = null
    init {
        //i am keeping this screen empty and removed the dummy data
        // and the new msgs will be getting appended as they are sent.
        processMessagesAndUpdateState(emptyList())
        listenToMessages(currentRoomId)
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

        // Optimistically add to _tempMessageStore and update UI
        val updatedMessages = (_tempMessageStore.value + newMessage).sortedBy { it.clientTimestamp }
        _tempMessageStore.value = updatedMessages
        processMessagesAndUpdateState(updatedMessages) // Update UI immediately


        viewModelScope.launch {
            val result = chatRepository.sendMessage(currentRoomId, newMessage)
            val finalStatus = if (result.isSuccess) MessageStatus.SENT else MessageStatus.FAILED

            // Update the status in _tempMessageStore
            // The listener might also pick this up, the distinctBy in collect should handle it.
            val messagesAfterSendAttempt = _tempMessageStore.value.map {
                if (it.id == newMessage.id) it.copy(status = finalStatus) else it
            }.sortedBy { it.clientTimestamp }

            _tempMessageStore.value = messagesAfterSendAttempt
            processMessagesAndUpdateState(messagesAfterSendAttempt) // Update UI with final status

            if (result.isFailure) {
                Log.e("ChatViewModel", "Failed to send message ID: ${newMessage.id}", result.exceptionOrNull())
            }
        }
    }

    private fun listenToMessages(roomId: String) {
        messageListenerJob?.cancel() // Cancel previous listener if any
        messageListenerJob = viewModelScope.launch {
            chatRepository.getChatMessages(roomId)
                .onStart {
                    Log.d("ChatViewModel", "Starting to listen for messages in room: $roomId")
                    // You might want to ensure UI is in Loading state or some indication
                    // if _tempMessageStore is empty and _uiState is not already Content.
                    // For now, processMessagesAndUpdateState(emptyList()) in init handles initial state.
                }
                .catch { exception ->
                    Log.e("ChatViewModel", "Error listening to messages for room $roomId", exception)
                    // Optionally update UI to show an error state
                    // For example: _uiState.value = ChatUiState.Error("Failed to load messages: ${exception.message}")
                    // Ensure your ChatUiState sealed class has an Error state if you do this.
                }
                .collect { newMessagesFromListener ->
                    Log.d("ChatViewModel", "Received ${newMessagesFromListener.size} messages from listener for room $roomId")

                    // Get current messages, especially those in SENDING state, from our temp store
                    val currentLocalMessages = _tempMessageStore.value

                    // Combine: Prioritize messages from the listener (source of truth).
                    // If a message ID exists in both, the one from the listener is usually preferred.
                    // However, for SENDING messages, we want to keep our local status until the listener
                    // confirms it as SENT or it's updated by sendMessage completion.

                    val combinedMessagesMap = mutableMapOf<String, ChatMessage>()

                    // Add all current local messages
                    for (msg in currentLocalMessages) {
                        combinedMessagesMap[msg.id] = msg
                    }
                    // Add/replace with messages from the listener
                    // If a message from listener has status SENT/FAILED, it overrides local SENDING.
                    for (listenerMsg in newMessagesFromListener) {
                        val localMsg = combinedMessagesMap[listenerMsg.id]
                        if (localMsg != null && localMsg.status == MessageStatus.SENDING &&
                            (listenerMsg.status == MessageStatus.SENT || listenerMsg.status == MessageStatus.FAILED)) {
                            // Listener confirms a message that was locally SENDING
                            combinedMessagesMap[listenerMsg.id] = listenerMsg
                        } else if (localMsg == null) {
                            // New message from listener not seen before
                            combinedMessagesMap[listenerMsg.id] = listenerMsg
                        }
                        // If localMsg.status is already SENT/FAILED, and listenerMsg is the same, no change needed.
                        // If listenerMsg is somehow older (e.g. status NONE), prefer local if more definitive.
                        // This logic can get complex depending on how server timestamps and statuses are handled.
                        // For now, simpler: listener's version usually wins if ID matches, unless local is SENDING.
                        else if (localMsg.status != MessageStatus.SENDING) { // local is already SENT/FAILED
                            combinedMessagesMap[listenerMsg.id] = listenerMsg // keep listener version
                        }
                        // else: local is SENDING, listener is also SENDING or NONE, keep local SENDING
                    }


                    val finalMessages = combinedMessagesMap.values.toList().sortedBy { it.clientTimestamp }

                    _tempMessageStore.value = finalMessages
                    processMessagesAndUpdateState(finalMessages)
                }
        }
    }


    fun retrySendMessage(failedMessage: ChatMessage) {
        if (failedMessage.status != MessageStatus.FAILED) return // Only retry failed messages

        val messageToRetry = failedMessage.copy(status = MessageStatus.SENDING)

        // Optimistically update status in _tempMessageStore
        val messagesWithRetry = _tempMessageStore.value.map {
            if (it.id == failedMessage.id) messageToRetry else it
        }.sortedBy { it.clientTimestamp }
        _tempMessageStore.value = messagesWithRetry
        processMessagesAndUpdateState(messagesWithRetry)

        viewModelScope.launch {
            // Use the original failedMessage object, as its ID is what matters for Firestore document path
            val result = chatRepository.sendMessage(messageToRetry.roomId, messageToRetry)
            val finalStatus = if (result.isSuccess) MessageStatus.SENT else MessageStatus.FAILED

            val messagesAfterRetryAttempt = _tempMessageStore.value.map {
                if (it.id == messageToRetry.id) it.copy(status = finalStatus) else it
            }.sortedBy { it.clientTimestamp }
            _tempMessageStore.value = messagesAfterRetryAttempt
            processMessagesAndUpdateState(messagesAfterRetryAttempt)

            if (result.isFailure) {
                Log.e("ChatViewModel", "Failed to retry message ID: ${messageToRetry.id}", result.exceptionOrNull())
            }
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

    override fun onCleared() {
        super.onCleared()
        messageListenerJob?.cancel()
        Log.d("ChatViewModel", "ViewModel cleared, message listener job cancelled for room: $currentRoomId")
    }
}