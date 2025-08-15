package com.kalpi.prochat.data.repository
import com.kalpi.prochat.data.model.ChatRoom
import com.kalpi.prochat.data.model.Member
import com.kalpi.prochat.data.repository.ChatRoomRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * A fake implementation of ChatRoomRepository for testing purposes.
 */
class FakeChatRoomRepository : ChatRoomRepository {
    private val _chatRooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val chatRooms = _chatRooms.asStateFlow()

    // Control the return values for suspend functions
    var deleteChatroomResult: Result<Unit> = Result.success(Unit)
    var softDeleteChatroomResult: Result<Unit> = Result.success(Unit)
    var createChatRoomResult: Result<String> = Result.success("fake_room_id")
    var markRoomAsReadResult: Result<Unit> = Result.success(Unit)

    override fun listenToChatRooms(userId: String): Flow<List<ChatRoom>> {
        // This simulates the flow of chat rooms
        return chatRooms
    }

    // You can use this helper in your tests to simulate chat rooms appearing
    fun addChatRoom(chatRoom: ChatRoom) {
        _chatRooms.update { it + chatRoom }
    }

    override suspend fun getUnreadCountForRoom(roomId: String, lastReadTimestamp: Long): Int {
        // For testing, we can just return a hardcoded value or a predictable count
        return 1
    }

    suspend fun createChatRoom(roomName: String, participantIds: List<String>): Result<String> {
        return createChatRoomResult
    }

    // The ViewModel doesn't use this, but we include it for completeness
    override suspend fun joinChatRoom(userId: String, roomId: String): Result<String> {
        return Result.success(roomId)
    }

    override suspend fun toggleMuteStatus(userId: String, roomId: String, isMuted: Boolean) {
        // No-op for testing
    }

    override suspend fun deleteChatroomForUser(roomId: String, userId: String): Result<Unit> {
        return deleteChatroomResult
    }

    override suspend fun softDeleteChatroom(roomId: String, userId: String): Result<Unit> {
        return softDeleteChatroomResult
    }

    override suspend fun createGroupChatRoom(
        roomTitle: String,
        initialMembers: List<String>,
        createdBy: String
    ): Result<String> {
        TODO("Not yet implemented")
    }

    override suspend fun addMemberToGroup(
        roomId: String,
        userId: String,
        addedBy: String
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun removeMemberFromGroup(
        roomId: String,
        userId: String
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun leaveGroup(
        roomId: String,
        userId: String
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun getChatRoomDetails(roomId: String): ChatRoom? {
        TODO("Not yet implemented")
    }

    override suspend fun getMemberRole(
        roomId: String,
        userId: String
    ): String? {
        TODO("Not yet implemented")
    }

    override suspend fun transferOwnership(
        roomId: String,
        currentAdminId: String,
        newAdminId: String
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun getMembers(roomId: String): List<Member> {
        TODO("Not yet implemented")
    }

    override suspend fun markRoomAsRead(userId: String, roomId: String): Result<Unit> {
        return markRoomAsReadResult
    }

    override suspend fun createDirectChatRoom(
        roomName: String,
        participantIds: List<String>
    ): Result<String> {
        TODO("Not yet implemented")
    }
}