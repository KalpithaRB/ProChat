package com.kalpi.prochat.data.repository

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.kalpi.prochat.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

// UserRepository.kt
interface UserRepository {
    fun searchUsers(query: String): Flow<List<User>>
    suspend fun getUserById(userId: String): User?
    suspend fun getUsers(): List<User>
    fun listenToUsers(userIds: List<String>): Flow<Map<String, User>>
}

class RealUserRepository(private val firestore: FirebaseFirestore) : UserRepository {
    override fun searchUsers(query: String): Flow<List<User>> {
        return flow {
            // Placeholder: A real implementation would query by name or user ID
            val users = firestore.collection("users").get().await().toObjects(User::class.java)
            emit(users.filter { it.name.contains(query, ignoreCase = true) || it.userId.contains(query, ignoreCase = true) })
        }
    }

    override suspend fun getUserById(userId: String): User? {
        return try {
            firestore.collection("users").document(userId).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getUsers(): List<User> {
        return try {
            firestore.collection("users")
                .get()
                .await()
                .toObjects(User::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Listens for real-time updates for a specific list of user IDs.
     * @param userIds The list of user IDs to listen to.
     * @return A Flow of a Map where the key is the user ID and the value is the User object.
     */

    override fun listenToUsers(userIds: List<String>): Flow<Map<String, User>> = callbackFlow {
        if (userIds.isEmpty()) {
            trySend(emptyMap())
            awaitClose()
            return@callbackFlow
        }

        // Firestore's `whereIn` clause has a limit of 10.
        // For simplicity, we assume the list of IDs is small.
        val query = firestore.collection("users").whereIn(FieldPath.documentId(), userIds)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val usersMap = snapshot.documents.associate { doc ->
                    doc.id to doc.toObject(User::class.java)
                }.filterValues { it != null } as Map<String, User>
                trySend(usersMap)
            }
        }

        awaitClose {
            subscription.remove()
        }
    }
}