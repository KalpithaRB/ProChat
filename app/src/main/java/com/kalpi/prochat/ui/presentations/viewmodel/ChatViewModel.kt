package com.kalpi.prochat.ui.presentations.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalpi.prochat.data.ChatItem
import com.kalpi.prochat.data.repository.ChatRoomRepository
import com.kalpi.prochat.data.model.ChatMessage
import com.kalpi.prochat.data.model.MessageStatus
import com.kalpi.prochat.data.model.MessageType
import com.kalpi.prochat.data.repository.ChatRepository
import com.kalpi.prochat.utils.getFileNameFromUri
import com.kalpi.prochat.utils.getFileTypeFromUri
import com.kalpi.prochat.utils.getFileSizeFromUri
import com.kalpi.prochat.ui.chat.ChatUiState
import com.kalpi.prochat.utils.NetworkStatusObserver
import com.kalpi.prochat.utils.formatDateSeparator
import com.kalpi.prochat.utils.isSameDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.net.HttpURLConnection

/**
 * ViewModel for the ChatScreen.
 *
 * This ViewModel is responsible for preparing and managing the UI-related data for the ChatScreen.
 * It exposes the chat messages and UI state through [StateFlow].
 *
 * For Day 1, it uses a predefined list of dummy messages.
 * In later stages, it will interact with a data source (e.g., Firebase) to fetch and send messages.
 */

