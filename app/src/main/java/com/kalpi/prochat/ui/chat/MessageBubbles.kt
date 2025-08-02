package com.kalpi.prochat.ui.chat


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.ui.graphics.Color

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kalpi.prochat.data.model.ChatMessage
import com.kalpi.prochat.data.model.MessageStatus
import com.kalpi.prochat.data.model.MessageType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.kalpi.prochat.utils.formatTime

/**
 * A composable for displaying a text-only chat message.
 */
@Composable
fun TextMessage(
    message: ChatMessage,
    isCurrentUser: Boolean,
    textColor: androidx.compose.ui.graphics.Color
) {
    Text(
        text = message.text ?: "",
        color = textColor,
        style = MaterialTheme.typography.bodyLarge
    )
}

/**
 * A composable for displaying an image chat message.
 */
@Composable
fun ImageMessage(
    message: ChatMessage,
    isCurrentUser: Boolean,
    uploadProgress: Int?,
    onRetryClick: (ChatMessage) -> Unit
) {
    val imageUrl = message.imageUrl ?: return
    val imageModifierBase = Modifier
        .fillMaxWidth()
        .heightIn(min = 100.dp, max = 220.dp)
        .clip(RoundedCornerShape(12.dp))

    // Check for local URI and show progress
    if (message.status == MessageStatus.SENDING && uploadProgress != null) {
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Uploading image",
                contentScale = ContentScale.Crop,
                modifier = imageModifierBase.alpha(0.4f)
            )
            CircularProgressIndicator(
                progress = { uploadProgress / 100f },
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                strokeWidth = 3.dp
            )
            Text(
                text = "$uploadProgress%",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Chat image",
            contentScale = ContentScale.Crop,
            modifier = imageModifierBase
        )
    }

    if (message.status == MessageStatus.FAILED) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = "Message failed",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Failed to send",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { onRetryClick(message) }, modifier = Modifier.size(20.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Refresh, // Placeholder for a refresh icon
                        contentDescription = "Retry",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}


/**
 * A composable for displaying the status icon of a message.
 */

@Composable
fun MessageStatusIcon(
    status: MessageStatus,
    onRetry: () -> Unit,
    isCurrentUser: Boolean
) {
    val iconSize = 16.dp
    val iconTint = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
    else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)

    when (status) {
        MessageStatus.SENDING -> {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = "Sending message",
                modifier = Modifier.size(iconSize),
                tint = iconTint
            )
        }
        MessageStatus.SENT -> {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = "Message sent",
                modifier = Modifier.size(iconSize),
                tint = iconTint
            )
        }
        MessageStatus.DELIVERED -> {
            Icon(
                imageVector = Icons.Outlined.DoneAll,
                contentDescription = "Message delivered",
                modifier = Modifier.size(iconSize),
                tint = iconTint
            )
        }
        MessageStatus.READ -> {
            Icon(
                imageVector = Icons.Outlined.DoneAll,
                contentDescription = "Message read",
                modifier = Modifier.size(iconSize),
                // Use a different color for the "read" status
                tint = Color(0xFF34B7F1)
            )
        }
        MessageStatus.FAILED -> {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = "Message failed, tap to retry",
                modifier = Modifier
                    .size(iconSize)
                    .clickable(onClick = onRetry),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}