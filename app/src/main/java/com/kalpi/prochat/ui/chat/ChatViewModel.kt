package com.kalpi.prochat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalpi.prochat.data.ChatMessage
import com.kalpi.prochat.data.MessageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID // For generating unique IDs for dummy messages


/**
 * ViewModel for the ChatScreen.
 *
 * This ViewModel is responsible for preparing and managing the UI-related data for the ChatScreen.
 * It exposes the chat messages and UI state through [StateFlow].
 *
 * For Day 1, it uses a predefined list of dummy messages.
 * In later stages, it will interact with a data source (e.g., Firebase) to fetch and send messages.
 */

class ChatViewModel : ViewModel() {

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

    init {
        loadDummyMessages()
    }

    /**
     * Simulates loading messages.
     * In a real app, this would involve fetching data from a repository or data source.
     * For Day 1, it populates the state with a predefined list of dummy messages.
     */
    private fun loadDummyMessages() {
        viewModelScope.launch {
            // Simulate a small delay for loading, can be removed if not desired
            // kotlinx.coroutines.delay(500)

            val dummyMessages = listOf(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    senderId = OTHER_USER_ID,
                    text = "Hey, how are you doing today?",
                    timestamp = System.currentTimeMillis() - 100000, // Approx 1 min 40 secs ago
                    messageType = MessageType.USER
                ),
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    senderId = CURRENT_USER_ID,
                    text = "I'm good, thanks! Just working on this cool chat app. You?",
                    timestamp = System.currentTimeMillis() - 80000, // Approx 1 min 20 secs ago
                    messageType = MessageType.USER
                ),
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    senderId = OTHER_USER_ID,
                    text = "Oh nice! Sounds interesting. What features are you building?",
                    timestamp = System.currentTimeMillis() - 60000, // Approx 1 min ago
                    messageType = MessageType.USER
                ),
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    senderId = CURRENT_USER_ID,
                    text = "Right now, just the basic UI layout with message bubbles and an input field. Trying to make it look polished!",
                    timestamp = System.currentTimeMillis() - 40000, // Approx 40 secs ago
                    messageType = MessageType.USER
                ),
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    senderId = "system", // Or a specific ID for system messages
                    text = "Alice has joined the chat.",
                    timestamp = System.currentTimeMillis() - 30000, // Approx 30 secs ago
                    messageType = MessageType.SYSTEM // Example of a system message (Stretch Goal)
                ),
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    senderId = OTHER_USER_ID,
                    text = "Cool! Keep it up!",
                    timestamp = System.currentTimeMillis() - 20000, // Approx 20 secs ago
                    messageType = MessageType.USER
                )
            )
            _uiState.value = ChatUiState.Content(dummyMessages)
        }
    }

    // Placeholder for Day 2: Sending a message
    // fun sendMessage(text: String) {
    //     // Logic to create a new ChatMessage and update Firebase/local list
    //     // For now, could just add to a local list and update _uiState
    // }
}