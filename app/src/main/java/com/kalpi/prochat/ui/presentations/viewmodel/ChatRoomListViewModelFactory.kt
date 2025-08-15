package com.kalpi.prochat.ui.presentations.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kalpi.prochat.data.repository.ChatRoomRepository
import com.kalpi.prochat.data.repository.UserRepository

class ChatRoomListViewModelFactory(
    private val chatRoomRepository: ChatRoomRepository,
    private val userRepository: UserRepository,
    private val currentUserId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatRoomListViewModel::class.java)) {
            return ChatRoomListViewModel(chatRoomRepository, userRepository, currentUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}