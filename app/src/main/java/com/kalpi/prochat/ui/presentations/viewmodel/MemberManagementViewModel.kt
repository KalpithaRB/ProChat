package com.kalpi.prochat.ui.presentations.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kalpi.prochat.data.model.Member
import com.kalpi.prochat.data.repository.ChatRoomRepository
import com.kalpi.prochat.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.kalpi.prochat.data.model.UiMember
import com.kalpi.prochat.data.repository.PresenceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.collections.emptyList


class MemberManagementViewModel(
    private val chatRoomRepository: ChatRoomRepository,
    private val userRepository: UserRepository,
    private val presenceRepository: PresenceRepository,
    private val roomId: String,
    private val currentUserId: String
) : ViewModel() {

    // State to hold the list of members in the room
//    private val _members = MutableStateFlow<List<Member>>(emptyList())
//    val members: StateFlow<List<Member>> = _members

    // State to track the current user's role
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    // One-time events for UI (e.g., showing a toast)
    private val _uiEvent = MutableStateFlow<UiEvent>(UiEvent.Idle)
    val uiEvent: StateFlow<UiEvent> = _uiEvent

    private val _currentUserId = MutableStateFlow("")
    val currenUserId: StateFlow<String> = _currentUserId



    init {
        _currentUserId.value = currentUserId
        // ONLY proceed if the roomId is not blank
        if (roomId.isNotBlank()) {

            checkUserRole()
            listenToChatRoomDetails()
        } else {
            // Handle the error case gracefully, e.g., show a toast.
            // You can't navigate back from the ViewModel, so emitting an event is the way.
            viewModelScope.launch {
                _uiEvent.value = UiEvent.NavigateBack("Failed to load members: invalid chat room ID.")
            }
        }
    }

    // A flow that emits the list of raw members from the chat room's subcollection
    private val membersFlow = chatRoomRepository.listenToMembers(roomId)

    // A flow that emits a list of all member IDs
    private val memberIdsFlow = membersFlow
        .map { members -> members.map { it.userId } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // A flow that fetches user details for each member ID
    @OptIn(ExperimentalCoroutinesApi::class)
    private val usersMapFlow = memberIdsFlow.flatMapLatest { ids ->
        if (ids.isEmpty()) flowOf(emptyMap())
        else userRepository.listenToUsers(ids)
    }

    // A flow that listens to the presence status for all members
    @OptIn(ExperimentalCoroutinesApi::class)
    private val presenceStatusFlow = memberIdsFlow.flatMapLatest { ids ->
        if (ids.isEmpty()) flowOf(emptyMap())
        else presenceRepository.listenToPresence(ids)
    }

    // ⭐ NEW: flows to hold the chat room's title and avatar URL
    private val _chatRoomTitle = MutableStateFlow("")
    val chatRoomTitle: StateFlow<String> = _chatRoomTitle

    private val _chatRoomAvatarUrl = MutableStateFlow<String?>(null)
    val chatRoomAvatarUrl: StateFlow<String?> = _chatRoomAvatarUrl


    // The single, combined state flow for the UI. This is the key.
    val uiMembers: StateFlow<List<UiMember>> =
        combine(membersFlow, usersMapFlow, presenceStatusFlow ) { members, usersMap, presenceMap ->
            members.mapNotNull { member ->
                val user = usersMap[member.userId]
                if (user != null) {
                    val lastActiveTimestamp = presenceMap[member.userId]
                    val isOnline = lastActiveTimestamp?.let {
                        // A user is considered online if their last active timestamp is within 5 minutes.
                        System.currentTimeMillis() - it < (5 * 60 * 1000)
                    } ?: false

                    UiMember(
                        userId = user.userId,
                        name = user.name,
                        role = member.role,
                        isCurrentUser = member.userId == currentUserId,
                        isAdmin = member.role == "admin",
                        isMuted = member.isMuted,
                        joinedAt = member.joinedAt,
                        isOnline = isOnline
                    )
                } else {
                    null
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun checkUserRole() {
        viewModelScope.launch {
            val role = chatRoomRepository.getMemberRole(roomId, currentUserId)
            _isAdmin.value = role == "admin"
        }
    }

    private fun listenToChatRoomDetails() {
        viewModelScope.launch {
            chatRoomRepository.listenToChatRoom(roomId)
                .collect { chatRoom ->
                    if (chatRoom != null) {
                        _chatRoomTitle.value = chatRoom.title
                        _chatRoomAvatarUrl.value = chatRoom.avatarUrl
                    }
                }
        }
    }

    fun addMember(newMemberId: String) {
        viewModelScope.launch {
            val result = chatRoomRepository.addMemberToGroup(roomId, newMemberId, currentUserId)
            if (result.isSuccess) {
                _uiEvent.value = UiEvent.ShowToast("Member added successfully!")
//                fetchMembers() // Refresh the list
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
//                fetchMembers()
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
                val errorMessage = result.exceptionOrNull()?.message ?: "Failed to leave group."
                if (errorMessage.contains("Admins must transfer ownership")) {
                    _uiEvent.value = UiEvent.ShowAdminLeaveWarning(errorMessage)
                } else {
                    _uiEvent.value = UiEvent.ShowToast(errorMessage)
                }
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
        data class ShowAdminLeaveWarning(val message: String) : UiEvent()
    }

    data class UiMember(
        val userId: String,
        val name: String,
        val role: String,
        val isCurrentUser: Boolean,
        val isAdmin: Boolean,
        val isMuted: Boolean,
        val joinedAt: Long,
        val isOnline: Boolean = false
    )
}

class MemberManagementViewModelFactory(
    private val chatRoomRepository: ChatRoomRepository,
    private val userRepository: UserRepository,
    private val presenceRepository: PresenceRepository,
    private val roomId: String,
    private val currentUserId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MemberManagementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MemberManagementViewModel(
                chatRoomRepository,
                userRepository,
                presenceRepository,
                roomId,
                currentUserId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}