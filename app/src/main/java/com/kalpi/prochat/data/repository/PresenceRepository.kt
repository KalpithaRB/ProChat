package com.kalpi.prochat.data.repository

import com.google.firebase.firestore.FieldPath
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class PresenceRepository(private val db: FirebaseFirestore) {

    companion object {
        private const val TAG = "PresenceRepository"
    }

    /**
     * Updates the user's 'lastActiveAt' timestamp in Firestore.
     * This acts as a "heartbeat" to signal the user is online.
     */
    suspend fun updateLastActive(userId: String) {
        try {
            val userPresenceRef = db.collection("presence").document(userId)
            val data = mapOf("lastActiveAt" to FieldValue.serverTimestamp())

            userPresenceRef.set(data).await()
            Log.d(TAG, "Presence updated for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating presence for user: $userId", e)
        }
    }

    /**
     * Gets a user's last active timestamp.
     */
    suspend fun getLastActive(userId: String): Long? {
        return try {
            val userPresenceDoc = db.collection("presence").document(userId).get().await()
            val timestamp = userPresenceDoc.getTimestamp("lastActiveAt")?.toDate()?.time
            timestamp
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last active for user: $userId", e)
            null
        }
    }

    // NEW: Function to listen to presence for multiple users
    fun listenToPresence(userIds: List<String>): Flow<Map<String, Long>> = callbackFlow {
        if (userIds.isEmpty()) {
            trySend(emptyMap())
            awaitClose() // No users to listen to, close the flow immediately
            return@callbackFlow
        }

        // Firestore `whereIn` clause is limited to 10 items.
        // For a simple app, this might be fine. For a larger app, you'd need to break this down.
        // We will assume for now that the number of chat participants is small.
        val query = db.collection("presence").whereIn(FieldPath.documentId(), userIds)

        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Map each document to a key-value pair of userId to lastActiveAt
                val presenceMap = snapshot.documents.associate { doc ->
                    doc.id to doc.getTimestamp("lastActiveAt")?.toDate()?.time
                }.filterValues { it != null } as Map<String, Long>

                trySend(presenceMap)
            }
        }

        awaitClose {
            subscription.remove()
        }
    }
}