class ChatViewModel (
    application: Application,
    private val chatRepository: ChatRepository,
    private val chatRoomRepository: ChatRoomRepository,
    private val networkStatusObserver: NetworkStatusObserver,
    val currentRoomId: String,
    val currentUserId: String

): AndroidViewModel(application) {
    companion object {
//        const val CURRENT_USER_ID = "currentUser"

        private const val TAG = "ChatViewModel"
        fun getOrCreateUserId(context: Context): String {
            val prefs = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
            var userId = prefs.getString("user_id", null)

            if (userId == null) {
                userId = UUID.randomUUID().toString()
                prefs.edit().putString("user_id", userId).apply()
            }
            return userId
        }
    }

    //private val networkStatusObserver = NetworkStatusObserver(application)


    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    /**
     * Publicly exposed [StateFlow] of the [ChatUiState].
     * The UI (ChatScreen) will observe this flow for updates.
     */
    val uiState: StateFlow<ChatUiState> = chatRepository.listenToMessages(currentRoomId)
        .onEach { messages ->
            // Use onEach to process the list before it's passed to the UI
            processDeliveredStatus(messages)
        }
        .map { messages ->
            processMessagesAndUpdateState(messages)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5000),
            initialValue = ChatUiState.Loading
        )

    private var audioRecorder: MediaRecorder? = null
    private var audioFile: File? = null

    // State to hold upload progress for messages (MessageID -> Progress Percentage)
    private val _uploadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val uploadProgress: StateFlow<Map<String, Int>> = _uploadProgress.asStateFlow()

    init {
        viewModelScope.launch {
            networkStatusObserver.observe().collect { isConnected ->
                if (isConnected) {
                    Log.d(TAG, "Network reconnected. Retrying failed messages...")
                    retryFailedMessages()
                } else {
                    Log.d(TAG, "Network unavailable. Operating in offline mode.")
                }
            }
        }
    }



    // Sending a message
    // --- Message Sending Logic ---
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val newMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderId = currentUserId,
            text = text.trim(),
            clientTimestamp = System.currentTimeMillis(),
            messageType = MessageType.TEXT,
            status = MessageStatus.SENDING,
            roomId = currentRoomId
        )

        viewModelScope.launch {
            chatRepository.sendMessage(currentRoomId, newMessage)
        }
    }

    fun retryFailedMessages() {
        viewModelScope.launch {
            // Use a coroutine scope to perform the retry
            val failedMessages = chatRepository.getFailedMessages(currentRoomId)

            failedMessages.forEach { failedMessage ->
                Log.d(TAG, "Retrying failed message with ID: ${failedMessage.id}")
                chatRepository.sendMessage(currentRoomId, failedMessage.copy(status = MessageStatus.SENDING))
            }
        }
    }

    fun prepareAndSendImageMessage(imageUri: Uri) {
        Log.d("ChatViewModel", "prepareAndSendImageMessage called with URI: $imageUri")

        // 1. Create a temporary ChatMessage object for UI display (optional, but good for UX)
        // This message will show an upload progress indicator.
        val tempImageMessage = ChatMessage(
            id = UUID.randomUUID().toString(), // Client-generated unique ID
            senderId = currentUserId,
            // text = null, // No text for a pure image message initially
            imageUrl = imageUri.toString(), // Store local URI temporarily for local display/placeholder
            clientTimestamp = System.currentTimeMillis(),
            messageType = MessageType.IMAGE,
            status = MessageStatus.SENDING, // Indicate it's in the process of being sent/uploaded
            roomId = currentRoomId
            // We might add an 'uploadProgress' field to ChatMessage later if needed,
            // or manage progress separately. For now, SENDING status can imply progress.
        )

        _uploadProgress.update { currentProgress ->
            currentProgress + (tempImageMessage.id to 0)
        }

        viewModelScope.launch {
            Log.d(TAG, "Calling repository to upload image for message ID: ${tempImageMessage.id}")

            val uploadResult = chatRepository.uploadFileToCloudinaryAndGetUrl(
                fileUri = imageUri,
                uploadPreset = "prochat_unsigned_images",
                messageIdForLog = tempImageMessage.id,
                onProgress = { progressPercentage ->
                    _uploadProgress.update { currentProgress ->
                        currentProgress + (tempImageMessage.id to progressPercentage)
                    }
                }
            )

            if (uploadResult.isSuccess) {
                val cloudinaryUrl = uploadResult.getOrNull()
                if (cloudinaryUrl != null) {
                    Log.d(TAG, "Image upload success. URL: $cloudinaryUrl")
                    val finalImageMessage = tempImageMessage.copy(
                        imageUrl = cloudinaryUrl,
                        status = MessageStatus.SENT
                    )
                    chatRepository.sendMessage(currentRoomId, finalImageMessage)
                } else {
                    Log.e(TAG, "Image upload succeeded but URL was null.")
                }
            } else {
                Log.e(TAG, "Image upload failed: ${uploadResult.exceptionOrNull()?.message}")
            }
        }
    }


    fun prepareAndSendFileMessage(fileUri: Uri) {
        Log.d(TAG, "prepareAndSendFileMessage called with URI: $fileUri")

        viewModelScope.launch {
            val context = getApplication<Application>()

            val fileName = getFileNameFromUri(context, fileUri)
            val fileMimeType = getFileTypeFromUri(context, fileUri)
            val fileSize = getFileSizeFromUri(context, fileUri)

            if (fileName.isNullOrBlank() || fileMimeType.isNullOrBlank()) {
                Log.e(TAG, "Could not determine file details for URI: $fileUri")
                return@launch
            }

            // CORRECTED: The ChatMessage constructor is now fully explicit to avoid ambiguity.
            // This is the source of your first error.
            val messageId = UUID.randomUUID().toString()
            val tempFileMessage = ChatMessage(
                id = messageId,
                senderId = currentUserId,
                text = null,
                imageUrl = null,
                fileUrl = null,
                fileName = fileName,
                fileType = fileMimeType,
                clientTimestamp = System.currentTimeMillis(),
                messageType = MessageType.FILE,
                status = MessageStatus.SENDING,
                roomId = currentRoomId
            )

            // This is the source of your second error.
            // By declaring tempFileMessage above, it is now in scope for the rest of the function.
            _uploadProgress.update { currentProgress ->
                currentProgress + (tempFileMessage.id to 0)
            }

            Log.d(TAG, "Calling repository to upload file for message ID: ${tempFileMessage.id}")

            val uploadPreset = "prochat_unsigned_files"
            val uploadResult = chatRepository.uploadFileToCloudinaryAndGetUrl(
                fileUri = fileUri,
                uploadPreset = uploadPreset,
                messageIdForLog = tempFileMessage.id, // tempFileMessage.id is now in scope
                onProgress = { progressPercentage ->
                    _uploadProgress.update { currentProgress ->
                        currentProgress + (tempFileMessage.id to progressPercentage) // tempFileMessage.id is now in scope
                    }
                }
            )

            uploadResult.onSuccess { fileUrl ->
                Log.d(TAG, "File upload success. URL: $fileUrl")
                val finalFileMessage = tempFileMessage.copy(
                    fileUrl = fileUrl,
                    status = MessageStatus.SENT
                )
                chatRepository.sendMessage(currentRoomId, finalFileMessage)
            }.onFailure { e ->
                Log.e(TAG, "File upload failed: ${e.message}")
                chatRepository.updateMessageStatus(currentRoomId, messageId, MessageStatus.FAILED)
            }
        }
    }



    fun retrySendMessage(failedMessage: ChatMessage) {
        viewModelScope.launch {
            chatRepository.sendMessage(currentRoomId, failedMessage.copy(status = MessageStatus.SENDING))
        }
    }

    // --- Helper function to process messages and update UI state (from Day 1) ---
    private fun processMessagesAndUpdateState(messages: List<ChatMessage>): ChatUiState {
        if (messages.isEmpty()) {
            return ChatUiState.Content(emptyList())
        }
        val sortedMessages = messages.sortedBy { it.clientTimestamp }
        val chatItems = mutableListOf<ChatItem>()
        var lastDateHeaderTimestamp: Long? = null

        sortedMessages.forEach { message ->
            if (lastDateHeaderTimestamp == null || !isSameDay(
                    message.clientTimestamp,
                    lastDateHeaderTimestamp!!
                )
            ) {
                chatItems.add(ChatItem.DateSeparator(formatDateSeparator(message.clientTimestamp), message.clientTimestamp))
                lastDateHeaderTimestamp = message.clientTimestamp
            }
            chatItems.add(ChatItem.Message(message))
        }
        return ChatUiState.Content(chatItems)
    }

    fun markMessagesAsRead(messageIds: List<String>) {
        viewModelScope.launch {
            messageIds.forEach { messageId ->
                chatRepository.updateMessageStatus(
                    currentRoomId,
                    messageId,
                    MessageStatus.READ
                )
            }
        }
    }

    // NEW: Add a SharedFlow to emit the URI for the file to the UI
    private val _exportFileUri = MutableSharedFlow<Uri>()
    val exportFileUri: SharedFlow<Uri> = _exportFileUri

    fun onExportChatClicked() {
        val currentMessages = (uiState.value as? ChatUiState.Content)?.messages
        if (currentMessages.isNullOrEmpty()) {
            // You can emit an error or a toast message here
            return
        }

        // Launch a coroutine to handle file I/O
        viewModelScope.launch(Dispatchers.IO) {
            val fileName = "chat_export_${currentRoomId}.txt"
            val fileContent = createChatTextContent(currentMessages)

            // CORRECTED: Use getApplication<Application>() to get the context
            val file = File(getApplication<Application>().cacheDir, fileName)

            try {
                // CORRECTED: The use block now works correctly
                FileOutputStream(file).use {
                    it.write(fileContent.toByteArray())
                }

                // CORRECTED: Use getApplication<Application>() to get the context
                val uri = FileProvider.getUriForFile(
                    getApplication(),
                    "${getApplication<Application>().packageName}.fileprovider",
                    file
                )

                // Emit the URI to be handled by the UI
                _exportFileUri.emit(uri)

            } catch (e: Exception) {
                // Handle file creation or I/O error
                Log.e("ChatViewModel", "Error exporting chat", e)
            }
        }
    }

    private fun createChatTextContent(messages: List<ChatItem>): String {
        val stringBuilder = StringBuilder()
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        messages.forEach { chatItem ->
            if (chatItem is ChatItem.Message) {
                val message = chatItem.message
                val timestamp = timeFormat.format(message.clientTimestamp)
                val senderId = message.senderId // You might want to get the sender's name
                val content = when(message.messageType) {
                    MessageType.TEXT -> message.text ?: ""
                    MessageType.IMAGE -> "[Image] ${message.imageUrl ?: ""}"
                    else -> message.text ?: ""
                }
                stringBuilder.append("[$timestamp] $senderId: $content\n")
            }
        }
        return stringBuilder.toString()
    }

    private val _deletionSuccess = MutableSharedFlow<Unit>()
    val deletionSuccess: SharedFlow<Unit> = _deletionSuccess

    fun deleteChatroom() {
        viewModelScope.launch {
            val result = chatRoomRepository.deleteChatroomForUser(currentRoomId, currentUserId)
            if (result.isSuccess) {
                _deletionSuccess.emit(Unit) // Signal the UI to navigate back
            } else {
                Log.e(TAG, "Failed to delete chatroom: ${result.exceptionOrNull()?.message}")
                // TODO: You might want to show a toast or a snackbar with the error
            }
        }
    }

    // A function to handle ZIP export
    fun onExportChatAsZipClicked() {
        val currentMessages = (uiState.value as? ChatUiState.Content)?.messages
        if (currentMessages.isNullOrEmpty()) {
            // You can emit an error or a toast message here
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val appCacheDir = getApplication<Application>().cacheDir
                val zipFileName = "chat_export_${currentRoomId}.zip"
                val zipFile = File(appCacheDir, zipFileName)

                // Prepare a text file with chat content
                val textFileName = "chat_transcript.txt"
                val textFile = File(appCacheDir, textFileName)
                val fileContent = createChatTextContent(currentMessages)
                FileOutputStream(textFile).use { it.write(fileContent.toByteArray()) }

                // Download all image files
                val imageFiles = mutableListOf<File>()
                for (chatItem in currentMessages) {
                    if (chatItem is ChatItem.Message && chatItem.message.messageType == MessageType.IMAGE) {
                        val imageUrl = chatItem.message.imageUrl
                        if (!imageUrl.isNullOrBlank()) {
                            val imageFileName = imageUrl.substringAfterLast("/")
                            val imageFile = File(appCacheDir, imageFileName)
                            downloadImageToFile(imageUrl, imageFile)
                            imageFiles.add(imageFile)
                        }
                    }
                }

                // Zip everything together
                zipFiles(zipFile, textFile, imageFiles)

                //Clean up temporary files
                textFile.delete()
                imageFiles.forEach { it.delete() }

                // Emit the ZIP file URI for sharing
                val uri = FileProvider.getUriForFile(
                    getApplication(),
                    "${getApplication<Application>().packageName}.fileprovider",
                    zipFile
                )
                _exportFileUri.emit(uri)

            } catch (e: Exception) {
                Log.e(TAG, "Error exporting chat as ZIP", e)
            }
        }
    }

    // Helper function to download an image from a URL
    private fun downloadImageToFile(url: String, file: File) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connect()
            connection.inputStream.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download image from $url", e)
            // You might want to handle this gracefully, e.g., save a placeholder file
        }
    }

    // Helper function to create the ZIP file
    private fun zipFiles(zipFile: File, textFile: File, imageFiles: List<File>) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            // Add the text file to the zip
            FileInputStream(textFile).use { fileIn ->
                val zipEntry = ZipEntry(textFile.name)
                zipOut.putNextEntry(zipEntry)
                fileIn.copyTo(zipOut)
            }

            // Add each image file to the zip
            for (imageFile in imageFiles) {
                FileInputStream(imageFile).use { fileIn ->
                    val zipEntry = ZipEntry(imageFile.name)
                    zipOut.putNextEntry(zipEntry)
                    fileIn.copyTo(zipOut)
                }
            }
        }
    }

    fun markLastMessageAsRead() {
        viewModelScope.launch {
            // Find the last message in the current list
            val messages = (uiState.value as? ChatUiState.Content)
                ?.messages
                ?.filterIsInstance<ChatItem.Message>()
                ?.map { it.message }
                ?.sortedByDescending { it.clientTimestamp }

            val lastMessage = messages?.firstOrNull()

            if (lastMessage != null && lastMessage.senderId != currentUserId && lastMessage.status != MessageStatus.READ) {
                // Mark the last message as READ if it's from another user and not already read
                chatRepository.updateMessageStatus(
                    currentRoomId,
                    lastMessage.id,
                    MessageStatus.READ
                )
            }
        }
    }

    private fun processDeliveredStatus(messages: List<ChatMessage>) {
        messages.forEach { message ->
            // Check if the message is from another user and its status is SENT
            if (message.senderId != currentUserId && message.status == MessageStatus.SENT) {
                viewModelScope.launch {
                    chatRepository.updateMessageStatus(
                        currentRoomId,
                        message.id,
                        MessageStatus.DELIVERED
                    )
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun startAudioRecording() {
        val timestamp = System.currentTimeMillis()
        val appCacheDir = getApplication<Application>().cacheDir


        audioFile = File(appCacheDir, "audio_message_$timestamp.3gp")

        audioRecorder = MediaRecorder(getApplication()).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(audioFile?.absolutePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
                start()
                Log.d(TAG, "Audio recording started.")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to prepare and start audio recording", e)
            }
        }
    }

    fun stopAudioRecording() {
        audioRecorder?.apply {
            try {
                stop()
                release()
                Log.d(TAG, "Audio recording stopped and released.")
                // Upload the recorded file
                audioFile?.let { file ->
                    prepareAndSendAudioMessage(Uri.fromFile(file))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop and release audio recorder", e)
                // Clean up the file if an error occurred
                audioFile?.delete()
            }
        }
        audioRecorder = null
        audioFile = null
    }

    // New function to handle the audio upload and message sending
    fun prepareAndSendAudioMessage(audioUri: Uri) {
        Log.d(TAG, "prepareAndSendAudioMessage called with URI: $audioUri")

        viewModelScope.launch {
            val messageId = UUID.randomUUID().toString()
            val fileName = "audio_message_${System.currentTimeMillis()}.3gp" // Use a dynamic name

            val tempAudioMessage = ChatMessage(
                id = messageId,
                senderId = currentUserId,
                fileName = fileName,
                fileType = "audio/3gpp", // Common format for mobile recordings
                clientTimestamp = System.currentTimeMillis(),
                messageType = MessageType.AUDIO,
                status = MessageStatus.SENDING,
                roomId = currentRoomId
            )
            chatRepository.sendMessage(currentRoomId, tempAudioMessage)


            val uploadPreset = "prochat_unsigned_audios"
            val uploadResult = chatRepository.uploadFileToCloudinaryAndGetUrl(
                fileUri = audioUri,
                uploadPreset = uploadPreset,
                messageIdForLog = messageId,
                onProgress = { progressPercentage ->
                    _uploadProgress.update { currentProgress ->
                        currentProgress + (tempAudioMessage.id to progressPercentage)
                    }
                }
            )

            uploadResult.onSuccess { audioUrl ->
                Log.d(TAG, "Audio upload success. URL: $audioUrl")
                val finalAudioMessage = tempAudioMessage.copy(
                    fileUrl = audioUrl,
                    status = MessageStatus.SENT
                )
                chatRepository.sendMessage(currentRoomId, finalAudioMessage)
            }.onFailure { e ->
                Log.e(TAG, "Audio upload failed: ${e.message}")
                chatRepository.updateMessageStatus(currentRoomId, messageId, MessageStatus.FAILED)
            }
        }
    }

}