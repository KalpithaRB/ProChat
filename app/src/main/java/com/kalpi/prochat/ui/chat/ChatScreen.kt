package com.kalpi.prochat.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo // Or filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Error // For FAILED state
import androidx.compose.material.icons.filled.Refresh // For FAILED state retry
import androidx.compose.material.icons.outlined.CheckCircle // For SENT state (optional)
import androidx.compose.material.icons.outlined.Schedule // For SENDING state
import androidx.compose.material3.CircularProgressIndicator // Can also use for SENDING
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.layout.ContentScale // <<< ADD for image scaling
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage // <<< ADD COIL (or Glide)
import coil.request.ImageRequest
import com.kalpi.prochat.R

import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel // For viewModel()
import com.kalpi.prochat.data.ChatMessage
import com.kalpi.prochat.data.MessageType
import com.kalpi.prochat.data.MessageStatus
import com.kalpi.prochat.ui.chat.ChatViewModel
import com.kalpi.prochat.ui.theme.ProChatTheme // Assuming your theme is named ProChatTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import android.net.Uri // Important: use android.net.Uri
import androidx.compose.ui.semantics.error
//import androidx.privacysandbox.tools.core.generator.build
//import androidx.wear.compose.material.placeholder
import java.util.*
import com.kalpi.prochat.data.ChatItem


/**
 * The main screen for displaying a chat conversation.
 * It observes [ChatUiState] from the [ChatViewModel] and displays messages,
 * an input field, and a send button.
 *
 * @param chatViewModel The ViewModel providing chat data and state.
 */

