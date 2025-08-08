package com.kalpi.prochat.ui.presentations.screens

import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.graphics.Color
//import androidx.privacysandbox.tools.core.generator.build
//import androidx.wear.compose.material.placeholder
import com.kalpi.prochat.data.ChatItem
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.sp
import com.kalpi.prochat.ui.chat.ChatInput
import com.kalpi.prochat.ui.chat.ChatUiState
import kotlinx.coroutines.launch
import java.util.Locale
import com.kalpi.prochat.ui.presentations.viewmodel.ChatViewModel
import com.kalpi.prochat.ui.chat.MessageStatusIcon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.kalpi.prochat.ui.chat.FileMessage
import com.kalpi.prochat.ui.chat.TextMessage
import com.kalpi.prochat.ui.chat.ImageMessage


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
    onBackClicked: () -> Unit,
    onDeleteSuccess: () -> Unit,
    roomName: String
) {
    val uiState by chatViewModel.uiState.collectAsState()
    val listState = rememberLazyListState() // For auto-scrolling
    var textState by remember { mutableStateOf(TextFieldValue("")) }

    val uploadProgress by chatViewModel.uploadProgress.collectAsState()
    // Coroutine scope for launching animations or other suspend functions if needed
     val coroutineScope = rememberCoroutineScope()

    val showScrollToBottomButton by remember {
        derivedStateOf {
            !listState.canScrollForward
        }
    }
    val context = LocalContext.current

    var showDeleteDialog by remember { mutableStateOf(false) }

    // Use a LaunchedEffect to listen for the navigation event
    // that the ViewModel will emit after a successful deletion.
    LaunchedEffect(Unit) {
        chatViewModel.deletionSuccess.collect {
            onDeleteSuccess()
        }
    }


    LaunchedEffect(Unit) {
        chatViewModel.exportFileUri.collect { uri ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "text/plain"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share chat export"))
        }
    }

    // LaunchedEffect to mark messages as read when the screen is shown
    LaunchedEffect(listState) {
        // Collect snapshots of the last visible item index
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val currentMessages = (uiState as? ChatUiState.Content)?.messages ?: emptyList()
                if (lastVisibleIndex != null && currentMessages.isNotEmpty()) {
                    // Get all messages that are visible on the screen
                    val visibleMessages = listState.layoutInfo.visibleItemsInfo.mapNotNull {
                        (currentMessages[it.index] as? ChatItem.Message)?.message
                    }

                    // Mark any visible messages that are not from the current user and are not already read
                    val messagesToMarkAsRead = visibleMessages.filter {
                        it.senderId != chatViewModel.currentUserId && it.status != MessageStatus.READ
                    }

                    if (messagesToMarkAsRead.isNotEmpty()) {
                        // Call a new ViewModel function to mark these messages as read
                        chatViewModel.markMessagesAsRead(messagesToMarkAsRead.map { it.id })
                    }
                }
            }
    }

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
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                title = { Text(roomName) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    var expanded by remember { mutableStateOf(false) }

                    // Three-dot menu icon
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = Color.White
                        )
                    }

                    // Dropdown menu
                    DropdownMenu(
                        expanded = expanded, onDismissRequest = { expanded = false }
                    ) {
                        // "Export Chat" menu item
                        DropdownMenuItem(
                            text = { Text("Export Chat") },
                            onClick = {
                                expanded = false
                                chatViewModel.onExportChatClicked()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Download,
                                    contentDescription = "Export"
                                )
                            }
                        )
                        // "Export Chat (ZIP)" menu item
                        DropdownMenuItem(
                            text = { Text("Export Chat (ZIP)") },
                            onClick = {
                                expanded = false
                                chatViewModel.onExportChatAsZipClicked() // New ViewModel function
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.CreateNewFolder, contentDescription = "Export ZIP")
                            }
                        )

                        // "Delete Chat" menu item
                        DropdownMenuItem(
                            text = { Text("Delete Chatroom") },
                            onClick = {
                                expanded = false
                                showDeleteDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete Chatroom"
                                )
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            ChatInput(
                onSendMessage = chatViewModel::sendMessage,
                onSendImageMessage = chatViewModel::prepareAndSendImageMessage,
                onSendFileMessage = chatViewModel::prepareAndSendFileMessage,
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


            AnimatedVisibility(
                visible = !showScrollToBottomButton, // CORRECTED: Show when at the bottom
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp, end = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            val messages = (uiState as? ChatUiState.Content)?.messages
                            if (messages != null && messages.isNotEmpty()) {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Scroll to bottom"
                    )
                }
            }
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete Chatroom") },
                    text = { Text("Are you sure you want to delete this chatroom? This action cannot be undone.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteDialog = false
                                chatViewModel.deleteChatroom() // Call the ViewModel function
                            }
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
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
    val isCurrentUser = message.senderId == currentUserId

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
                .alpha(if (message.status == MessageStatus.SENDING) 0.7f else 1.0f)
                .padding(10.dp)
        ) {
            Column {
                // CORRECTED LOGIC: Use a 'when' statement to render the correct content
                when (message.messageType) {
                    MessageType.TEXT -> {
                        message.text?.let { textContent ->
                            TextMessage(
                                text = textContent,
                                color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    MessageType.IMAGE -> {
                        ImageMessage(
                            message = message,
                            isCurrentUser = isCurrentUser,
                            uploadProgress = uploadProgress,
                            onRetryClick = onRetryClick
                        )
                    }
                    MessageType.FILE -> {
                        FileMessage(
                            message = message,
                            onRetryClick = onRetryClick
                        )
                    }
                    MessageType.SYSTEM -> {
                        Text(
                            text = message.text ?: "",
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                    MessageType.USER -> {
                        // This case can be treated the same as TEXT, or handled with a generic text message if needed
                        message.text?.let { textContent ->
                            TextMessage(
                                text = textContent,
                                color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

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

                    Spacer(modifier = Modifier.width(4.dp))

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


