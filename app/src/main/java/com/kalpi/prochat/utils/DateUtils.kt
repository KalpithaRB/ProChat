package com.kalpi.prochat.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
        isSameDay(
            timestamp,
            today.apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
        ) -> "Yesterday"
        else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
/**
 * Formats a timestamp to a user-friendly time string (e.g., "10:30 AM").
 *
 * @param timestamp The timestamp in milliseconds to format.
 * @return The formatted time string.
 */
fun formatTime(timestamp: Long): String {
    val simpleDateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return simpleDateFormat.format(Date(timestamp))
}