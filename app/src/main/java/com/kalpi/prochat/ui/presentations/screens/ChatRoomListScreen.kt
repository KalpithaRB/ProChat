package com.kalpi.prochat.ui.presentations.screens


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
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
import androidx.compose.ui.unit.dp
import com.kalpi.prochat.ui.presentations.viewmodel.ChatRoomListViewModel
import com.kalpi.prochat.ui.presentations.ChatRoomListItem
import com.kalpi.prochat.ui.presentations.viewmodel.ChatRoomListUiState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomListScreen(
    chatRoomListViewModel: ChatRoomListViewModel,
    onRoomClicked: (String) -> Unit
) {
    val uiState by chatRoomListViewModel.uiState.collectAsState()
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var showJoinRoomDialog by remember { mutableStateOf(false) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    var newRoomName by remember { mutableStateOf("") }
    var newRecipientId by remember { mutableStateOf("") }
    var joinRoomId by remember { mutableStateOf("") }

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
        // NEW: Animated Floating Action Button
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // The two smaller FABs are only visible when the menu is expanded
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
                        // FAB for joining a room
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
                        // FAB for creating a room
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

                // The main FAB that toggles the menu
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
                                ChatRoomListItem(
                                    chatRoom = chatRoom,
                                    onRoomClicked = {
                                        // When a room is clicked, also mark it as read
                                        chatRoomListViewModel.onRoomClicked(it)
                                        onRoomClicked(it)
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

    // This is the dialog that appears when the FAB is clicked
    if (showCreateRoomDialog) {
        AlertDialog(
            onDismissRequest = { showCreateRoomDialog = false },
            title = { Text("Create New Room") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newRoomName,
                        onValueChange = { newRoomName = it },
                        label = { Text("Room Name") },
                        singleLine = true,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = newRecipientId,
                        onValueChange = { newRecipientId = it },
                        label = { Text("Recipient User ID") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Call the ViewModel function to create the room
                        chatRoomListViewModel.createNewRoom(newRoomName, newRecipientId)
                        // Dismiss the dialog and reset the name
                        showCreateRoomDialog = false
                        newRoomName = ""
                        newRecipientId = ""
                    },
                    // The button is only enabled if both fields are not blank
                    enabled = newRoomName.isNotBlank() && newRecipientId.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateRoomDialog = false
                    newRoomName = "" // Also clear the text on dismiss
                    newRecipientId = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // NEW: AlertDialog for joining a room
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
                        // TODO: Call ViewModel function to join the room here
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


}