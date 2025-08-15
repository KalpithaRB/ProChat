package com.kalpi.prochat.ui.presentations.screens


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import com.kalpi.prochat.data.model.ChatRoom
import com.kalpi.prochat.data.model.User
import com.kalpi.prochat.data.repository.UserRepository
import kotlinx.coroutines.flow.collectLatest
import com.kalpi.prochat.ui.presentations.viewmodel.ChatRoomListViewModel
import com.kalpi.prochat.ui.presentations.ChatRoomListItem
import com.kalpi.prochat.ui.presentations.viewmodel.ChatRoomListUiState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomListScreen(
    chatRoomListViewModel: ChatRoomListViewModel,
    userRepository: UserRepository,
    onRoomClicked: (String, String) -> Unit
) {
    val uiState by chatRoomListViewModel.uiState.collectAsState()
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var showJoinRoomDialog by remember { mutableStateOf(false) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    var isGroupChat by remember { mutableStateOf(false) }
    var newRoomName by remember { mutableStateOf("") }
    var newRecipientId by remember { mutableStateOf("") }
    var joinRoomId by remember { mutableStateOf("") }
    var showShareRoomIdDialog by remember { mutableStateOf(false) }
    var sharedRoomId by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()


    var showDeleteDialog by remember { mutableStateOf(false) }
    var roomToDelete by remember { mutableStateOf<ChatRoom?>(null) }



    // LaunchedEffect to listen for one-time events from the ViewModel
    LaunchedEffect(key1 = Unit) {
        chatRoomListViewModel.uiEvent.collectLatest { event ->
            when (event) {
                is ChatRoomListViewModel.UiEvent.RoomCreated -> {
                    sharedRoomId = event.roomId
                    showShareRoomIdDialog = true
                }
                is ChatRoomListViewModel.UiEvent.RoomCreatedAndNavigate -> {
                    onRoomClicked(event.roomId, event.roomName)
                }
                is ChatRoomListViewModel.UiEvent.RoomJoined -> {
                    // TODO: We need to get the room name here to navigate correctly
                    // We'll address this in the next step
                }
                is ChatRoomListViewModel.UiEvent.ShowToast -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(event.message)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Rooms") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                AnimatedVisibility(
                    visible = isFabMenuExpanded,
                    enter = fadeIn(animationSpec = tween(durationMillis = 200)) + slideInVertically(
                        animationSpec = tween(durationMillis = 200),
                        initialOffsetY = { fullHeight -> fullHeight / 2 }
                    ),
                    exit = fadeOut(animationSpec = tween(durationMillis = 200)) + slideOutVertically(
                        animationSpec = tween(durationMillis = 200),
                        targetOffsetY = { fullHeight -> fullHeight / 2 }
                    )
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Join Room", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(8.dp))
                            FloatingActionButton(
                                onClick = {
                                    showJoinRoomDialog = true
                                    isFabMenuExpanded = false
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.GroupAdd, contentDescription = "Join Room")
                            }
                        }
                        Spacer(Modifier.size(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Create Room", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(8.dp))
                            FloatingActionButton(
                                onClick = {
                                    showCreateRoomDialog = true
                                    isFabMenuExpanded = false
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Create, contentDescription = "Create New Room")
                            }
                        }
                        Spacer(Modifier.size(16.dp))
                    }
                }
                FloatingActionButton(onClick = { isFabMenuExpanded = !isFabMenuExpanded }) {
                    Icon(
                        if (isFabMenuExpanded) Icons.Default.Add else Icons.Default.Add,
                        contentDescription = "Expand Menu"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is ChatRoomListUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ChatRoomListUiState.Error -> {
                    Text(
                        text = "Error: ${state.errorMessage}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ChatRoomListUiState.Content -> {
                    if (state.chatRooms.isEmpty()) {
                        Text(
                            text = "No chat rooms yet. Start a new chat!",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            items(items = state.chatRooms, key = { it.roomId }) { chatRoom ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = {dismissValue ->
                                        when (dismissValue) {
                                            // When the user swipes from start to end (left to right)
                                            SwipeToDismissBoxValue.StartToEnd -> {
                                                // Call the ViewModel function to soft delete the chatroom
                                                chatRoomListViewModel.deleteChatroom(chatRoom.roomId)
                                                true // Allow the dismissal animation to complete
                                            }
                                            // When the user swipes from end to start (right to left)
                                            SwipeToDismissBoxValue.EndToStart -> {
                                                // For now, we won't implement an archive function, so we block this swipe
                                                false // Do not allow the dismissal
                                            }
                                            // When the item settles back in place
                                            SwipeToDismissBoxValue.Settled -> false
                                        }
                                    }
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    enableDismissFromStartToEnd = true, // Swipe right to delete
                                    enableDismissFromEndToStart = true, // Swipe left to archive
                                    // CORRECTED: Use 'backgroundContent' instead of 'background'
                                    backgroundContent = {
                                        // This is the UI that is shown behind the list item
                                        val color by animateColorAsState(
                                            when (dismissState.targetValue) {
                                                SwipeToDismissBoxValue.StartToEnd -> Color.Red // Swiping from start (left)
                                                SwipeToDismissBoxValue.EndToStart -> Color(0xFFF9A825) // Swiping from end (right)
                                                SwipeToDismissBoxValue.Settled -> Color.Transparent
                                            }, label = "SwipeBackground"
                                        )
                                        val icon = when (dismissState.targetValue) {
                                            SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Delete
                                            SwipeToDismissBoxValue.EndToStart -> Icons.Default.Archive
                                            else -> null
                                        }
                                        val alignment = when (dismissState.targetValue) {
                                            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                            else -> Alignment.Center
                                        }

                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .background(color)
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = alignment
                                        ) {
                                            if (icon != null) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = "Swipe Action",
                                                    tint = Color.White
                                                )
                                            }
                                        }
                                    },
                                    content = {
                                        // This Row will contain your existing chat room item and the new menu button
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    // This is the click listener for the main chat item
                                                    onRoomClicked(chatRoom.roomId, chatRoom.title)
                                                }
                                                .background(MaterialTheme.colorScheme.surface)
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // This is your ChatRoomListItem's content, but we'll put it directly here
                                            Column(
                                                modifier = Modifier.weight(1f) // This makes the Column take up all available space
                                            ) {Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(chatRoom.title, style = MaterialTheme.typography.titleMedium)
                                                if (chatRoom.muted) {
                                                    Spacer(Modifier.width(8.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.NotificationsOff,
                                                        contentDescription = "Muted",
                                                        modifier = Modifier.size(20.dp),
                                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                                Text(chatRoom.title, style = MaterialTheme.typography.titleMedium)
                                                chatRoom.lastMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                                            }

                                            // Three-dot menu button
                                            Box {
                                                var showMenu by remember { mutableStateOf(false) }

                                                IconButton(onClick = { showMenu = true }) {
                                                    Icon(Icons.Default.MoreVert, contentDescription = "Room Options")
                                                }

                                                DropdownMenu(
                                                    expanded = showMenu,
                                                    onDismissRequest = { showMenu = false }
                                                ) {
                                                    // Toggle Mute Menu Item
                                                    DropdownMenuItem(
                                                        text = {
                                                            val text = if (chatRoom.muted) "Unmute Chat" else "Mute Chat"
                                                            Text(text)
                                                        },
                                                        onClick = {
                                                            showMenu = false
                                                            chatRoomListViewModel.onToggleMute(chatRoom.roomId)
                                                        }
                                                    )
                                                    // Delete Chatroom Menu Item
                                                    DropdownMenuItem(
                                                        text = { Text("Delete Chatroom") },
                                                        onClick = {
                                                            showMenu = false
                                                            roomToDelete = chatRoom
                                                            showDeleteDialog = true
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = DividerDefaults.Thickness, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    if (showCreateRoomDialog) {
        var users by remember { mutableStateOf<List<User>>(emptyList()) }
        var selectedUserIds by remember { mutableStateOf(emptySet<String>()) }
        var isLoadingUsers by remember { mutableStateOf(true) }

        // Fetch users once when the dialog is shown
        LaunchedEffect(Unit) {
            isLoadingUsers = true
            val fetchedUsers = userRepository.getUsers()
            users = fetchedUsers
            isLoadingUsers = false
        }

        AlertDialog(
            onDismissRequest = {
                showCreateRoomDialog = false
                newRoomName = ""
                isGroupChat = false
                selectedUserIds = emptySet()
            },
            title = { Text(if (isGroupChat) "Create Group Chat" else "Create Direct Message") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newRoomName,
                        onValueChange = { newRoomName = it },
                        label = { Text("Room Name") },
                        singleLine = true,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // User selection UI
                    if (isLoadingUsers) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (users.isEmpty()) {
                        Text("No other users found.")
                    } else {
                        Text("Select Users:", style = MaterialTheme.typography.labelLarge)
                        LazyColumn(
                            modifier = Modifier
                                .heightIn(max = 200.dp)
                                .padding(top = 8.dp)
                        ) {
                            items(users) { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedUserIds = if (selectedUserIds.contains(user.userId)) {
                                                selectedUserIds - user.userId
                                            } else {
                                                selectedUserIds + user.userId
                                            }
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedUserIds.contains(user.userId),
                                        onCheckedChange = { isChecked ->
                                            selectedUserIds = if (isChecked) {
                                                selectedUserIds + user.userId
                                            } else {
                                                selectedUserIds - user.userId
                                            }
                                        }
                                    )
                                    Text(
                                        text = user.name,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isGroupChat,
                            onCheckedChange = { isGroupChat = it }
                        )
                        Text("Create as Group Chat")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        chatRoomListViewModel.createNewRoom(
                            roomName = newRoomName,
                            recipientIds = selectedUserIds.toList(),
                            isGroupChat = isGroupChat
                        )
                        showCreateRoomDialog = false
                        newRoomName = ""
                        selectedUserIds = emptySet()
                        isGroupChat = false
                    },
                    enabled = newRoomName.isNotBlank() && selectedUserIds.isNotEmpty()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateRoomDialog = false
                    newRoomName = ""
                    selectedUserIds = emptySet()
                    isGroupChat = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showJoinRoomDialog) {
        AlertDialog(
            onDismissRequest = { showJoinRoomDialog = false },
            title = { Text("Join a Room") },
            text = {
                OutlinedTextField(
                    value = joinRoomId,
                    onValueChange = { joinRoomId = it },
                    label = { Text("Enter Room ID") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        chatRoomListViewModel.joinRoom(joinRoomId)
                        showJoinRoomDialog = false
                        joinRoomId = ""
                    },
                    enabled = joinRoomId.isNotBlank()
                ) {
                    Text("Join")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showJoinRoomDialog = false
                    joinRoomId = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showShareRoomIdDialog) {
        AlertDialog(
            onDismissRequest = {
                showShareRoomIdDialog = false
                sharedRoomId = ""
            },
            title = { Text("Room Created!") },
            text = {
                Column {
                    Text("Share this ID with your friend to join the room:")
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = sharedRoomId,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showShareRoomIdDialog = false
                        sharedRoomId = ""
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    if (showDeleteDialog && roomToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                roomToDelete = null
            },
            title = { Text("Delete Chatroom") },
            text = { Text("Are you sure you want to delete '${roomToDelete?.title}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Call the new ViewModel function
                        chatRoomListViewModel.deleteChatroom(roomToDelete!!.roomId)
                        showDeleteDialog = false
                        roomToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        roomToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}