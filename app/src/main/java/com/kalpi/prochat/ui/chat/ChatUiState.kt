package com.kalpi.prochat.ui.chat


import com.kalpi.prochat.data.ChatMessage

/**
 * Represents the different states the ChatScreen can be in.
 * This follows the principles of a sealed interface to model UI states,
 * ensuring all possible states are handled.
 */
sealed interface ChatUiState { // Using sealed interface for modern Kotlin
    /**
     * Represents the loading state, typically shown when messages are being fetched.
     */
    object Loading : ChatUiState

    /**
     * Represents the state where chat messages are successfully loaded and ready to be displayed.
     * @property messages The list of [ChatMessage] objects to display.
     */
    data class Content(val messages: List<ChatMessage>) : ChatUiState

    /**
     * Represents an error state, typically shown if fetching messages fails.
     * @property errorMessage A user-friendly message describing the error.
     */
    data class Error(val errorMessage: String) : ChatUiState
}