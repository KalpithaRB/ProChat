package com.kalpi.prochat.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.kalpi.prochat.data.model.ChatRoom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.google.firebase.firestore.Query
import com.kalpi.prochat.data.model.Member
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.callbackFlow

interface ChatRoomRepository{
    fun listenToChatRooms(userId: String): Flow<List<ChatRoom>>
    suspend fun getUnreadCountForRoom(roomId: String, lastReadTimestamp: Long): Int
    suspend fun markRoomAsRead(userId: String, roomId: String): Result<Unit>
    suspend fun createDirectChatRoom(roomName: String, participantIds: List<String>): Result<String>
    suspend fun joinChatRoom(userId: String, roomId: String): Result<String>
    suspend fun toggleMuteStatus(userId: String, roomId: String, isMuted: Boolean)
    suspend fun deleteChatroomForUser(roomId: String, userId: String): Result<Unit>
    suspend fun softDeleteChatroom(roomId: String, userId: String): Result<Unit>

    suspend fun createGroupChatRoom(roomTitle: String, initialMembers: List<String>, createdBy: String): Result<String>
    suspend fun addMemberToGroup(roomId: String, userId: String, addedBy: String): Result<Unit>
    suspend fun removeMemberFromGroup(roomId: String, userId: String): Result<Unit>

    suspend fun getChatRoomDetails(roomId: String): ChatRoom?


}

class RealChatRoomRepository(private val db: FirebaseFirestore) : ChatRoomRepository{

    companion object {
        private const val TAG = "ChatRoomRepository"
        private const val CHATROOMS_COLLECTION = "chatrooms"
        private const val USER_CHATROOMS_COLLECTION = "user_chat_rooms"
        private const val CHATROOMS_SUB_COLLECTION = "rooms"
        private const val MESSAGES_COLLECTION = "messages"
    }

