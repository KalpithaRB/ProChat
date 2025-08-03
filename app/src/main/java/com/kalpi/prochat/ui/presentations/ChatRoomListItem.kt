package com.kalpi.prochat.ui.presentations


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.kalpi.prochat.data.model.ChatRoom
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatRoomListItem(
    chatRoom: ChatRoom,
    onRoomClicked: (String, String) -> Unit,
    onToggleMuteClicked: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRoomClicked(chatRoom.roomId, chatRoom.name) },
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Placeholder
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            // Room details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Room name
                Text(
                    text = if (chatRoom.name.isNotBlank()) chatRoom.name else "Unnamed Room",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Last message preview
                chatRoom.lastMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            //Mute/UnMute icon
            IconButton(onClick = { onToggleMuteClicked(chatRoom.roomId) }) {
                Icon(
                    // CORRECTED: Check 'chatRoom.muted' instead of 'chatRoom.isMuted'
                    imageVector = if (chatRoom.muted) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                    contentDescription = if (chatRoom.muted) "Unmute Chat" else "Mute Chat",
                    tint = if (chatRoom.muted) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Timestamp and unread count
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Last message timestamp
                Text(
                    text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(chatRoom.lastTimestamp)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Unread count badge
                if (chatRoom.unreadCount > 0) {
                    BadgedBox(badge = {
                        Badge { Text(text = chatRoom.unreadCount.toString()) }
                    }) {
                        // Badge content
                    }
                }
            }
        }
    }
}