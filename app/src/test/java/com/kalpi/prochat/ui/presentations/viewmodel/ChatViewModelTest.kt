package com.kalpi.prochat.ui.presentations.viewmodel


import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import com.kalpi.prochat.data.model.ChatMessage
import com.kalpi.prochat.data.model.MessageStatus
import com.kalpi.prochat.data.model.MessageType
import com.kalpi.prochat.data.repository.ChatRepository
import com.kalpi.prochat.utils.NetworkStatusObserver
import com.kalpi.prochat.ui.presentations.viewmodel.ChatViewModel
import com.kalpi.prochat.ui.chat.ChatUiState
import com.kalpi.prochat.data.ChatItem
import com.kalpi.prochat.utils.MainDispatcherRule
import com.kalpi.prochat.data.repository.FakeChatRepository // Your FakeChatRepository
import com.kalpi.prochat.data.repository.FakeChatRoomRepository // Your FakeChatRoomRepository
import app.cash.turbine.test
import io.mockk.mockk
import io.mockk.coVerify
import io.mockk.every
import io.mockk.spyk
import io.mockk.mockk
import io.mockk.mockkStatic
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
//import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // SUT (System Under Test)
    private lateinit var viewModel: ChatViewModel

    // Fakes and Mocks for dependencies
    private lateinit var fakeChatRepository: FakeChatRepository
    private lateinit var fakeChatRoomRepository: FakeChatRoomRepository
    private lateinit var mockNetworkStatusObserver: NetworkStatusObserver
    private lateinit var mockApplication: Application
    private lateinit var mockContext: Context

    // Test data
    private val testRoomId = "test_room_123"
    private val testUserId = "test_user_456"


    @Before
    fun setup() {
        // Initialize mocks and fakes
        mockApplication = mockk(relaxed = true)

        // Mock the ConnectivityManager
        val mockConnectivityManager = mockk<ConnectivityManager>(relaxed = true)

        // Set up the mock Application to return the mock ConnectivityManager when getSystemService is called
        every { mockApplication.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager

        // We don't need a separate `mockContext` variable, as the `Application` will provide it.
        // However, if your ViewModel calls `application.applicationContext.getSystemService()`,
        // you would need to mock the context's getSystemService as well.
        // Let's assume for now the ViewModel calls getSystemService on the Application directly.

        // Use a Spyk on the FakeChatRepository to still be able to verify calls
        fakeChatRepository = spyk(FakeChatRepository())
        fakeChatRoomRepository = spyk(FakeChatRoomRepository())

        // Initialize the ViewModel with the mocked application
        viewModel = ChatViewModel(
            application = mockApplication, // The ViewModel receives the mocked Application
            chatRepository = fakeChatRepository,
            chatRoomRepository = fakeChatRoomRepository,
            currentRoomId = testRoomId,
            currentUserId = testUserId
        )
    }

    @Test
    fun sendMessage_shouldAddMessageWithSendingStatus_thenUpdateToSent() = runTest {
        val testMessageText = "Hello from test!"
        val initialMessages = listOf(
            ChatMessage(id = "1", senderId = "other_user", text = "Hi", roomId = testRoomId)
        )
        fakeChatRepository.addMessage(initialMessages[0])

        viewModel.uiState.test {
            // Await the initial state with the first message
            var initialState = awaitItem() as ChatUiState.Content
            assertEquals(1, initialState.messages.filterIsInstance<ChatItem.Message>().size)

            // Send a new message
            viewModel.sendMessage(testMessageText)

            // Wait for the message with SENDING status to be emitted
            val sendingState = awaitItem() as ChatUiState.Content
            val sendingMessage = sendingState.messages.filterIsInstance<ChatItem.Message>().last().message
            assertEquals(testMessageText, sendingMessage.text)
            assertEquals(MessageStatus.SENDING, sendingMessage.status)
            assertEquals(testUserId, sendingMessage.senderId)

            // Let the coroutine run and simulate the network call completion
            advanceUntilIdle()

            // Wait for the final state with the message status updated to SENT
            val sentState = awaitItem() as ChatUiState.Content
            val sentMessage = sentState.messages.filterIsInstance<ChatItem.Message>().last().message
            assertEquals(MessageStatus.SENT, sentMessage.status)

            // Verify that the repository's sendMessage was called
            coVerify(exactly = 1) { fakeChatRepository.sendMessage(eq(testRoomId), any()) }
        }
    }

    @Test
    fun retryFailedMessages_shouldResendMessagesWithFailedStatus() = runTest {
        val failedMessage = ChatMessage(
            id = "failed_msg_1",
            senderId = testUserId,
            text = "Failed message",
            clientTimestamp = System.currentTimeMillis(),
            messageType = MessageType.TEXT,
            status = MessageStatus.FAILED,
            roomId = testRoomId
        )
        fakeChatRepository.addMessage(failedMessage)

        viewModel.uiState.test {
            // Await the initial state which contains the failed message
            val initialState = awaitItem() as ChatUiState.Content
            assertEquals(MessageStatus.FAILED, initialState.messages.filterIsInstance<ChatItem.Message>().first().message.status)

            viewModel.retryFailedMessages()

            // After retry, the message should briefly show SENDING
            val retryingState = awaitItem() as ChatUiState.Content
            assertEquals(MessageStatus.SENDING, retryingState.messages.filterIsInstance<ChatItem.Message>().first().message.status)

            // Let the coroutine finish the simulated network call
            advanceUntilIdle()

            // Wait for the final state where the message is SENT
            val sentState = awaitItem() as ChatUiState.Content
            assertEquals(MessageStatus.SENT, sentState.messages.filterIsInstance<ChatItem.Message>().first().message.status)

            // Verify that getFailedMessages and sendMessage were called
            coVerify(exactly = 1) { fakeChatRepository.getFailedMessages(eq(testRoomId)) }
            coVerify(exactly = 1) { fakeChatRepository.sendMessage(eq(testRoomId), any()) }
        }
    }

    @Test
    fun prepareAndSendImageMessage_shouldUploadFileAndSendFinalMessage() = runTest {
        val dummyImageUri: Uri = mockk()
        every { dummyImageUri.toString() } returns "file:///local/path/to/image.jpg"

        viewModel.uiState.test {
            val initialState = awaitItem() as ChatUiState.Loading
            assertEquals(ChatUiState.Loading, initialState)

            // Send the image
            viewModel.prepareAndSendImageMessage(dummyImageUri)

            // Let the upload progress updates occur
            viewModel.uploadProgress.test {
                // Initial progress (0%)
                assertEquals(0, awaitItem().values.first())

                // Advance time to allow the fake repository to simulate progress
                advanceTimeBy(10)
                // Halfway progress (50%)
                assertEquals(50, awaitItem().values.first())

                // Let the upload complete
                advanceUntilIdle()

                // Final progress (100%)
                assertEquals(100, awaitItem().values.first())
            }

            // Check the uiState for the initial message with the local URI
            val sendingState = awaitItem() as ChatUiState.Content
            val sendingMessage = sendingState.messages.filterIsInstance<ChatItem.Message>().first().message
            assertEquals(MessageStatus.SENDING, sendingMessage.status)
            assertEquals(dummyImageUri.toString(), sendingMessage.imageUrl)

            // After the upload is complete, the final message with the Cloudinary URL is sent.
            // The flow will emit a new state.
            val finalState = awaitItem() as ChatUiState.Content
            val finalMessage = finalState.messages.filterIsInstance<ChatItem.Message>().first().message
            assertEquals(MessageStatus.SENT, finalMessage.status)
            assertTrue(finalMessage.imageUrl!!.startsWith("https://fakeurl.com"))

            // Verify that the repository functions were called
            coVerify(exactly = 1) { fakeChatRepository.uploadFileToCloudinaryAndGetUrl(any(), any(), any(), any()) }
            coVerify(exactly = 1) {
                fakeChatRepository.sendMessage(
                    eq(testRoomId),
                    // Use match with a lambda to create a custom matcher
                    match { message ->
                        message.imageUrl != dummyImageUri.toString() && message.status == MessageStatus.SENT
                    }
                )
            }
        }
    }

    @Test
    fun markLastMessageAsRead_shouldUpdateStatusOfLastIncomingMessage() = runTest {
        val incomingMessage = ChatMessage(
            id = "msg_1",
            senderId = "other_user",
            text = "Test read status",
            clientTimestamp = 100L,
            status = MessageStatus.DELIVERED,
            roomId = testRoomId
        )
        val outgoingMessage = ChatMessage(
            id = "msg_2",
            senderId = testUserId,
            text = "Okay",
            clientTimestamp = 200L,
            status = MessageStatus.SENT,
            roomId = testRoomId
        )
        fakeChatRepository.addMessage(incomingMessage)
        fakeChatRepository.addMessage(outgoingMessage)

        viewModel.uiState.test {
            // Await initial state with two messages
            val initialState = awaitItem() as ChatUiState.Content
            assertEquals(2, initialState.messages.filterIsInstance<ChatItem.Message>().size)
            assertEquals(MessageStatus.DELIVERED, initialState.messages.filterIsInstance<ChatItem.Message>().first().message.status)

            // Mark the last incoming message as read
            viewModel.markLastMessageAsRead()

            // Let the coroutine finish
            advanceUntilIdle()

            // Await the new state and verify the change
            val updatedState = awaitItem() as ChatUiState.Content
            val readMessage = updatedState.messages.filterIsInstance<ChatItem.Message>().first().message
            assertEquals(MessageStatus.READ, readMessage.status)

            // Verify that the repository's updateMessageStatus was called
            coVerify(exactly = 1) { fakeChatRepository.updateMessageStatus(eq(testRoomId), eq("msg_1"), eq(MessageStatus.READ)) }
        }
    }

    @Test
    fun deleteChatroom_shouldCallRepositoryAndEmitSuccessOnSharedFlow() = runTest {
        // Prepare the SharedFlow to be tested
        viewModel.deletionSuccess.test {
            // Set up fake repository to return success
            // This line can be removed if you want the default behavior
            // fakeChatRoomRepository.deleteChatroomResult = Result.success(Unit)

            // Trigger the deletion
            viewModel.deleteChatroom()

            // Wait for the coroutine to complete
            advanceUntilIdle()

            // Verify that the SharedFlow emitted a Unit
            assertNotNull(awaitItem())
        }

        // Verify that the repository method was called with the correct parameters
        coVerify(exactly = 1) {
            fakeChatRoomRepository.deleteChatroomForUser(eq(testRoomId), eq(testUserId))
        }
    }

    // You can write similar tests for:
    // - `prepareAndSendFileMessage` by adapting the `prepareAndSendImageMessage` test.
    // - `prepareAndSendAudioMessage` by adapting the `prepareAndSendImageMessage` test.
    // - `processDeliveredStatus` by having the fake repo emit a SENT message from another user.
}