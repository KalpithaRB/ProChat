package com.kalpi.prochat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kalpi.prochat.ui.presentations.screens.ChatScreen
import com.kalpi.prochat.ui.theme.ProChatTheme
import androidx.lifecycle.viewmodel.compose.viewModel // Standard viewModel delegate
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.kalpi.prochat.data.repository.ChatRepository // Your ChatRepository
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kalpi.prochat.data.repository.ChatRoomRepository
import com.kalpi.prochat.ui.presentations.screens.ChatRoomListScreen
import com.kalpi.prochat.ui.presentations.viewmodel.ChatRoomListViewModel
import com.kalpi.prochat.ui.presentations.viewmodel.ChatRoomListViewModelFactory
import com.kalpi.prochat.ui.presentations.viewmodel.ChatViewModel
import com.kalpi.prochat.ui.presentations.viewmodel.ChatViewModelFactory
import com.kalpi.prochat.ui.theme.ProChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProChatTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Get a Firestore instance for all repositories
                    val firestore = Firebase.firestore

                    // Get the unique user ID, which is needed for all ViewModels
                    val uniqueUserId = ChatViewModel.getOrCreateUserId(LocalContext.current)

                    // Navigation State: Holds the ID of the chat room we are currently viewing.
                    // If it's null, we display the ChatRoomListScreen.
                    var currentRoomId: String? by remember { mutableStateOf(null) }

                    // We need a conditional display based on our navigation state
                    if (currentRoomId == null) {
                        // Display the list of chat rooms
                        val chatRoomRepository = ChatRoomRepository(firestore)
                        val chatRoomListViewModel: ChatRoomListViewModel = viewModel(
                            factory = ChatRoomListViewModelFactory(chatRoomRepository, uniqueUserId)
                        )
                        ChatRoomListScreen(
                            chatRoomListViewModel = chatRoomListViewModel,
                            onRoomClicked = { roomId ->
                                // When a room is clicked, update the state to the selected room ID.
                                // This will cause a recomposition, showing the ChatScreen.
                                currentRoomId = roomId
                            }
                        )
                    } else {
                        // Display the chat screen for the selected room
                        val chatRepository = ChatRepository(firestore)
                        val chatViewModel: ChatViewModel = viewModel(
                            factory = ChatViewModelFactory(chatRepository, currentRoomId!!, uniqueUserId)
                        )
                        ChatScreen(
                            chatViewModel = chatViewModel,
                            onBackClicked = {
                                // When the back button is clicked, reset the state to null.
                                // This will cause a recomposition, returning to the list screen.
                                currentRoomId = null
                            }
                        )
                    }
                }
            }
        }
    }
}