    /**
     * Listens for a user's chat rooms in real-time and calculates the unread count for each.
     * This function now separates the concerns: the callbackFlow emits the raw list,
     * and the subsequent map operator enriches each item with the unread count.
     */
    override fun listenToChatRooms(userId: String): Flow<List<ChatRoom>> {
        val roomsFlow = callbackFlow {
            val roomsCollection = db.collection(USER_CHATROOMS_COLLECTION)
                .document(userId)
                .collection(CHATROOMS_SUB_COLLECTION)
                .orderBy("lastTimestamp", Query.Direction.DESCENDING)
                .whereEqualTo("isDeleted", false)

            val subscription = roomsCollection.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val chatRooms = snapshot.documents.mapNotNull { document ->
                        document.toObject(ChatRoom::class.java)
                    }
                    trySend(chatRooms)
                }
            }
            awaitClose { subscription.remove() }
        }

        // Now we map over the flow to get the full chatroom details and unread count.
        return roomsFlow.map { chatRooms ->
            coroutineScope {
                chatRooms.map { chatRoom ->
                    async {
                        val fullChatRoom = getChatRoomDetails(chatRoom.roomId)
                        val unreadCount = getUnreadCountForRoom(chatRoom.roomId, chatRoom.lastReadTimestamp)
                        // Copy the full details, then add the user-specific info like unread count
                        fullChatRoom?.copy(
                            unreadCount = unreadCount,
                            lastMessage = chatRoom.lastMessage,
                            lastTimestamp = chatRoom.lastTimestamp,
                            lastReadTimestamp = chatRoom.lastReadTimestamp,
                            muted = chatRoom.muted
                        ) ?: chatRoom.copy(unreadCount = unreadCount)
                    }
                }.awaitAll().filterNotNull()
            }
        }
    }

    /**
     * A suspend function to fetch the unread message count for a specific room.
     * It queries the messages collection for messages with a timestamp newer than the lastReadTimestamp.
     */
    override suspend fun getUnreadCountForRoom(roomId: String, lastReadTimestamp: Long): Int {
        return try {
            Log.d(TAG, "Getting unread count for room: $roomId. Comparing with lastReadTimestamp: $lastReadTimestamp")

            val querySnapshot = db.collection(CHATROOMS_COLLECTION)
                .document(roomId)
                .collection(MESSAGES_COLLECTION)
                .whereGreaterThan("clientTimestamp", lastReadTimestamp)
                .get()
                .await()

            Log.d(TAG, "Query returned ${querySnapshot.size()} new messages for room: $roomId")

            querySnapshot.size()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unread count for room $roomId", e)
            0
        }
    }

    /**
     * Marks a specific chat room as read by updating the lastReadTimestamp.
     */
    override suspend fun markRoomAsRead(userId: String, roomId: String): Result<Unit> {
        return try {
            db.collection(USER_CHATROOMS_COLLECTION)
                .document(userId)
                .collection(CHATROOMS_SUB_COLLECTION)
                .document(roomId)
                .update("lastReadTimestamp", System.currentTimeMillis())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking room as read", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a new chat room and adds it to the user's list.
     */
    override suspend fun createDirectChatRoom(roomName: String, participantIds: List<String>): Result<String> {
        return try {
            val newRoom = mapOf(
                // Use 'title' for consistency, but for DMs, it can be the other user's name
                "type" to "dm",
                "title" to roomName, // The name of the other user in a DM
                "createdAt" to System.currentTimeMillis(),
                "participants" to participantIds // Storing participants for DMs is fine
            )
            val newRoomRef = db.collection(CHATROOMS_COLLECTION).add(newRoom).await()
            val newRoomId = newRoomRef.id

            // Create a document for each participant
            participantIds.forEach { userId ->
                val userRoomData = mapOf(
                    "type" to "dm", // New: Add type to the user's room list
                    "title" to roomName, // New: Add title to the user's room list
                    "lastMessage" to "Room created.",
                    "lastTimestamp" to System.currentTimeMillis(),
                    "lastReadTimestamp" to System.currentTimeMillis(),
                    "isDeleted" to false
                )
                db.collection(USER_CHATROOMS_COLLECTION)
                    .document(userId)
                    .collection(CHATROOMS_SUB_COLLECTION)
                    .document(newRoomId)
                    .set(userRoomData)
                    .await()
            }

            Log.d(TAG, "Successfully created new DM room with ID: $newRoomId for participants: $participantIds")
            Result.success(newRoomId)

        } catch (e: Exception) {
            Log.e(TAG, "Error creating new chat room", e)
            Result.failure(e)
        }
    }

    override suspend fun createGroupChatRoom(
        roomTitle: String,
        initialMembers: List<String>,
        createdBy: String
    ): Result<String> {
        return try {
            // Step 1: Create a new group chat room document
            val newRoomData = ChatRoom(
                type = "group",
                title = roomTitle,
                createdBy = createdBy,
                createdAt = System.currentTimeMillis()
            )
            val newRoomRef = db.collection(CHATROOMS_COLLECTION).add(newRoomData).await()
            val newRoomId = newRoomRef.id

            // Step 2: Create a transaction to add initial members and update user rooms
            db.runTransaction { transaction ->
                // Add initial members to the 'members' subcollection
                for (userId in initialMembers) {
                    val memberRole = if (userId == createdBy) "admin" else "member"
                    val memberData = Member(
                        userId = userId,
                        role = memberRole,
                        joinedAt = System.currentTimeMillis()
                    )
                    val memberDocRef = newRoomRef.collection("members").document(userId)
                    transaction.set(memberDocRef, memberData)
                }

                // Update user's list of rooms for all participants
                for (userId in initialMembers) {
                    val userRoomData = mapOf(
                        "roomId" to newRoomId,
                        "type" to "group",
                        "title" to roomTitle,
                        "lastMessage" to "Group created.",
                        "lastTimestamp" to System.currentTimeMillis(),
                        "lastReadTimestamp" to System.currentTimeMillis(),
                        "isDeleted" to false
                    )
                    val userChatRoomDocRef = db.collection(USER_CHATROOMS_COLLECTION)
                        .document(userId)
                        .collection(CHATROOMS_SUB_COLLECTION)
                        .document(newRoomId)
                    transaction.set(userChatRoomDocRef, userRoomData)
                }
                null // Transaction success
            }.await()

            Log.d(TAG, "Successfully created new group room with ID: $newRoomId")
            Result.success(newRoomId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating new group chat room", e)
            Result.failure(e)
        }
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

    override suspend fun getChatRoomDetails(roomId: String): ChatRoom? {
        return try {
            val document = db.collection(CHATROOMS_COLLECTION).document(roomId).get().await()
            document.toObject(ChatRoom::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting chat room details for room $roomId", e)
            null
        }
    }

    /**
     * Allows a user to join an existing chat room using its ID.
     * It first verifies the room exists and then adds it to the user's chat list.
     */
    override suspend fun joinChatRoom(userId: String, roomId: String): Result<String> {
        return try {
            // First, get the room's details from the main chatrooms collection
            val roomDocument = db.collection(CHATROOMS_COLLECTION)
                .document(roomId)
                .get()
                .await()

            if (!roomDocument.exists()) {
                Log.e(TAG, "Chat room with ID $roomId does not exist.")
                return Result.failure(Exception("Chat room not found."))
            }

            val roomName = roomDocument.getString("name") ?: "Unnamed Room"

            // Now, add the room to the user's specific chat list
            val userRoomData = mapOf(
                "name" to roomName,
                "lastMessage" to "Joined room.",
                "lastTimestamp" to System.currentTimeMillis(),
                "lastReadTimestamp" to System.currentTimeMillis(),
                "isDeleted" to false
            )

            db.collection(USER_CHATROOMS_COLLECTION)
                .document(userId)
                .collection(CHATROOMS_SUB_COLLECTION)
                .document(roomId)
                .set(userRoomData)
                .await()

            Log.d(TAG, "Successfully joined room with ID: $roomId")
            Result.success(roomId)
        } catch (e: Exception) {
            Log.e(TAG, "Error joining chat room", e)
            Result.failure(e)
        }
    }

    override suspend fun toggleMuteStatus(userId: String, roomId: String, isMuted: Boolean) {
        try {
            db.collection(USER_CHATROOMS_COLLECTION)
                .document(userId)
                .collection(CHATROOMS_SUB_COLLECTION)
                .document(roomId)
                .update("muted", isMuted)
                .await()
        } catch (e: Exception) {
            Log.e("ChatRoomRepository", "Error toggling mute status for room $roomId", e)
        }
    }

    override suspend fun deleteChatroomForUser(roomId: String, userId: String): Result<Unit> =
        try {
            // First, delete all messages in the chatroom
            val messagesCollection = db.collection("chatrooms").document(roomId).collection("messages")
            val batch = db.batch()
            val messagesSnapshot = messagesCollection.get().await()

            for (document in messagesSnapshot.documents) {
                batch.delete(document.reference)
            }
            batch.commit().await() // Commit the deletion of all messages

            // Then, remove the chatroom document reference from the user's chatroom list
            db.collection("users").document(userId).collection("chatrooms").document(roomId).delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun softDeleteChatroom(roomId: String, userId: String): Result<Unit> =
        try {
            db.collection(USER_CHATROOMS_COLLECTION)
                .document(userId)
                .collection(CHATROOMS_SUB_COLLECTION)
                .document(roomId)
                .update("isDeleted", true)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRoomRepository", "Error soft-deleting chatroom", e)
            Result.failure(e)
        }
}