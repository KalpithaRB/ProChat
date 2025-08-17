package com.kalpi.prochat.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.kalpi.prochat.data.model.ChatRoom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.util.Log
import androidx.annotation.Keep
import com.google.firebase.firestore.Query
import com.kalpi.prochat.data.model.Member
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking

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
    suspend fun leaveGroup(roomId: String, userId: String): Result<Unit>
    suspend fun getChatRoomDetails(roomId: String): ChatRoom?
    suspend fun getMemberRole(roomId: String, userId: String): String?
    suspend fun transferOwnership(roomId: String, currentAdminId: String, newAdminId: String): Result<Unit>

    suspend fun getMembers(roomId: String): List<Member>


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
        val userRoomsQuery = db.collection(USER_CHATROOMS_COLLECTION)
            .document(userId)
            .collection(CHATROOMS_SUB_COLLECTION)
            .orderBy("lastTimestamp", Query.Direction.DESCENDING)
            .whereEqualTo("isDeleted", false)

        return callbackFlow {
            val subscription = userRoomsQuery.addSnapshotListener { userRoomsSnapshot, userError ->
                if (userError != null) {
                    close(userError)
                    return@addSnapshotListener
                }

                if (userRoomsSnapshot != null) {
                    val deferredChatRooms = userRoomsSnapshot.documents.map { userRoomDoc ->
                        async(Dispatchers.IO) {
                            val roomId = userRoomDoc.id

                            // Step 1: Fetch the full chat room details from the main collection
                            val mainChatRoomDoc = db.collection(CHATROOMS_COLLECTION)
                                .document(roomId)
                                .get()
                                .await()

                            // Step 2: IMPORTANT! Check if the document exists before processing.
                            if (mainChatRoomDoc.exists()) {
                                // Correctly map the main document to the ChatRoom object
                                val fullChatRoom = mainChatRoomDoc.toObject(ChatRoom::class.java)

                                // Step 3: Map the user-specific data from the user's sub-collection
                                val userSpecificData = userRoomDoc.toObject(UserChatRoomData::class.java)
                                val unreadCount = getUnreadCountForRoom(roomId, userSpecificData?.lastReadTimestamp ?: 0L)

                                // Step 4: Combine the data and return a complete ChatRoom object
                                fullChatRoom?.copy(
                                    // Ensure the document ID is correctly set from the document itself
                                    documentId = mainChatRoomDoc.id,
                                    lastMessage = userSpecificData?.lastMessage,
                                    lastTimestamp = userSpecificData?.lastTimestamp ?: 0L,
                                    lastReadTimestamp = userSpecificData?.lastReadTimestamp ?: 0L,
                                    unreadCount = unreadCount,
                                    muted = userSpecificData?.muted ?: false
                                )
                            } else {
                                // If the main document doesn't exist, return null to be filtered out
                                Log.w(TAG, "Main chatroom document with ID '$roomId' not found. This may indicate a data inconsistency. Skipping.")
                                null
                            }
                        }
                    }

                    // Await all deferred results and filter out any nulls
                    val finalChatRooms = runBlocking {
                        deferredChatRooms.awaitAll().filterNotNull()
                    }
                    trySend(finalChatRooms)
                }
            }
            awaitClose { subscription.remove() }
        }.flowOn(Dispatchers.IO)
    }

    // You will need a new data class to map the user-specific data
    @Keep
    data class UserChatRoomData(
        val lastMessage: String? = null,
        val lastTimestamp: Long = 0L,
        val lastReadTimestamp: Long = 0L,
        val isDeleted: Boolean = false, // <-- Add this
        val muted: Boolean = false,
        val title: String = "", // <-- Add this
        val type: String = ""   // <-- Add this
    )

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
            // ✨ The fix: Create a deterministic ID from the participant IDs
            val sortedParticipantIds = participantIds.sorted()
            val dmChatRoomId = sortedParticipantIds.joinToString(separator = "_")

            val newRoomRef = db.collection(CHATROOMS_COLLECTION).document(dmChatRoomId)
            val documentSnapshot = newRoomRef.get().await()

            if (documentSnapshot.exists()) {
                // Room already exists, return its ID
                Log.d(TAG, "DM room already exists with ID: $dmChatRoomId")
                return Result.success(dmChatRoomId)
            }

            // The data for the main chatroom document.
            val mainChatRoomData = mapOf(
                "type" to "dm",
                "title" to roomName,
                "createdAt" to System.currentTimeMillis(),
                "participants" to participantIds
            )
            // Add this data to the main collection using the generated ID
            newRoomRef.set(mainChatRoomData).await()

            // Create a minimal document for each participant's room list.
            participantIds.forEach { userId ->
                val userRoomData = mapOf(
                    "lastMessage" to "Room created.",
                    "lastTimestamp" to System.currentTimeMillis(),
                    "lastReadTimestamp" to System.currentTimeMillis()
                )
                db.collection(USER_CHATROOMS_COLLECTION)
                    .document(userId)
                    .collection(CHATROOMS_SUB_COLLECTION)
                    .document(dmChatRoomId)
                    .set(userRoomData)
                    .await()
            }

            Log.d(TAG, "Successfully created new DM room with ID: $dmChatRoomId")
            Result.success(dmChatRoomId)

        } catch (e: Exception) {
            Log.e(TAG, "Error creating new direct chat room", e)
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
//                        "roomId" to newRoomId,
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
        return try {
            // Check if the user trying to add is an admin (a basic check)
            val adderRole = getMemberRole(roomId, addedBy)
            if (adderRole != "admin") {
                return Result.failure(Exception("Only admins can add members."))
            }

            db.runTransaction { transaction ->
                val memberRef = db.collection(CHATROOMS_COLLECTION)
                    .document(roomId)
                    .collection("members")
                    .document(userId)

                val userRoomRef = db.collection(USER_CHATROOMS_COLLECTION)
                    .document(userId)
                    .collection(CHATROOMS_SUB_COLLECTION)
                    .document(roomId)

                // Add the new member to the group's members sub-collection
                val newMemberData = Member(
                    userId = userId,
                    role = "member",
                    joinedAt = System.currentTimeMillis()
                )
                transaction.set(memberRef, newMemberData)

                // Add the chatroom to the new user's chat list
                val chatRoomData = mapOf(
                    "title" to "", // Will be filled in by the listenToChatRooms logic
                    "lastMessage" to "You were added to the group.",
                    "lastTimestamp" to System.currentTimeMillis(),
                    "lastReadTimestamp" to 0,
                    "isDeleted" to false,
                    "muted" to false,
                    "type" to "group"
                )
                transaction.set(userRoomRef, chatRoomData)
                null // Indicate success
            }.await()
            Log.d(TAG, "User $userId successfully added to room $roomId by $addedBy")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding member to group", e)
            Result.failure(e)
        }
    }

    override suspend fun removeMemberFromGroup(
        roomId: String,
        userId: String
    ): Result<Unit> {
        return try {
            // Check if the member being removed is an admin
            val removedRole = getMemberRole(roomId, userId)
            if (removedRole == "admin") {
                // A more robust check should verify if they are the only admin
                return Result.failure(Exception("Cannot remove an admin. Transfer ownership first."))
            }

            db.runTransaction { transaction ->
                // Remove the member from the group's members sub-collection
                val memberRef = db.collection(CHATROOMS_COLLECTION)
                    .document(roomId)
                    .collection("members")
                    .document(userId)
                transaction.delete(memberRef)

                // Soft-delete the room from the user's chat list
                val userRoomRef = db.collection(USER_CHATROOMS_COLLECTION)
                    .document(userId)
                    .collection(CHATROOMS_SUB_COLLECTION)
                    .document(roomId)
                transaction.update(userRoomRef, "isDeleted", true)

                // You might also want to log a system message in the chat
                // db.collection(MESSAGES_COLLECTION).add(...)
                null
            }.await()
            Log.d(TAG, "User $userId successfully removed from room $roomId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing member from group", e)
            Result.failure(e)
        }
    }

    override suspend fun leaveGroup(roomId: String, userId: String): Result<Unit> {
        return try {
            // Check the user's role
            val userRole = getMemberRole(roomId, userId)
            if (userRole == "admin") {
                return Result.failure(Exception("Admins must transfer ownership before leaving."))
            }
            db.runTransaction { transaction ->
                // Remove the member from the group's members sub-collection
                val memberRef = db.collection(CHATROOMS_COLLECTION)
                    .document(roomId)
                    .collection("members")
                    .document(userId)
                transaction.delete(memberRef)

                // Soft-delete the room from the user's chat list
                val userRoomRef = db.collection(USER_CHATROOMS_COLLECTION)
                    .document(userId)
                    .collection(CHATROOMS_SUB_COLLECTION)
                    .document(roomId)
                transaction.update(userRoomRef, "isDeleted", true)
                null
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error leaving group", e)
            Result.failure(e)
        }
    }

    override suspend fun transferOwnership(
        roomId: String,
        currentAdminId: String,
        newAdminId: String
    ): Result<Unit> {
        return try {
            // Check if the current user is an admin
            val role = getMemberRole(roomId, currentAdminId)
            if (role != "admin") {
                return Result.failure(Exception("You must be an admin to transfer ownership."))
            }

            db.runTransaction { transaction ->
                val currentAdminRef = db.collection(CHATROOMS_COLLECTION)
                    .document(roomId)
                    .collection("members")
                    .document(currentAdminId)

                val newAdminRef = db.collection(CHATROOMS_COLLECTION)
                    .document(roomId)
                    .collection("members")
                    .document(newAdminId)

                // Update the current admin's role to 'member'
                transaction.update(currentAdminRef, "role", "member")

                // Update the new admin's role to 'admin'
                transaction.update(newAdminRef, "role", "admin")

                // Optionally, add a system message to the chat
                // ...
                null // Indicate success
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error transferring ownership", e)
            Result.failure(e)
        }
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

    override suspend fun getMemberRole(roomId: String, userId: String): String? {
        return try {
            val memberDoc = db.collection(CHATROOMS_COLLECTION)
                .document(roomId)
                .collection("members")
                .document(userId)
                .get()
                .await()
            memberDoc.getString("role")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting member role for user $userId in room $roomId", e)
            null
        }
    }

    override suspend fun getMembers(roomId: String): List<Member> {
        return try {
            val snapshot = db.collection(CHATROOMS_COLLECTION)
                .document(roomId)
                .collection("members")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Member::class.java)?.copy(userId = doc.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting members for room $roomId", e)
            emptyList()
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