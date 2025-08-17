package com.kalpi.prochat.ui.presentations.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kalpi.prochat.data.model.Member
import com.kalpi.prochat.data.repository.ChatRoomRepository
import com.kalpi.prochat.ui.presentations.viewmodel.MemberManagementViewModel
import com.kalpi.prochat.ui.presentations.viewmodel.MemberManagementViewModelFactory
import com.kalpi.prochat.data.model.UiMember
import kotlinx.coroutines.launch

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

    val coroutineScope = rememberCoroutineScope()

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
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiMembers) { uiMember ->
                    // Pass the single, unified UiMember object
                    MemberListItem(
                        uiMember = uiMember,
                        onRemoveMember = { viewModel.removeMember(uiMember.userId) },
                        onTransferOwnership = { viewModel.transferOwnership(uiMember.userId) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Admin Actions (conditionally shown)
            if (isAdmin) {
                // Add Member button (you'll need to implement the dialog for this)
                Button(
                    onClick = { /* TODO: Show a dialog to add a new member */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Member")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Leave Group button
            Button(
                onClick = {
                    // Check if current user is admin before showing a confirmation
                    if (isAdmin) {
                        // TODO: Show a confirmation dialog for admin to ensure ownership transfer is handled
                    } else {
                        viewModel.leaveGroup()
                    }
                },
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
                // TODO: Show a toast with event.message
            }
            is MemberManagementViewModel.UiEvent.NavigateBack -> {
                onNavigateBack(event.message)
            }
            MemberManagementViewModel.UiEvent.Idle -> {}
        }
    }
}

@Composable
fun MemberListItem(
    uiMember: MemberManagementViewModel.UiMember, // The single, new data class
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
        // Placeholder for presence indicator
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
        if (uiMember.isAdmin && !uiMember.isCurrentUser) {
            IconButton(onClick = onRemoveMember) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove member",
                    tint = MaterialTheme.colorScheme.error
                )
            }
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