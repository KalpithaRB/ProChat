package com.kalpi.prochat.data

import java.util.Locale
import java.util.Calendar // For date comparison
import java.util.Date
import java.text.SimpleDateFormat
import kotlin.text.format


sealed interface ChatItem {
    data class Message(val message: ChatMessage) : ChatItem
    data class DateSeparator(val displayDate: String, val timestamp: Long) : ChatItem // Store original timestamp for sorting/key
}

// Helper function to check if two timestamps are on the same day
fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

// Helper to format date for separator
fun formatDateSeparator(timestamp: Long): String {
    val today = Calendar.getInstance()
    val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        isSameDay(timestamp, today.timeInMillis) -> "Today"
        isSameDay(timestamp, today.apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis) -> "Yesterday"
        else -> java.text.SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}