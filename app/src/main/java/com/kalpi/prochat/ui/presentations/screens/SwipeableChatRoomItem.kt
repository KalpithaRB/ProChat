package com.kalpi.prochat.ui.presentations.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kalpi.prochat.data.model.ChatRoom
import com.kalpi.prochat.ui.presentations.viewmodel.ChatRoomListViewModel
import com.kalpi.prochat.ui.presentations.ChatRoomListItem



//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun SwipeableChatRoomItem(
//    chatRoom: ChatRoom,
//    onRoomClicked: (String, String) -> Unit,
//    onArchive: (String) -> Unit,
//    onDelete: (String) -> Unit,
//    onUnarchive: (String) -> Unit
//) {
//    val dismissState = rememberSwipeToDismissBoxState(
//        confirmValueChange = {
//            when (it) {
//                SwipeToDismissBoxValue.StartToEnd -> {
//                    if (chatRoom.isArchived) {
//                        onUnarchive(chatRoom.roomId)
//                    } else {
//                        onArchive(chatRoom.roomId)
//                    }
//                    true
//                }
//                SwipeToDismissBoxValue.EndToStart -> {
//                    onDelete(chatRoom.roomId)
//                    true
//                }
//                else -> false
//            }
//        }
//    )
//
//    SwipeToDismissBox(
//        state = dismissState,
//        enableDismissFromEndToStart = true,
//        enableDismissFromStartToEnd = true,
//        backgroundContent = {
//            val direction = dismissState.dismissDirection
//            val color by animateColorAsState(
//                when (dismissState.targetValue) {
//                    SwipeToDismissBoxValue.StartToEnd -> if (chatRoom.isArchived) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
//                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
//                    else -> Color.Transparent
//                }, label = "Swipe Color"
//            )
//            val icon = when (dismissState.targetValue) {
//                SwipeToDismissBoxValue.StartToEnd -> if (chatRoom.isArchived) Icons.Default.Add else Icons.Default.Archive
//                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
//                else -> null
//            }
//            val description = when (dismissState.targetValue) {
//                SwipeToDismissBoxValue.StartToEnd -> if (chatRoom.isArchived) "Unarchive" else "Archive"
//                SwipeToDismissBoxValue.EndToStart -> "Delete"
//                else -> null
//            }
//            Box(
//                Modifier
//                    .fillMaxSize()
//                    .background(color)
//                    .padding(horizontal = 20.dp),
//                contentAlignment = when (direction) {
//                    DismissDirection.StartToEnd -> Alignment.CenterStart
//                    DismissDirection.EndToStart -> Alignment.CenterEnd
//                    else -> Alignment.Center
//                }
//            ) {
//                if (icon != null) {
//                    Icon(
//                        imageVector = icon,
//                        contentDescription = description,
//                        tint = MaterialTheme.colorScheme.onPrimary
//                    )
//                }
//            }
//        },
//        content = {
//            ChatRoomListItem(
//                chatRoom = chatRoom,
//                onRoomClicked = onRoomClicked,
//                onToggleMuteClicked = { /* onToggleMute logic here, it is not being used right now*/ }
//            )
//        }
//    )
//}


