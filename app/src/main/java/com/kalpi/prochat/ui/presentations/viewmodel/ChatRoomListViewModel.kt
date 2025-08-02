package com.kalpi.prochat.ui.presentations.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalpi.prochat.data.model.ChatRoom
import com.kalpi.prochat.data.repository.ChatRoomRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.flow.flow

// Sealed class to represent the UI state of the chatroom list
sealed class ChatRoomListUiState {
    object Loading : ChatRoomListUiState()
    data class Content(val chatRooms: List<ChatRoom>) : ChatRoomListUiState()
    data class Error(val errorMessage: String) : ChatRoomListUiState()
}

class ChatRoomListViewModel(
    private val chatRoomRepository: ChatRoomRepository,
    private val currentUserId: String
) : ViewModel() {

    val uiState: StateFlow<ChatRoomListUiState> =
        flow {
            // Start by emitting the loading state
            emit(ChatRoomListUiState.Loading)
            // Listen to the real-time data from the repository
            chatRoomRepository.listenToChatRooms(currentUserId)
                .collect { chatRooms ->
                    // On each new list of rooms, emit the content state
                    emit(ChatRoomListUiState.Content(chatRooms))
                }
        }
            .catch { e ->
                // If any error occurs in the flow, emit the error state
                Log.e("ChatRoomListViewModel", "Error fetching chat rooms", e)
                emit(ChatRoomListUiState.Error(e.message ?: "Unknown error"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ChatRoomListUiState.Loading // This initialValue is no longer needed but kept for robustness
            )

    /**
     * Public function for the UI to call to mark a room as read.
     */
    fun onRoomClicked(roomId: String) {
        viewModelScope.launch {
            chatRoomRepository.markRoomAsRead(currentUserId, roomId)
        }
    }

    /**
     * Creates a new chat room with the given name and recipient ID.
     */
    fun createNewRoom(roomName: String, recipientId: String) {
        viewModelScope.launch {
            // Basic validation to ensure we have a recipient
            if (recipientId.isNotBlank() && recipientId != currentUserId) {
                // Combine the current user and the recipient into a list
                val participantIds = listOf(currentUserId, recipientId)
                chatRoomRepository.createChatRoom(roomName, participantIds)
                    .onSuccess { newRoomId ->
                        Log.d("ChatRoomListViewModel", "New room created with ID: $newRoomId")
                        // TODO: Navigate to the new room.
                    }.onFailure {
                        Log.e("ChatRoomListViewModel", "Failed to create new room", it)
                    }
            } else {
                Log.e("ChatRoomListViewModel", "Invalid recipient ID or self-chat attempt.")
            }
        }
    }
}