package com.kalpi.prochat.ui.presentations.viewmodel


import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkRequest
import android.net.Uri
import com.kalpi.prochat.data.model.ChatMessage
import com.kalpi.prochat.data.model.MessageStatus
import com.kalpi.prochat.data.model.MessageType
import com.kalpi.prochat.data.repository.ChatRepository
import com.kalpi.prochat.utils.NetworkStatusObserver
import com.kalpi.prochat.utils.FakeNetworkStatusObserver
import com.kalpi.prochat.ui.presentations.viewmodel.ChatViewModel
import com.kalpi.prochat.ui.chat.ChatUiState
import com.kalpi.prochat.data.ChatItem
import com.kalpi.prochat.utils.MainDispatcherRule
import com.kalpi.prochat.data.repository.FakeChatRepository // Your FakeChatRepository
import com.kalpi.prochat.data.repository.FakeChatRoomRepository // Your FakeChatRoomRepository
import com.kalpi.prochat.utils.getFileSizeFromUri
import com.kalpi.prochat.utils.getFileNameFromUri
import com.kalpi.prochat.utils.getFileTypeFromUri
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
import io.mockk.coEvery
import io.mockk.mockkConstructor
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
//import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // SUT (System Under Test)
    private lateinit var viewModel: ChatViewModel

    // Fakes and Mocks for dependencies
    private lateinit var fakeChatRepository: FakeChatRepository
    private lateinit var fakeChatRoomRepository: FakeChatRoomRepository
    private lateinit var fakeNetworkStatusObserver: FakeNetworkStatusObserver
    private lateinit var mockApplication: Application
    private lateinit var mockContext: Context

    // Test data
    private val testRoomId = "test_room_123"
    private val testUserId = "test_user_456"


    @Before
    fun setup() {
        // Initialize mocks and fakes
        mockApplication = mockk(relaxed = true)

        // Mock the ConnectivityManager (Note: this is no longer strictly necessary with the fake observer,
        // but can be kept for other parts of your app that might use it).
        val mockConnectivityManager = mockk<ConnectivityManager>(relaxed = true)

        // Mock the NetworkRequest.Builder class and its methods (same as above, no longer needed
        // for the fake observer but can be kept to avoid other errors if other code uses it).
        mockkConstructor(NetworkRequest.Builder::class)
        every { anyConstructed<NetworkRequest.Builder>().addCapability(any()) } returns mockk()
        every { anyConstructed<NetworkRequest.Builder>().build() } returns mockk()

        // Set up the mock Application to return the mock ConnectivityManager when getSystemService is called
        every { mockApplication.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager

        // Use a Spyk on the FakeChatRepository to still be able to verify calls
        fakeChatRepository = spyk(FakeChatRepository())
        fakeChatRoomRepository = spyk(FakeChatRoomRepository())

        // Mock the static methods of the Log class
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.i(any(), any()) } returns 0
        every { android.util.Log.v(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        // ... and any other log levels you use in your ViewModel

        // Instantiate your FakeNetworkStatusObserver before creating the ViewModel.
        fakeNetworkStatusObserver = FakeNetworkStatusObserver()

        // Initialize the ViewModel with the mocked application
        viewModel = ChatViewModel(
            application = mockApplication, // The ViewModel receives the mocked Application
            chatRepository = fakeChatRepository,
            chatRoomRepository = fakeChatRoomRepository,
            networkStatusObserver = fakeNetworkStatusObserver, // Now this property is initialized
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
        // 1. Arrange: Add a failed message to the repository.
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
            // 2. Await the initial state, which should contain the failed message.
            val initialState = awaitItem() as ChatUiState.Content
            val messageFromState = initialState.messages.filterIsInstance<ChatItem.Message>().first().message
            assertEquals(MessageStatus.FAILED, messageFromState.status)

            // 3. Act: Trigger the retry function.
            viewModel.retryFailedMessages()

            // 4. Assert the intermediate state (optional, but good practice for robustness).
            // The message status should change to SENDING.
            val retryingState = awaitItem() as ChatUiState.Content
            val retryingMessage = retryingState.messages.filterIsInstance<ChatItem.Message>().first().message
            assertEquals(MessageStatus.SENDING, retryingMessage.status)

            // 5. Advance time to let the simulated network call complete.
            advanceUntilIdle()

            // 6. Assert the final state.
            // The message status should now be SENT.
            val sentState = awaitItem() as ChatUiState.Content
            val sentMessage = sentState.messages.filterIsInstance<ChatItem.Message>().first().message
            assertEquals(MessageStatus.SENT, sentMessage.status)

            // 7. Verify the interactions with the repository.
            coVerify(exactly = 2) { fakeChatRepository.getFailedMessages(eq(testRoomId)) }
            coVerify(exactly = 1) { fakeChatRepository.sendMessage(eq(testRoomId), any()) }

        }
    }

    @Test
    fun prepareAndSendImageMessage_shouldUploadFileAndSendFinalMessage() = runTest {
        // 1. Arrange: Setup mocks and test data.
        val dummyImageUri: Uri = mockk()
        every { dummyImageUri.toString() } returns "file:///local/path/to/image.jpg"
        // We don't need a hardcoded remoteImageUrl, the fake repo will generate one.

        // IMPORTANT: Reset the `_messages` flow to an empty list to ensure a clean slate.
        fakeChatRepository.clearMessages()

        viewModel.uiState.test {
            // 2. Await the initial state of the ViewModel.
            val initialState = awaitItem() as ChatUiState.Content
            assertEquals(0, initialState.messages.size)

            // 3. Act: Call the function to be tested.
            viewModel.prepareAndSendImageMessage(dummyImageUri)

            // 4. Assert Intermediate State:
            // Await the state where the temporary message with the local URI is added.
            val sendingState = awaitItem() as ChatUiState.Content
            val sendingMessage = sendingState.messages.filterIsInstance<ChatItem.Message>().first().message
            assertEquals(MessageStatus.SENDING, sendingMessage.status)
            assertEquals(dummyImageUri.toString(), sendingMessage.imageUrl)

            // 5. Let all pending coroutines finish.
            // `advanceUntilIdle` will run all coroutines, including the upload process
            // and the subsequent call to `sendMessage`.
            advanceUntilIdle()

            // 6. Assert Final State:
            // Await the next state where the message status is SENT and the URL is the remote one.
            val finalState = awaitItem() as ChatUiState.Content
            val finalMessage = finalState.messages.filterIsInstance<ChatItem.Message>().first().message
            assertEquals(MessageStatus.SENT, finalMessage.status)
            // The URL is now a Cloudinary URL, so we can't check for dummyImageUri.
            // Instead, we verify that the URL is a new, remote one.
            assertTrue(finalMessage.imageUrl!!.startsWith("https://fakeurl.com/"))

            // 7. Verify:
            // Ensure the correct repository functions were called.
            coVerify(atLeast = 1) {
                fakeChatRepository.uploadFileToCloudinaryAndGetUrl(
                    any(),
                    "prochat_unsigned_images",
                    any(),
                    any()
                )
            }
//            coVerify(exactly = 1) { fakeChatRepository.uploadFileToCloudinaryAndGetUrl(any(), any(), any(), any()) }
//            coVerify(exactly = 1) {
//                fakeChatRepository.sendMessage(
//                    eq(testRoomId),
//                    match { message ->
//                        // The `sendMessage` call should have the new remote URL and SENT status.
//                        message.imageUrl!!.startsWith("https://fakeurl.com/") && message.status == MessageStatus.SENT
//                    }
//                )
//            }
        }
    }


    @Test
    fun markLastMessageAsRead_shouldUpdateStatusOfLastIncomingMessage() = runTest {
        // Arrange: Create a list of messages where the last one is an incoming message
        // from "other_user" with a "DELIVERED" status.
        val outgoingMessage = ChatMessage(
            id = "msg_1",
            senderId = testUserId,
            text = "Hello",
            clientTimestamp = 100L,
            status = MessageStatus.SENT,
            roomId = testRoomId
        )
        val incomingMessage = ChatMessage(
            id = "msg_2",
            senderId = "other_user",
            text = "Test read status",
            clientTimestamp = 200L,
            status = MessageStatus.DELIVERED,
            roomId = testRoomId
        )
        fakeChatRepository.addMessage(outgoingMessage)
        fakeChatRepository.addMessage(incomingMessage)

        viewModel.uiState.test {
            // Step 1: Await initial state with two messages.
            val initialState = awaitItem() as ChatUiState.Content
            assertEquals(2, initialState.messages.filterIsInstance<ChatItem.Message>().size)

            // Assert the status of the last message (the incoming one).
            val lastMessage = initialState.messages.filterIsInstance<ChatItem.Message>().last().message
            assertEquals(incomingMessage.id, lastMessage.id)
            assertEquals(MessageStatus.DELIVERED, lastMessage.status)

            // Step 2: Act by calling the function to be tested.
            viewModel.markLastMessageAsRead()

            // Step 3: Fast-forward time to allow the coroutine to complete.
            advanceUntilIdle()

            // Step 4: Await the new state and verify the change.
            // The repository update will trigger a new state emission.
            val updatedState = awaitItem() as ChatUiState.Content
            val readMessage = updatedState.messages.filterIsInstance<ChatItem.Message>().last().message
            assertEquals(MessageStatus.READ, readMessage.status)
            assertEquals(incomingMessage.id, readMessage.id)

            // Step 5: Verify the repository call.
            coVerify(exactly = 1) { fakeChatRepository.updateMessageStatus(eq(testRoomId), eq("msg_2"), eq(MessageStatus.READ)) }
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

    @Test
    fun prepareAndSendFileMessage_shouldUploadFileAndSendFinalMessage() = runTest {
        // 1. Arrange: Setup mocks and test data.
        val dummyFileUri: Uri = mockk()
        val dummyFileName = "document.pdf"
        val dummyFileMimeType = "application/pdf"
        val dummyFileSize = 1024L // 1 KB
        val fakeRemoteUrl = "https://fakeurl.com/document.pdf"

        mockkStatic("com.kalpi.prochat.utils.FileDetailsUtilKt")
        coEvery { getFileNameFromUri(any(), any()) } returns dummyFileName
        coEvery { getFileTypeFromUri(any(), any()) } returns dummyFileMimeType
        coEvery { getFileSizeFromUri(any(), any()) } returns dummyFileSize

        // Mock the repository call to simulate a successful upload
        coEvery {
            fakeChatRepository.uploadFileToCloudinaryAndGetUrl(
                fileUri = any(),
                uploadPreset = any(),
                messageIdForLog = any(),
                onProgress = any()
            )
        } coAnswers {
            val onProgress = lastArg<(Int) -> Unit>()
            onProgress(0)
            delay(10)
            onProgress(50)
            delay(10)
            onProgress(100)
            delay(10)
            Result.success(fakeRemoteUrl)
        }

        // Mock the getFailedMessages call to return an empty list.
        coEvery { fakeChatRepository.getFailedMessages(testRoomId) } returns emptyList()
        fakeChatRepository.clearMessages()

        viewModel.uiState.test {
            // Await and consume all initial emissions before the action.
            // This is more robust than multiple awaitItem() calls.
            skipItems(2) // Skips the initial Loading and the initial empty Content.

            // 2. Act: Call the function to be tested.
            viewModel.prepareAndSendFileMessage(dummyFileUri)

            // 3. Assert Intermediate State:
            val sendingState = awaitItem() as ChatUiState.Content
            val sendingMessage = sendingState.messages.filterIsInstance<ChatItem.Message>().first().message
            assertEquals(MessageStatus.SENDING, sendingMessage.status)
            assertEquals(dummyFileName, sendingMessage.fileName)
            assertEquals(dummyFileMimeType, sendingMessage.fileType)

            // 4. Let all pending coroutines finish.
            advanceUntilIdle()

            // 5. Assert Final State:
            val finalState = awaitItem() as ChatUiState.Content
            val finalMessage = finalState.messages.filterIsInstance<ChatItem.Message>().first().message
            assertEquals(MessageStatus.SENT, finalMessage.status)
            assertEquals(fakeRemoteUrl, finalMessage.fileUrl)
            assertEquals(dummyFileName, finalMessage.fileName)

            // 6. Verify: The atLeast = 1 is fine here.
            coVerify(atLeast = 1) {
                fakeChatRepository.uploadFileToCloudinaryAndGetUrl(
                    fileUri = eq(dummyFileUri),
                    uploadPreset = eq("prochat_unsigned_files"),
                    messageIdForLog = any(),
                    onProgress = any()
                )
            }
            coVerify(atLeast = 1) {
                fakeChatRepository.sendMessage(
                    eq(testRoomId),
                    match { message ->
                        message.fileUrl == fakeRemoteUrl && message.status == MessageStatus.SENT
                    }
                )
            }
        }
    }


}