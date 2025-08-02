package com.kalpi.prochat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kalpi.prochat.data.ChatRepository

/**
 * Factory for creating instances of ChatViewModel with dependencies.
 */
class ChatViewModelFactory(
    private val chatRepository: ChatRepository,
    private val roomId: String,
    private val currentUserId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(chatRepository, roomId, currentUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
