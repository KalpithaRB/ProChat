package com.kalpi.prochat.ui.presentations.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalpi.prochat.data.model.ChatRoom
import com.kalpi.prochat.data.repository.ChatRoomRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
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
            emit(ChatRoomListUiState.Loading)
            try {
                chatRoomRepository.listenToChatRooms(currentUserId)
                    .collect { chatRooms ->
                        emit(ChatRoomListUiState.Content(chatRooms))
                    }
            } catch (e: Exception) {
                Log.e("ChatRoomListViewModel", "Error fetching chat rooms", e)
                emit(ChatRoomListUiState.Error(e.message ?: "Unknown error"))
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChatRoomListUiState.Loading
        )


    // NEW: SharedFlow to send one-time UI events (like showing a dialog)
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class RoomCreated(val roomId: String) : UiEvent()
        data class RoomJoined(val roomId: String) : UiEvent()
        data class ShowToast(val message: String) : UiEvent()
    }


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
            if (recipientId.isNotBlank() && recipientId != currentUserId) {
                val participantIds = listOf(currentUserId, recipientId)
                chatRoomRepository.createChatRoom(roomName, participantIds)
                    .onSuccess { newRoomId ->
                        Log.d("ChatRoomListViewModel", "New room created with ID: $newRoomId")
                        // NEW: Emit the event to the UI
                        _uiEvent.emit(UiEvent.RoomCreated(newRoomId))
                    }.onFailure {
                        Log.e("ChatRoomListViewModel", "Failed to create new room", it)
                        _uiEvent.emit(UiEvent.ShowToast("Failed to create room. Please try again."))
                    }
            } else {
                Log.e("ChatRoomListViewModel", "Invalid recipient ID or self-chat attempt.")
                _uiEvent.emit(UiEvent.ShowToast("Please enter a valid recipient ID."))
            }
        }
    }

    /**
     * Allows the current user to join an existing chat room using its ID.
     */
    fun joinRoom(roomId: String) {
        viewModelScope.launch {
            if (roomId.isNotBlank()) {
                chatRoomRepository.joinChatRoom(currentUserId, roomId)
                    .onSuccess { joinedRoomId ->
                        Log.d("ChatRoomListViewModel", "Successfully joined room: $joinedRoomId")
                        // NEW: Emit an event to navigate to the joined room
                        _uiEvent.emit(UiEvent.RoomJoined(joinedRoomId))
                    }
                    .onFailure {
                        Log.e("ChatRoomListViewModel", "Failed to join room", it)
                        _uiEvent.emit(UiEvent.ShowToast("Failed to join room. Please check the ID."))
                    }
            } else {
                Log.e("ChatRoomListViewModel", "Room ID cannot be blank.")
                _uiEvent.emit(UiEvent.ShowToast("Room ID cannot be blank."))
            }
        }
    }

    private val uniqueUserId = currentUserId

    fun onToggleMute(roomId: String) {
        viewModelScope.launch {
            // We get the current list of rooms from the UI state
            val currentRooms = (uiState.value as? ChatRoomListUiState.Content)?.chatRooms ?: return@launch

            // Find the specific room the user clicked on
            val chatRoomToMute = currentRooms.find { it.roomId == roomId } ?: return@launch

            // Pass the userId to the repository function
            chatRoomRepository.toggleMuteStatus(uniqueUserId, roomId, !chatRoomToMute.muted)
        }
    }

    fun deleteChatroom(roomId: String) {
        viewModelScope.launch {
            // You can get the current user ID from the constructor, a repository, or FirebaseAuth.
            // For example, if you have `private val currentUserId: String` in the constructor:
            // Or if you get it from auth: val userId = auth.currentUser?.uid ?: return@launch

            chatRoomRepository.softDeleteChatroom(roomId, currentUserId).fold(
                onSuccess = {
                    // The UI will automatically update due to the real-time listener
                    // Show a success message to the user
                    _uiEvent.emit(UiEvent.ShowToast("Chatroom deleted successfully."))
                },
                onFailure = { e ->
                    // Show an error message to the user
                    _uiEvent.emit(UiEvent.ShowToast("Failed to delete chatroom: ${e.message}"))
                }
            )
        }
    }
}