@OptIn(ExperimentalMaterial3Api::class) // For Scaffold, TopAppBar, TextField
@Composable
fun ChatScreen(
    // We can allow providing a specific recipient name if needed later for the TopAppBar
    // recipientName: String = "Chat User",
    modifier: Modifier = Modifier,
    chatViewModel: ChatViewModel // Uses the default ViewModel factory
) {
    val uiState by chatViewModel.uiState.collectAsState()
    val listState = rememberLazyListState() // For auto-scrolling
    var textState by remember { mutableStateOf(TextFieldValue("")) }

    // Coroutine scope for launching animations or other suspend functions if needed
    // val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to the bottom when new messages arrive and the user is near the bottom
    LaunchedEffect(uiState) {
        if (uiState is ChatUiState.Content) {
            val messages = (uiState as ChatUiState.Content).messages
            if (messages.isNotEmpty()) {
                // Heuristic: if last visible item is close to the end, then scroll.
                // You might want a more sophisticated check if the user has scrolled up significantly.
                if (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == messages.size - 2 ||
                    listState.layoutInfo.visibleItemsInfo.isEmpty() // Initially scroll
                ) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Pro" /* TODO: Use recipientName later */) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
                // TODO: Add back button or other actions if needed
            )
        },
        bottomBar = {
            ChatInput(
                chatViewModel = chatViewModel
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is ChatUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ChatUiState.Error -> {
                    Text(
                        text = "Error: ${state.errorMessage}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                is ChatUiState.Content -> {
                    if (state.messages.isEmpty()) {
                        Text(
                            text = "No messages yet. Start chatting!",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            //verticalArrangement = Arrangement.spacedBy(8.dp) // Spacing between messages
                        ) {
                            items(
                                items = state.messages, // Use state.items
                                key = { chatItem -> // Unique key for each item
                                    when (chatItem) {
                                        is ChatItem.Message -> chatItem.message.id
                                        is ChatItem.DateSeparator -> chatItem.timestamp.toString() // Use timestamp for key
                                    }
                                },
                                contentType = { chatItem -> // For performance with different item types
                                    when (chatItem) {
                                        is ChatItem.Message -> "message"
                                        is ChatItem.DateSeparator -> "separator"
                                    }
                                }
                            ){ chatItem ->
                                when (chatItem) {
                                    is ChatItem.Message -> {
                                        MessageBubble(
                                            message = chatItem.message,
                                            currentUserId = ChatViewModel.CURRENT_USER_ID, // Or however you get current user ID
                                            onRetryClick = { messageToRetry -> // Add this lambda
                                                chatViewModel.retrySendMessage(messageToRetry)
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp)) // Add spacing after each message
                                    }
                                    is ChatItem.DateSeparator -> {
                                        DateSeparatorItem(text = chatItem.displayDate)
                                        Spacer(modifier = Modifier.height(8.dp)) // Add spacing after each separator
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun DateSeparatorItem(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp), // More padding for separators
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall, // Or bodySmall
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f), // Subtle background
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}



/**
 * Composable for displaying a single chat message bubble.
 * It adjusts alignment and color based on whether the message is from the current user.
 *
 * @param message The [ChatMessage] to display.
 * @param currentUserId The ID of the current user, to determine message alignment.
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    currentUserId: String,
    onRetryClick: (ChatMessage) -> Unit // Lambda to handle retry
) {
    val isCurrentUser = message.senderId == currentUserId
    val bubbleContainerAlignment  = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer
    //val horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start

    // For timestamp formatting
    val simpleDateFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.clientTimestamp) {
        simpleDateFormat.format(Date(message.clientTimestamp))
    }
    // Adjust opacity for SENDING state
    //val bubbleAlpha = if (message.status == MessageStatus.SENDING) 0.7f else 1.0f
    val bubbleAlpha = if (message.status == MessageStatus.SENDING && message.messageType == MessageType.IMAGE) 0.6f // More faded for pending image
    else if (message.status == MessageStatus.SENDING) 0.7f
    else 1.0f



    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isCurrentUser) 64.dp else 0.dp, // Indent opposite side
                end = if (isCurrentUser) 0.dp else 64.dp,   // Indent opposite side
                top = 2.dp,
                bottom = 2.dp
            )
            .alpha(bubbleAlpha),
        contentAlignment = bubbleContainerAlignment // This aligns the content (bubble) within the Column
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            // For "other user" messages, status icon could be on the left (optional)
             if (!isCurrentUser && message.status == MessageStatus.FAILED) {
                 Icon(
                     imageVector = Icons.Filled.Error,
                     contentDescription = "Message failed",
                     tint = MaterialTheme.colorScheme.error,
                     modifier = Modifier
                         .size(16.dp)
                         .padding(end = 4.dp)
                 )
             }
            Column {
            if (message.messageType == MessageType.SYSTEM) {
                // System Message Styling
                Text(
                    text = message.text ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            } else {
                // User Message Bubble
                Box(
                    modifier = Modifier
                        // .wrapContentWidth() // Let the bubble size to its content
                        .clip(
                            RoundedCornerShape( // Dynamic corner rounding
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
                            )
                        )
                        .background(bubbleColor)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column ( // Content inside the bubble
                        modifier = Modifier.padding( // General padding for text, adjusted if only image
                            horizontal = if (message.imageUrl != null && message.text == null) 0.dp else 12.dp,
                            vertical = if (message.imageUrl != null && message.text == null) 0.dp else 8.dp
                        )
                    )
                    { // To stack text and timestamp vertically
                        // Display Image if imageUrl is present
                        message.imageUrl?.let { imageUrl ->
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageUrl) // Handles local URIs and remote URLs
                                    .crossfade(true)
                                    // You can add a placeholder and error drawable
                                    .placeholder(R.drawable.ic_placeholder_image) // Create this drawable
                                    .error(R.drawable.ic_error_image)       // Create this drawable
                                    .build(),
                                contentDescription = "Chat image",
                                contentScale = ContentScale.Fit, // Or ContentScale.Crop
                                modifier = Modifier
                                    .fillMaxWidth(0.6f) // Max width for image
                                    .aspectRatio(1f) // Square aspect ratio, adjust as needed
                                    .clip(
                                        RoundedCornerShape( // Clip image inside bubble if it's the only content
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                                            bottomEnd = if (isCurrentUser) 4.dp else 16.dp
                                        )
                                    )
                            )
                        // If there's also text with the image, add some space
                        if (message.text != null) {
                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    // Display Text if text is present
                    message.text?.let { textContent ->
                        if (textContent.isNotBlank()) { // Only display if text is not blank
                            Text(
                                text = textContent,
                                color = textColor,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    // Timestamp and Status Row (always present if not a system message)
                    // If only image and no text, padding might need adjustment here or outside
                    if (message.text != null || message.imageUrl != null) { // Ensure there's some content
                        Spacer(Modifier.height(4.dp)) // Spacer before timestamp/status
                    }
                        Row(
                            modifier = Modifier.align(Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formattedTime,
                                color = textColor.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                            // Status Icon for current user messages, shown to the right of timestamp
                            if (isCurrentUser) {
                                Spacer(Modifier.width(4.dp))
                                MessageStatusIcon(
                                    status = message.status,
                                    onRetry = { onRetryClick(message) },
                                    isCurrentUser = true
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}


@Composable
fun MessageStatusIcon(
    status: MessageStatus,
    onRetry: () -> Unit,
    isCurrentUser: Boolean // To potentially adjust icon color or style
) {
    val iconSize = 16.dp
    when (status) {
        MessageStatus.SENDING -> {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = "Sending message",
                modifier = Modifier.size(iconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            // Or use a small CircularProgressIndicator:
            // CircularProgressIndicator(modifier = Modifier.size(iconSize), strokeWidth = 1.5.dp)
        }
        MessageStatus.SENT -> {
            // Optionally show a "sent" tick, or nothing for a cleaner look
            Icon(
                imageVector = Icons.Outlined.CheckCircle, // Or a single tick
                contentDescription = "Message sent",
                modifier = Modifier.size(iconSize),
                tint = MaterialTheme.colorScheme.secondary // Or a more subtle color
            )
        }
        MessageStatus.FAILED -> {
            Icon(
                imageVector = Icons.Filled.Error, // Or Icons.Filled.Refresh to imply retry directly
                contentDescription = "Message failed, tap to retry",
                modifier = Modifier
                    .size(iconSize)
                    .clickable(onClick = onRetry),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}


// A simplified ViewModel for preview purposes, or use a fake/mocking library for more complex scenarios
//class PreviewChatViewModel(initialState: ChatUiState = ChatUiState.Loading) :
//    ViewModel(),{
//    private val _uiState = MutableStateFlow(initialState)
//   val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow() // Mark as override
//}

// region Preview
//@Preview(showBackground = true, name = "Chat Screen Light")
//@Composable
//fun ChatScreenPreview() {
//    ProChatTheme(darkTheme = false) {
//        val previewViewModel = remember { // remember it to avoid re-creation on recomposition
//            PreviewChatViewModel(
//                ChatUiState.Content(
//                    listOf(
//                        ChatMessage("1", ChatViewModel.OTHER_USER_ID, "Hello Preview!", System.currentTimeMillis(), MessageType.USER),
//                        ChatMessage("2", ChatViewModel.CURRENT_USER_ID, "Hi there from Preview!", System.currentTimeMillis() - 5000, MessageType.USER)
//                    )
//                )
//            )
//        }
//        ChatScreen(chatViewModel = previewViewModel)
//    }
//}
//
//@Preview(showBackground = true, name = "Chat Screen Dark")
//@Composable
//fun ChatScreenDarkPreview() {
//    ProChatTheme(darkTheme = true) {
//        val previewViewModel = remember {
//            PreviewChatViewModel(
//                ChatUiState.Content(
//                    listOf(
//                        ChatMessage("1", ChatViewModel.OTHER_USER_ID, "Dark mode preview!", System.currentTimeMillis(), MessageType.USER)
//                    )
//                )
//            )
//        }
//        ChatScreen(chatViewModel = previewViewModel)
//    }
//}

@Preview(showBackground = true, name = "Message Bubble Sent")
@Composable
fun MessageBubbleSentPreview() {
    ProChatTheme {
        MessageBubble(
            message = ChatMessage(
                id = "1",
                senderId = ChatViewModel.CURRENT_USER_ID,
                text = "This is a sent message example. It can be quite long to test wrapping.",
                // clientTimestamp = System.currentTimeMillis()
            ),
            currentUserId = ChatViewModel.CURRENT_USER_ID,
            onRetryClick = { /* Preview: No action needed for retry */ } // Add this
        )
    }
}


@Preview(showBackground = true, name = "Message Bubble Received")
@Composable
fun MessageBubbleReceivedPreview() {
    ProChatTheme {
        MessageBubble(
            message = ChatMessage(
                id = "2",
                senderId = ChatViewModel.OTHER_USER_ID, // Make sure OTHER_USER_ID is defined or use a string
                text = "This is a received message! Shorter this time.",
                // clientTimestamp = System.currentTimeMillis() // Assuming your ChatMessage constructor uses this
                // If your constructor still has 'timestamp', adjust accordingly or update ChatMessage
            ),
            currentUserId = ChatViewModel.CURRENT_USER_ID,
            onRetryClick = { /* Preview: No action needed for retry */ } // Add this
        )
    }
}


@Preview(showBackground = true, name = "System Message")
@Composable
fun SystemMessagePreview() {
    ProChatTheme {
        MessageBubble(
            message = ChatMessage(
                id = "system1",
                senderId = "system", // Or your system sender ID
                text = "User Has Joined The Chat",
                // clientTimestamp = System.currentTimeMillis(),
                messageType = MessageType.SYSTEM
            ),
            currentUserId = ChatViewModel.CURRENT_USER_ID, // Or a dummy ID for system messages
            onRetryClick = { /* Preview: No action needed for retry, system messages might not fail/retry */ } // Add this
        )
    }
}
