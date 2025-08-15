package com.kalpi.prochat

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kalpi.prochat.ui.presentations.screens.ChatScreen
import com.kalpi.prochat.ui.chat.UserManager
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
import com.google.firebase.messaging.FirebaseMessaging
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.kalpi.prochat.data.repository.ChatRoomRepository
import com.kalpi.prochat.ui.presentations.screens.ChatRoomListScreen
import com.kalpi.prochat.ui.presentations.viewmodel.ChatRoomListViewModel
import com.kalpi.prochat.ui.presentations.viewmodel.ChatRoomListViewModelFactory
import com.kalpi.prochat.ui.presentations.viewmodel.ChatViewModel
import com.kalpi.prochat.ui.presentations.viewmodel.ChatViewModelFactory
import com.kalpi.prochat.data.repository.UserRepository
import com.kalpi.prochat.data.repository.RealUserRepository
import com.kalpi.prochat.ui.theme.ProChatTheme
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.DisposableEffect
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kalpi.prochat.data.repository.RealChatRepository
import com.kalpi.prochat.data.repository.RealChatRoomRepository

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProChatTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Get a Firestore instance for all repositories
                    val firestore = Firebase.firestore

                    // Get the unique user ID, which is needed for all ViewModels
                    val uniqueUserId = UserManager.getOrCreateUserId(LocalContext.current)

                    val initialRoomId: String? = intent?.data?.lastPathSegment
                    val initialRoomName: String? = if (initialRoomId != null) "Chat Pro" else null
                    Log.d("MainActivity", "Initial room from deep link: $initialRoomId")

                    // Navigation State: Holds the ID of the chat room we are currently viewing.
                    // If it's null, we display the ChatRoomListScreen.
                    var currentRoomId: String? by remember { mutableStateOf(initialRoomId) }

                    // room name for the top bar.
                    var currentRoomName: String? by remember { mutableStateOf(initialRoomName) }

                    DisposableEffect(currentRoomId) {
                        AppState.currentChatRoomId = currentRoomId
                        onDispose {
                            AppState.currentChatRoomId = null
                        }
                    }

                    if (currentRoomId == null) {
                        // Display the list of chat rooms
                        val chatRoomRepository: ChatRoomRepository =
                            RealChatRoomRepository(firestore)
                        val userRepository: UserRepository = RealUserRepository(firestore)
                        val chatRoomListViewModel: ChatRoomListViewModel = viewModel(
                            factory = ChatRoomListViewModelFactory(chatRoomRepository,userRepository, uniqueUserId)
                        )
                        ChatRoomListScreen(
                            chatRoomListViewModel = chatRoomListViewModel,
                            onRoomClicked = { roomId, roomName ->
                                currentRoomId = roomId
                                currentRoomName = roomName
                            }
                        )
                    } else {
                        val context = LocalContext.current
                        val application = context.applicationContext as Application
                        // Display the chat screen for the selected room
                        val chatRepository: ChatRepository = RealChatRepository(firestore)
                        val chatRoomRepository: ChatRoomRepository = RealChatRoomRepository(firestore)

                        val chatViewModel: ChatViewModel = viewModel(
                            factory = ChatViewModelFactory(
                                application,
                                chatRepository,
                                chatRoomRepository,
                                currentRoomId!!,
                                uniqueUserId
                            )
                        )
                        ChatScreen(
                            chatViewModel = chatViewModel,
                            roomName = currentRoomName ?: "Chat Pro",
                            onBackClicked = {
                                currentRoomId = null
                                currentRoomName = null
                            },
                            onDeleteSuccess = {
                                currentRoomId = null
                                currentRoomName = null
                            }
                        )
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
                }
            }
        }
    }
}
