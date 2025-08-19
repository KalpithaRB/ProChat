package com.kalpi.prochat.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.kalpi.prochat.data.model.UiChatRoom
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@Composable
fun ChatRoomAvatar(chatRoom: UiChatRoom) {
    val initials = if (chatRoom.title.isNotBlank()) {
        val words = chatRoom.title.split(" ")
        when (words.size) {
            0 -> ""
            1 -> words[0].firstOrNull()?.toString()?.uppercase() ?: ""
            else -> {
                val firstInitial = words[0].firstOrNull()?.toString()?.uppercase() ?: ""
                val secondInitial = words[1].firstOrNull()?.toString()?.uppercase() ?: ""
                firstInitial + secondInitial
            }
        }
    } else {
        "?"
    }

    // A simple, deterministic color based on the room's title
    val color = remember {
        val colorIndex = chatRoom.title.sumOf { it.code } % 5
        when (colorIndex) {
            0 -> Color(0xFFD32F2F)
            1 -> Color(0xFFC2185B)
            2 -> Color(0xFF00796B)
            3 -> Color(0xFF1976D2)
            else -> Color(0xFF512DA8)
        }
    }

    // Check if an avatar URL exists
    if (chatRoom.avatarUrl.isNullOrBlank()) {
        // Display a circle with initials
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }
    } else {
        // Display the image from the URL using Coil
        Image(
            painter = rememberAsyncImagePainter(model = chatRoom.avatarUrl),
            contentDescription = "Chat Room Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface) // Placeholder background
        )
    }
}