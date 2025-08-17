package com.kalpi.prochat.navigation


import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kalpi.prochat.data.repository.ChatRepository
import com.kalpi.prochat.data.repository.ChatRoomRepository
import com.kalpi.prochat.data.repository.UserRepository
import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.LaunchedEffect
import com.kalpi.prochat.data.repository.PresenceRepository
import com.kalpi.prochat.ui.presentations.screens.ChatRoomListScreen
import com.kalpi.prochat.ui.presentations.screens.ChatScreen
import com.kalpi.prochat.ui.presentations.screens.MemberManagementScreen
import com.kalpi.prochat.ui.presentations.viewmodel.ChatRoomListViewModel
import com.kalpi.prochat.ui.presentations.viewmodel.ChatRoomListViewModelFactory
import com.kalpi.prochat.ui.presentations.viewmodel.ChatViewModel
import com.kalpi.prochat.ui.presentations.viewmodel.ChatViewModelFactory
import com.kalpi.prochat.ui.presentations.viewmodel.MemberManagementViewModel
import com.kalpi.prochat.ui.presentations.viewmodel.MemberManagementViewModelFactory
import kotlinx.coroutines.delay

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    userId: String,
    chatRoomRepository: ChatRoomRepository,
    chatRepository: ChatRepository,
    userRepository: UserRepository,
    presenceRepository: PresenceRepository,
    initialRoomId: String?
) {
    val startDestination = if (initialRoomId != null) {
        "chatScreen/{roomId}/{roomName}"
    } else {
        "chatRoomList"
    }
    LaunchedEffect(Unit) {
        while (true) {
            presenceRepository.updateLastActive(userId)
            delay(10_000L) // Update every 10 seconds
        }
    }


    NavHost(
        navController = navController,
        startDestination = "chatRoomList"
    ) {
        composable("chatRoomList") {
            val chatRoomListViewModel: ChatRoomListViewModel = viewModel(
                factory = ChatRoomListViewModelFactory(chatRoomRepository, userRepository, userId)
            )
            ChatRoomListScreen(
                chatRoomListViewModel = chatRoomListViewModel,
                onRoomClicked = { roomId, roomName ->
                    navController.navigate("chatScreen/$roomId/$roomName")
                },
                userRepository = userRepository
            )
        }

        composable(
            "chatScreen/{roomId}/{roomName}",
            arguments = listOf(
                navArgument("roomId") {
                    type = NavType.StringType
                    defaultValue = initialRoomId
                    nullable = true },
                navArgument("roomName") {
                    type = NavType.StringType
                    defaultValue = "Chat Pro"
                    nullable = true }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            val roomName = backStackEntry.arguments?.getString("roomName") ?: "Chat"
            val context = LocalContext.current
            val application = context.applicationContext as Application

            val chatViewModel: ChatViewModel = viewModel(
                factory = ChatViewModelFactory(
                    application,
                    chatRepository,
                    chatRoomRepository,
                    roomId,
                    userId
                )
            )

            ChatScreen(
                chatViewModel = chatViewModel,
                roomName = roomName,
                onBackClicked = { navController.popBackStack() },
                onDeleteSuccess = { navController.popBackStack() },
                onNavigateToMemberManagement = { navRoomId, navUserId ->
                    navController.navigate("memberManagement/$navRoomId/$navUserId")
                }
            )
        }

        composable(
            "memberManagement/{roomId}/{userId}",
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            val userId = backStackEntry.arguments?.getString("userId") ?: ""

            val viewModel: MemberManagementViewModel = viewModel(
                factory = MemberManagementViewModelFactory(
                    chatRoomRepository,
                    roomId,
                    userId
                )
            )

            MemberManagementScreen(
                viewModel = viewModel,
                onNavigateBack = { message ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("toastMessage", message)
                    navController.popBackStack()
                }
            )
        }
    }
}