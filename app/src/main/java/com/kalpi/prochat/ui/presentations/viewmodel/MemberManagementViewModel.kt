package com.kalpi.prochat.ui.presentations.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kalpi.prochat.data.model.Member
import com.kalpi.prochat.data.repository.ChatRoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log

class MemberManagementViewModel(
    private val chatRoomRepository: ChatRoomRepository,
    private val roomId: String,
    private val currentUserId: String
) : ViewModel() {

    // State to hold the list of members in the room
    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: StateFlow<List<Member>> = _members

    // State to track the current user's role
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    // One-time events for UI (e.g., showing a toast)
    private val _uiEvent = MutableStateFlow<UiEvent>(UiEvent.Idle)
    val uiEvent: StateFlow<UiEvent> = _uiEvent

    init {
        // Fetch and listen for real-time updates to members
        fetchMembers()
        // Check user's role on startup
        checkUserRole()
    }

    private fun fetchMembers() {
        // TODO: Implement a function in ChatRoomRepository to listen to members
        // For now, we'll use a one-time fetch.
        viewModelScope.launch {
            // For now, you'll need to create a function in your repository to fetch this.
            // Let's assume you'll add 'suspend fun getMembers(roomId: String): List<Member>'
             _members.value = chatRoomRepository.getMembers(roomId)
        }
    }

    private fun checkUserRole() {
        viewModelScope.launch {
            val role = chatRoomRepository.getMemberRole(roomId, currentUserId)
            _isAdmin.value = role == "admin"
        }
    }

    fun addMember(newMemberId: String) {
        viewModelScope.launch {
            val result = chatRoomRepository.addMemberToGroup(roomId, newMemberId, currentUserId)
            if (result.isSuccess) {
                _uiEvent.value = UiEvent.ShowToast("Member added successfully!")
                fetchMembers() // Refresh the list
            } else {
                _uiEvent.value = UiEvent.ShowToast(result.exceptionOrNull()?.message ?: "Failed to add member.")
            }
        }
    }

    fun removeMember(memberIdToRemove: String) {
        viewModelScope.launch {
            val result = chatRoomRepository.removeMemberFromGroup(roomId, memberIdToRemove)
            if (result.isSuccess) {
                _uiEvent.value = UiEvent.ShowToast("Member removed.")
                fetchMembers()
            } else {
                _uiEvent.value = UiEvent.ShowToast(result.exceptionOrNull()?.message ?: "Failed to remove member.")
            }
        }
    }

    fun leaveGroup() {
        viewModelScope.launch {
            val result = chatRoomRepository.leaveGroup(roomId, currentUserId)
            if (result.isSuccess) {
                _uiEvent.value = UiEvent.NavigateBack("You have left the group.")
            } else {
                _uiEvent.value = UiEvent.ShowToast(result.exceptionOrNull()?.message ?: "Failed to leave group.")
            }
        }
    }

    fun transferOwnership(newAdminId: String) {
        viewModelScope.launch {
            val result = chatRoomRepository.transferOwnership(roomId, currentUserId, newAdminId)
            if (result.isSuccess) {
                _uiEvent.value = UiEvent.NavigateBack("Ownership transferred successfully.")
            } else {
                _uiEvent.value = UiEvent.ShowToast(result.exceptionOrNull()?.message ?: "Failed to transfer ownership.")
            }
        }
    }

    sealed class UiEvent {
        data object Idle : UiEvent()
        data class ShowToast(val message: String) : UiEvent()
        data class NavigateBack(val message: String) : UiEvent()
    }
}

class MemberManagementViewModelFactory(
    private val chatRoomRepository: ChatRoomRepository,
    private val roomId: String,
    private val currentUserId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MemberManagementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MemberManagementViewModel(chatRoomRepository, roomId, currentUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}