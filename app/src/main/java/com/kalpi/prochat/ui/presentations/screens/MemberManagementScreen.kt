package com.kalpi.prochat.ui.presentations.screens


import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.kalpi.prochat.data.model.Member
import com.kalpi.prochat.data.repository.ChatRoomRepository
import com.kalpi.prochat.ui.presentations.viewmodel.MemberManagementViewModel
import com.kalpi.prochat.ui.presentations.viewmodel.MemberManagementViewModelFactory
import com.kalpi.prochat.data.model.UiMember
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberManagementScreen(
    viewModel: MemberManagementViewModel,// Passed from navigation
    onNavigateBack: (String?) -> Unit // A callback to navigate back with a message
) {

    val uiMembers by viewModel.uiMembers.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val uiEvent by viewModel.uiEvent.collectAsState()
    val currentUserId by viewModel.currenUserId.collectAsState()
    val chatRoomTitle by viewModel.chatRoomTitle.collectAsState()
    val chatRoomAvatarUrl by viewModel.chatRoomAvatarUrl.collectAsState()
    val context = LocalContext.current
    // State for the "Add Member" dialog
    var showAddMemberDialog by remember { mutableStateOf(false) }

    // State for the new member's ID
    var newMemberIdInput by remember { mutableStateOf("") }

    // State for the ownership transfer confirmation dialog
    var showTransferOwnershipDialog by remember { mutableStateOf(false) }
    var selectedMemberIdToTransfer by remember { mutableStateOf<String?>(null) }
    var showAdminLeaveDialog by remember { mutableStateOf(false) }

    var showAvatarOptionsDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // ⭐ Call the ViewModel function to handle the upload
            viewModel.uploadGroupAvatar(uri)
            // You can also emit a toast here if you like
            // coroutineScope.launch {
            //     Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show()
            // }
        } else {
            // Handle the case where the user cancels the picker
            coroutineScope.launch {
                Toast.makeText(context, "Image selection cancelled.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Members") },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack(null) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ⭐ Chat Room Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // The clickable circular avatar
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(color = MaterialTheme.colorScheme.primaryContainer)
                        .clickable(enabled = isAdmin) {
                            showAvatarOptionsDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (chatRoomAvatarUrl.isNullOrBlank()) {
                        val initials = chatRoomTitle.split(" ")
                            .take(2)
                            .map { it.firstOrNull()?.uppercaseChar() ?: ' ' }
                            .joinToString("")
                            .trim()

                        Text(
                            text = initials.ifEmpty { "?" },
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Image(
                            painter = rememberAsyncImagePainter(model = chatRoomAvatarUrl),
                            contentDescription = "Group Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Chat room title
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = chatRoomTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
            Text(
                text = "Members",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )


            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiMembers) { uiMember ->
                    // Pass the single, unified UiMember object
                    MemberListItem(
                        uiMember = uiMember,
                        currentUserIdIsAdmin = isAdmin,
                        onRemoveMember = { viewModel.removeMember(uiMember.userId) },
                        onTransferOwnership = {
                            selectedMemberIdToTransfer = uiMember.userId
                            showTransferOwnershipDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Admin Actions (conditionally shown)
            if (isAdmin) {
                // Add Member button (you'll need to implement the dialog for this)
                Button(
                    onClick = { showAddMemberDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Member")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Leave Group button
            Button(
                onClick = { viewModel.leaveGroup() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(if (isAdmin) "Leave Group (Admin)" else "Leave Group")
            }
        }
    }

    // Handle UI events from the ViewModel
    LaunchedEffect(uiEvent) {
        when (val event = uiEvent) {
            is MemberManagementViewModel.UiEvent.ShowToast -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
            is MemberManagementViewModel.UiEvent.NavigateBack -> {
                onNavigateBack(event.message)
            }
            is MemberManagementViewModel.UiEvent.ShowAdminLeaveWarning -> {
                showAdminLeaveDialog = true
            }
            MemberManagementViewModel.UiEvent.Idle -> {}
        }
    }

    // Add Member Dialog
    if (showAddMemberDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Add New Member") },
            text = {
                OutlinedTextField(
                    value = newMemberIdInput,
                    onValueChange = { newMemberIdInput = it },
                    label = { Text("Enter Member ID") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addMember(newMemberIdInput)
                        showAddMemberDialog = false
                        newMemberIdInput = ""
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                Button(onClick = { showAddMemberDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Ownership Transfer Confirmation Dialog
    if (showTransferOwnershipDialog) {
        AlertDialog(
            onDismissRequest = { showTransferOwnershipDialog = false },
            title = { Text("Transfer Ownership") },
            text = { Text("Are you sure you want to transfer ownership of this group? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedMemberIdToTransfer?.let {
                            viewModel.transferOwnership(it)
                        }
                        showTransferOwnershipDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Transfer")
                }
            },
            dismissButton = {
                Button(onClick = { showTransferOwnershipDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAdminLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showAdminLeaveDialog = false },
            title = { Text("Cannot Leave Group") },
            text = { Text("You are the admin of this group. You must transfer ownership to another member before you can leave.") },
            confirmButton = {
                Button(onClick = { showAdminLeaveDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // ⭐ CORRECTED: Avatar Options Dialog using Dialog and Card
    if (showAvatarOptionsDialog) {
        Dialog(onDismissRequest = { showAvatarOptionsDialog = false }) {
            Card(
                modifier = Modifier.width(300.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Group Photo",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                    HorizontalDivider()
                    TextButton(
                        onClick = {
                            showAvatarOptionsDialog = false
                            // TODO: Implement image selection logic
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Change Group Photo")
                    }
                    TextButton(
                        onClick = {
                            showAvatarOptionsDialog = false
                            // TODO: Implement view profile logic
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View Profile Photo")
                    }
                }
            }
        }
    }
}

@Composable
fun MemberListItem(
    uiMember: MemberManagementViewModel.UiMember, // The single, new data class
    currentUserIdIsAdmin: Boolean,
    onRemoveMember: () -> Unit,
    onTransferOwnership: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // for presence indicator
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = if (uiMember.isOnline) Color.Green else Color.Gray,
                    shape = CircleShape
                )
                .align(Alignment.CenterVertically)
        )
        Spacer(Modifier.width(8.dp))

        // User's name and role
        Column(modifier = Modifier.weight(1f)) {
            // Use the name from the UiMember object
            Text(text = uiMember.name, style = MaterialTheme.typography.titleMedium)
            // Use the role from the UiMember object
            Text(text = uiMember.role, style = MaterialTheme.typography.bodySmall)
        }

        // Action buttons (conditionally shown)
        if (currentUserIdIsAdmin && !uiMember.isCurrentUser) {
            IconButton(onClick = onRemoveMember) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove member",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            // Logic to prevent transferring ownership to yourself
            if (!uiMember.isAdmin) { // You can't transfer ownership to an existing admin
                IconButton(onClick = onTransferOwnership) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Transfer ownership",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}