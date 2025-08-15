package com.kalpi.prochat.ui.presentations.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalpi.prochat.data.model.ChatRoom
import com.kalpi.prochat.data.repository.ChatRoomRepository
import com.kalpi.prochat.data.repository.UserRepository
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
    private val userRepository: UserRepository,
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


    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    sealed class UiEvent {
        data class RoomCreated(val roomId: String) : UiEvent()
        data class RoomJoined(val roomId: String) : UiEvent()
        data class ShowToast(val message: String) : UiEvent()
        data class RoomCreatedAndNavigate(val roomId: String, val roomName: String) : UiEvent()
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
     * Creates a new chat room.
     * This function now uses either createDirectChatRoom or createGroupChatRoom
     * based on the isGroupChat parameter.
     */
    fun createNewRoom(roomName: String, recipientId: String, isGroupChat: Boolean) {
        viewModelScope.launch {
            // Validate input
            if (recipientId.isBlank() || recipientId == currentUserId) {
                _uiEvent.emit(UiEvent.ShowToast("Invalid recipient ID."))
                return@launch
            }

            // Check if recipient exists using the new UserRepository function
            val recipientExists = try {
                userRepository.getUserById(recipientId) != null
            } catch (e: Exception) {
                Log.e("ChatRoomListViewModel", "Error checking recipient existence", e)
                false
            }

            if (!recipientExists) {
                _uiEvent.emit(UiEvent.ShowToast("User with that ID does not exist."))
                return@launch
            }

            // Corrected logic: Call the specific repository function based on isGroupChat
            val result = if (isGroupChat) {
                val participantIds = listOf(currentUserId, recipientId)
                chatRoomRepository.createGroupChatRoom(roomName, participantIds, currentUserId)
            } else {
                val participantIds = listOf(currentUserId, recipientId)
                chatRoomRepository.createDirectChatRoom(roomName, participantIds)
            }

            result.onSuccess { newRoomId ->
                Log.d("ChatRoomListViewModel", "New room created with ID: $newRoomId")
                if (isGroupChat) {
                    _uiEvent.emit(UiEvent.RoomCreatedAndNavigate(newRoomId, roomName))
                } else {
                    _uiEvent.emit(UiEvent.RoomCreated(newRoomId))
                }
            }.onFailure {
                Log.e("ChatRoomListViewModel", "Failed to create new room", it)
                _uiEvent.emit(UiEvent.ShowToast("Failed to create room. Please try again."))
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
            // Find the chatroom from the current UI state
            val chatRooms = (uiState.value as? ChatRoomListUiState.Content)?.chatRooms ?: return@launch
            val chatRoomToUpdate = chatRooms.firstOrNull { it.roomId == roomId } ?: return@launch
            val newIsMutedState = !chatRoomToUpdate.muted // Assuming the field is named `muted`

            // CORRECTED: Pass all three arguments to the repository function
            chatRoomRepository.toggleMuteStatus(
                userId = currentUserId, // This is the missing argument
                roomId = roomId,
                isMuted = newIsMutedState
            )

            // Determine the toast message based on the new state
            val message = if (newIsMutedState) {
                "${chatRoomToUpdate.title} is muted."
            } else {
                "${chatRoomToUpdate.title} is unmuted."
            }

            // Emit a UiEvent to show the toast
            _uiEvent.emit(UiEvent.ShowToast(message))
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