package com.kalpi.prochat.ui.presentations.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kalpi.prochat.data.repository.ChatRoomRepository

class ChatRoomListViewModelFactory(
    private val chatRoomRepository: ChatRoomRepository,
    private val currentUserId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatRoomListViewModel::class.java)) {
            return ChatRoomListViewModel(chatRoomRepository, currentUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}