package com.kalpi.prochat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kalpi.prochat.ui.chat.ChatScreen
import com.kalpi.prochat.ui.theme.ProChatTheme
import androidx.lifecycle.viewmodel.compose.viewModel // Standard viewModel delegate
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.kalpi.prochat.data.ChatRepository // Your ChatRepository
import com.kalpi.prochat.ui.chat.ChatViewModel // Your ChatViewModel
import com.kalpi.prochat.ui.chat.ChatViewModelFactory // Your ChatViewModelFactory


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProChatTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // 1. Get Firestore instance
                    val firestore = Firebase.firestore

                    // 2. Create ChatRepository instance
                    val chatRepository = ChatRepository(firestore)

                    // 3. Define your currentRoomId (using the constant from ChatRepository for Day 2)
                    val currentRoomId = ChatRepository.DEFAULT_ROOM_ID

                    // 4. Create the ChatViewModelFactory
                    val chatViewModelFactory = ChatViewModelFactory(chatRepository, currentRoomId)

                    // 5. Instantiate ChatViewModel using the factory
                    val chatViewModel: ChatViewModel = viewModel(factory = chatViewModelFactory)

                    // 6. Pass the viewModel to your ChatScreen
                    ChatScreen(chatViewModel = chatViewModel) // Assuming ChatScreen takes chatViewModel as a parameter
                }
            }
        }
    }
}

