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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.ui.graphics.Color
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
//import androidx.media3.ui.PlayerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kalpi.prochat.R
import com.kalpi.prochat.data.model.ChatMessage
import com.kalpi.prochat.data.model.MessageStatus
import com.kalpi.prochat.data.model.MessageType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.kalpi.prochat.utils.formatTime
import kotlin.math.ln
import kotlin.math.pow

/**
 * A composable for displaying a text-only chat message.
 */
@Composable
fun TextMessage(
    text: String,
    color: androidx.compose.ui.graphics.Color
) {
    Text(
        text = text,
        color = color,
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
 * A composable for displaying a non-image file chat message.
 */
@Composable
fun FileMessage(
    message: ChatMessage,
    onRetryClick: (ChatMessage) -> Unit
) {
    val fileName = message.fileName ?: "File"
    val fileSize = message.fileSize ?: -1L
    val isSending = message.status == MessageStatus.SENDING
    val isFailed = message.status == MessageStatus.FAILED

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(8.dp)
    ) {
        // Display a generic file icon
        Icon(
            imageVector = Icons.Filled.Description,
            contentDescription = "File icon",
            tint = if (isSending) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // File Name
            Text(
                text = fileName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // File Size (formatted for readability)
            if (fileSize != -1L) {
                Text(
                    text = formatFileSize(fileSize),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Display progress or error status
        when {
            isSending -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                    // You'll need to pass the uploadProgress state here.
                    // For now, we'll show a simple loading indicator.
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            isFailed -> {
                IconButton(
                    onClick = { onRetryClick(message) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Retry upload",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}


/**
 * Helper function to format file size from bytes to a readable format (KB, MB).
 */
fun formatFileSize(sizeInBytes: Long): String {
    if (sizeInBytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (ln(sizeInBytes.toDouble()) / ln(1024.0)).toInt()
    return String.format("%.1f %s", sizeInBytes / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}

@Composable
fun AudioMessage(message: ChatMessage) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            message.fileUrl?.let { url ->
                val mediaItem = MediaItem.fromUri(url)
                setMediaItem(mediaItem)
            }
        }
    }

    DisposableEffect(Unit) {
        exoPlayer.prepare()
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .background(Color.LightGray, shape = RoundedCornerShape(16.dp))
            .padding(16.dp)
            .width(200.dp) // Set a reasonable size for the audio message bubble
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                } else {
                    exoPlayer.play()
                }
            }) {
                Icon(
                    painter = painterResource(
                        id = if (exoPlayer.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                    ),
                    contentDescription = "Play/Pause Audio"
                )
            }
            // Optional: You can add a seek bar or a timestamp here
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
            // Single checkmark for "Sent"
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Message sent",
                modifier = Modifier.size(iconSize),
                tint = iconTint
            )
        }
        MessageStatus.DELIVERED -> {
            // Double checkmarks for "Delivered"
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Message delivered",
                modifier = Modifier.size(iconSize),
                tint = iconTint
            )
        }
        MessageStatus.READ -> {
            // Double checkmarks with a distinct color for "Read"
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Message read",
                modifier = Modifier.size(iconSize),
                tint = Color(0xFF34B7F1) // A blue color to indicate "read"
            )
        }
        MessageStatus.FAILED -> {
            IconButton(
                onClick = onRetry,
                modifier = Modifier.size(iconSize)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Message failed, tap to retry",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}