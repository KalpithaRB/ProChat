package com.kalpi.prochat.ui.chat

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.kalpi.prochat.data.model.User
import java.util.UUID

object UserManager {
    private const val PREFS_NAME = "chat_prefs"
    private const val USER_ID_KEY = "user_id"

    fun getOrCreateUserId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var userId = prefs.getString(USER_ID_KEY, null)

        if (userId == null) {
            userId = UUID.randomUUID().toString()
            prefs.edit().putString(USER_ID_KEY, userId).apply()

            // NEW: Create a user profile in Firestore
            val user = User(userId = userId, name = "User ${userId.take(4)}") // Simple default name
            FirebaseFirestore.getInstance().collection("users").document(userId).set(user)
        }
        return userId
    }
}