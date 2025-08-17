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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.kalpi.prochat.data.repository.PresenceRepository
import com.kalpi.prochat.data.repository.RealChatRepository
import com.kalpi.prochat.data.repository.RealChatRoomRepository
import com.kalpi.prochat.navigation.AppNavGraph

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

                    // Get the initial room ID from a deep link, if it exists
                    val initialRoomId: String? = intent?.data?.lastPathSegment
                    Log.d("MainActivity", "Initial room from deep link: $initialRoomId")

                    // Repositories are now instantiated once and passed to the NavGraph
                    val chatRoomRepository = RealChatRoomRepository(firestore)
                    val chatRepository = RealChatRepository(firestore, chatRoomRepository)
                    val userRepository = RealUserRepository(firestore)
                    val presenceRepository = PresenceRepository(firestore)

                    // Navigation setup
                    val navController = rememberNavController()

                    // Handle AppState logic based on the current navigation destination
                    DisposableEffect(navController) {
                        val listener = NavController.OnDestinationChangedListener { _, destination, arguments ->
                            val currentRoomId = arguments?.getString("roomId")
                            AppState.currentChatRoomId = currentRoomId
                            Log.d("MainActivity", "Current chat room ID: ${AppState.currentChatRoomId}")
                        }
                        navController.addOnDestinationChangedListener(listener)
                        onDispose {
                            navController.removeOnDestinationChangedListener(listener)
                        }
                    }

                    // Call the centralized navigation graph
                    AppNavGraph(
                        navController = navController,
                        userId = uniqueUserId,
                        chatRoomRepository = chatRoomRepository,
                        chatRepository = chatRepository,
                        userRepository = userRepository,
                        presenceRepository = presenceRepository,
                        initialRoomId = initialRoomId // Pass the deep-link ID to the graph
                    )
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
