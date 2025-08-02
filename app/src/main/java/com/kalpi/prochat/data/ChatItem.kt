package com.kalpi.prochat.data

import com.kalpi.prochat.data.model.ChatMessage
import java.util.Locale
import java.util.Calendar // For date comparison
import java.util.Date


sealed interface ChatItem {
    data class Message(val message: ChatMessage) : ChatItem
    data class DateSeparator(val displayDate: String, val timestamp: Long) : ChatItem // Store original timestamp for sorting/key
}

// Helper function to check if two timestamps are on the same day
