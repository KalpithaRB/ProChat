package com.kalpi.prochat.ui.presentations.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator // Can also use for SENDING
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Spacer

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.kalpi.prochat.data.model.ChatMessage
import com.kalpi.prochat.data.model.MessageType
import com.kalpi.prochat.data.model.MessageStatus
import java.text.SimpleDateFormat

//import androidx.privacysandbox.tools.core.generator.build
//import androidx.wear.compose.material.placeholder
import com.kalpi.prochat.data.ChatItem
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.sp
import com.kalpi.prochat.ui.chat.ChatInput
import com.kalpi.prochat.ui.chat.ChatUiState
import java.util.Locale
import com.kalpi.prochat.ui.presentations.viewmodel.ChatViewModel
import com.kalpi.prochat.ui.chat.MessageStatusIcon


/**
 * The main screen for displaying a chat conversation.
 * It observes [com.kalpi.prochat.ui.chat.ChatUiState] from the [ChatViewModel] and displays messages,
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
    chatViewModel: ChatViewModel, // Uses the default ViewModel factory
    onBackClicked: () -> Unit
) {
    val uiState by chatViewModel.uiState.collectAsState()
    val listState = rememberLazyListState() // For auto-scrolling
    var textState by remember { mutableStateOf(TextFieldValue("")) }

    val uploadProgress by chatViewModel.uploadProgress.collectAsState()
    // Coroutine scope for launching animations or other suspend functions if needed
    // val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to the bottom when new messages arrive and the user is near the bottom
    LaunchedEffect(uiState) {
        if (uiState is ChatUiState.Content) {
            val messages = (uiState as ChatUiState.Content).messages
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
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
                onSendMessage = chatViewModel::sendMessage,
                onSendImageMessage = chatViewModel::prepareAndSendImageMessage
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
                                        is ChatItem.Message -> {
                                            // Use the message ID if it exists, otherwise create a temporary one
                                            if (chatItem.message.id.isNotEmpty()) {
                                                chatItem.message.id
                                            } else {
                                                // This ensures unsent messages (with no ID) still have a unique key.
                                                "${chatItem.message.senderId}-${chatItem.message.clientTimestamp}"
                                            }
                                        }
                                        is ChatItem.DateSeparator -> {
                                            // Use the timestamp for a more reliable key for separators.
                                            // I've also added a prefix to prevent key clashes with messages.
                                            "separator-${chatItem.timestamp}"
                                        }
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
                                            currentUserId = chatViewModel.currentUserId,
                                            uploadProgress = uploadProgress[chatItem.message.id],
                                            onRetryClick = { messageToRetry ->
                                                chatViewModel.retrySendMessage(messageToRetry)
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    is ChatItem.DateSeparator -> {
                                        DateSeparatorItem(text = chatItem.displayDate)
                                        Spacer(modifier = Modifier.height(8.dp))
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
            .padding(vertical = 8.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
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
    uploadProgress: Int?,
    onRetryClick: (ChatMessage) -> Unit
) {
    // This is the CRITICAL part for alignment. If senderId and currentUserId are the same, it's 'me'.
    val isCurrentUser = message.senderId == currentUserId

    // Main container to align the bubble left or right
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        // The bubble content itself
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
                .alpha(if (message.status == MessageStatus.SENDING) 0.7f else 1.0f)
                .padding(10.dp)
        ) {
            Column {
                // Main message content
                if (message.messageType == MessageType.SYSTEM) {
                    Text(
                        text = message.text ?: "",
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                } else {
                    message.text?.let { textContent ->
                        Text(
                            text = textContent,
                            color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 16.sp
                        )
                    }

                    // A spacer is good practice to separate content from meta-data
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Row for time and status icon, aligned to the bottom end of the bubble
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    Text(
                        text = timeFormat.format(message.clientTimestamp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )

                    // Spacer between time and icon
                    Spacer(modifier = Modifier.width(4.dp))

                    // Status Icon (only for current user)
                    if (isCurrentUser) {
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



//@Composable
//fun MessageStatusIcon(
//    status: MessageStatus,
//    onRetry: () -> Unit,
//    isCurrentUser: Boolean // To potentially adjust icon color or style
//) {
//    val iconSize = 16.dp
//    val iconTint = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
//    else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
//
//    when (status) {
//        MessageStatus.SENDING -> {
//            Icon(
//                imageVector = Icons.Outlined.Schedule,
//                contentDescription = "Sending message",
//                modifier = Modifier.size(iconSize),
//                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
//            )
//            // Or use a small CircularProgressIndicator:
//            // CircularProgressIndicator(modifier = Modifier.size(iconSize), strokeWidth = 1.5.dp)
//        }
//        MessageStatus.SENT -> {
//            // Optionally show a "sent" tick, or nothing for a cleaner look
//            Icon(
//                imageVector = Icons.Outlined.CheckCircle, // Or a single tick
//                contentDescription = "Message sent",
//                modifier = Modifier.size(iconSize),
//                tint = MaterialTheme.colorScheme.secondary // Or a more subtle color
//            )
//        }
//        MessageStatus.FAILED -> {
//            Icon(
//                imageVector = Icons.Filled.Error, // Or Icons.Filled.Refresh to imply retry directly
//                contentDescription = "Message failed, tap to retry",
//                modifier = Modifier
//                    .size(iconSize)
//                    .clickable(onClick = onRetry),
//                tint = MaterialTheme.colorScheme.error
//            )
//        }
//    }
//}


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

//@Preview(showBackground = true, name = "Message Bubble Sent")
//@Composable
//fun MessageBubbleSentPreview() {
//    ProChatTheme {
//        MessageBubble(
//            message = ChatMessage(
//                id = "1",
//                senderId = ChatViewModel.CURRENT_USER_ID,
//                text = "This is a sent message example. It can be quite long to test wrapping.",
//                // clientTimestamp = System.currentTimeMillis()
//            ),
//            currentUserId = ChatViewModel.CURRENT_USER_ID,
//            onRetryClick = { /* Preview: No action needed for retry */ } // Add this
//        )
//    }
//}


//@Preview(showBackground = true, name = "Message Bubble Received")
//@Composable
//fun MessageBubbleReceivedPreview() {
//    ProChatTheme {
//        MessageBubble(
//            message = ChatMessage(
//                id = "2",
//                senderId = ChatViewModel.OTHER_USER_ID, // Make sure OTHER_USER_ID is defined or use a string
//                text = "This is a received message! Shorter this time.",
//                // clientTimestamp = System.currentTimeMillis() // Assuming your ChatMessage constructor uses this
//                // If your constructor still has 'timestamp', adjust accordingly or update ChatMessage
//            ),
//            currentUserId = ChatViewModel.CURRENT_USER_ID,
//            onRetryClick = { /* Preview: No action needed for retry */ } // Add this
//        )
//    }
//}


//@Preview(showBackground = true, name = "System Message")
//@Composable
//fun SystemMessagePreview() {
//    ProChatTheme {
//        MessageBubble(
//            message = ChatMessage(
//                id = "system1",
//                senderId = "system", // Or your system sender ID
//                text = "User Has Joined The Chat",
//                // clientTimestamp = System.currentTimeMillis(),
//                messageType = MessageType.SYSTEM
//            ),
//            currentUserId = ChatViewModel.CURRENT_USER_ID, // Or a dummy ID for system messages
//            onRetryClick = { /* Preview: No action needed for retry, system messages might not fail/retry */ } // Add this
//        )
//    }
//}
