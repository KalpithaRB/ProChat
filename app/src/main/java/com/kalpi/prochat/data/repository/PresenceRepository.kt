package com.kalpi.prochat.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log

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
}