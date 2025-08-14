package com.kalpi.prochat.ui.presentations.viewmodel

import androidx.lifecycle.ViewModel
import android.app.Application
import androidx.lifecycle.ViewModelProvider
import com.kalpi.prochat.data.repository.ChatRepository
import com.kalpi.prochat.data.repository.ChatRoomRepository
import com.kalpi.prochat.utils.NetworkStatusObserver
import com.kalpi.prochat.utils.RealNetworkStatusObserver

/**
 * Factory for creating instances of ChatViewModel with dependencies.
 */
class ChatViewModelFactory(
    private val application: Application,
    private val chatRepository: ChatRepository,
    private val chatRoomRepository: ChatRoomRepository,
    private val roomId: String,
    private val currentUserId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            val networkStatusObserver = RealNetworkStatusObserver(application)
            return ChatViewModel(
                application,
                chatRepository,
                chatRoomRepository,
                networkStatusObserver,
                roomId,
                currentUserId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}