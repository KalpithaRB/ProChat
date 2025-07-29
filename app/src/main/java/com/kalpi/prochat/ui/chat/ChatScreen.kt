package com.kalpi.prochat.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Spacer
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
import com.kalpi.prochat.ui.theme.ProChatTheme // Assuming your theme is named ProChatTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*


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
    chatViewModel: ChatViewModel = viewModel() // Uses the default ViewModel factory
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
                textState = textState,
                onTextChanged = { textState = it },
                onSendClick = {
                    if (textState.text.isNotBlank()) {
                        // For Day 1, we'll just clear the input.
                        // Day 2: chatViewModel.sendMessage(textState.text)
                        // Log.d("ChatScreen", "Send clicked: ${textState.text}")
                        textState = TextFieldValue("") // Clear input after "send"
                    }
                }
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
                            verticalArrangement = Arrangement.spacedBy(8.dp) // Spacing between messages
                        ) {
                            items(state.messages, key = { it.id }) { message ->
                                MessageBubble(
                                    message = message,
                                    currentUserId = ChatViewModel.CURRENT_USER_ID
                                )
                            }
                        }
                    }
                }
            }
        }
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
    currentUserId: String
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
    val formattedTime = remember(message.timestamp) {
        simpleDateFormat.format(Date(message.timestamp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isCurrentUser) 64.dp else 0.dp, // Indent opposite side
                end = if (isCurrentUser) 0.dp else 64.dp    // Indent opposite side
            ),
        contentAlignment = bubbleContainerAlignment // This aligns the content (bubble) within the Column
    ) {
        Column {
        if (message.messageType == MessageType.SYSTEM) {
            // System Message Styling
            Text(
                text = message.text,
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
                Column { // To stack text and timestamp vertically
                    Text(
                        text = message.text,
                        color = textColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formattedTime,
                        color = textColor.copy(alpha = 0.7f), // Slightly muted timestamp
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.End) // Timestamp to the right within the bubble
                    )
                }
            }
        }
    }
} }


/**
 * Composable for the chat input area, including a text field and a send button.
 *
 * @param textState The current [TextFieldValue] of the input field.
 * @param onTextChanged Callback when the text in the input field changes.
 * @param onSendClick Callback when the send button is clicked.
 */
@Composable
fun ChatInput(
    textState: TextFieldValue,
    onTextChanged: (TextFieldValue) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 4.dp // Add some elevation to distinguish from messages
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textState,
                onValueChange = onTextChanged,
                placeholder = { Text("Type a message...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors( // Using TextFieldDefaults.colors for M3
                    focusedIndicatorColor = Color.Transparent, // No indicator line
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onSendClick,
                enabled = textState.text.isNotBlank(),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = "Send message"
                )
            }
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
                timestamp = System.currentTimeMillis()
            ),
            currentUserId = ChatViewModel.CURRENT_USER_ID
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
                senderId = ChatViewModel.OTHER_USER_ID,
                text = "This is a received message! Shorter this time.",
                timestamp = System.currentTimeMillis()
            ),
            currentUserId = ChatViewModel.CURRENT_USER_ID
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
                timestamp = System.currentTimeMillis(),
                messageType = MessageType.SYSTEM
            ),
            currentUserId = ChatViewModel.CURRENT_USER_ID
        )
    }
}
// endregion
