package com.kalpi.prochat.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.kalpi.prochat.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

// UserRepository.kt
interface UserRepository {
    fun searchUsers(query: String): Flow<List<User>>
}

class RealUserRepository(private val firestore: FirebaseFirestore) : UserRepository {
    override fun searchUsers(query: String): Flow<List<User>> {
        return flow {
            // Placeholder: A real implementation would query by name or user ID
            val users = firestore.collection("users").get().await().toObjects(User::class.java)
            emit(users.filter { it.name.contains(query, ignoreCase = true) || it.userId.contains(query, ignoreCase = true) })
        }
    }
}