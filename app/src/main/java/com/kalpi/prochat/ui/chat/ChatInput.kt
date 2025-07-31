package com.kalpi.prochat.ui.chat

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.automirrored.filled.Send // For M3
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults // Import this
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import android.util.Log // For logging
import com.kalpi.prochat.data.getFileSizeFromUri // Adjust the import path
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction

/**
 * Composable for the chat input area, including a text field and a send button.
 *
 * @param chatViewModel The ViewModel to handle sending messages.
 * @param modifier Modifier for this composable.
 */

// Define your file size limit (5MB in bytes)
private const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB


private const val MAX_MESSAGE_LENGTH = 300

@Composable
fun ChatInput(
    chatViewModel: ChatViewModel, // Pass the ViewModel
    modifier: Modifier = Modifier
) {
    // Local state for the TextFieldValue, managed within ChatInput
    var textState by remember { mutableStateOf(TextFieldValue("")) }
    val context = LocalContext.current // Get context for Toast and getFileSizeFromUri

    // Launcher for picking an image from the device's gallery
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            Log.d("ChatInput", "Image URI selected: $selectedUri")

            // --- START FILE SIZE CHECK ---
            val fileSize = getFileSizeFromUri(context, selectedUri)
            Log.d("ChatInput", "Selected image URI: $selectedUri, Size: $fileSize bytes")

            if (fileSize == -1L) {
                Log.w("ChatInput", "Could not determine file size for $selectedUri.")
                Toast.makeText(context, "Could not determine image size.", Toast.LENGTH_SHORT).show()
                // Optionally, decide if you want to block or allow. Blocking is safer.
            } else if (fileSize > MAX_FILE_SIZE_BYTES) {
                Log.w("ChatInput", "Image $selectedUri is too large: $fileSize bytes. Max allowed: $MAX_FILE_SIZE_BYTES bytes.")
                Toast.makeText(context, "Image is too large (max 5MB). Please select a smaller one.", Toast.LENGTH_LONG).show()
            } else {
                // File size is OK, proceed with sending
                Log.d("ChatInput", "Image size OK. Calling ViewModel to prepare and send.")
                chatViewModel.prepareAndSendImageMessage(selectedUri) // Call ViewModel
            }
            // --- END FILE SIZE CHECK ---

        } ?: run {
            Log.d("ChatInput", "No image selected or image picker cancelled.")
        }
    }
    val isEnabled = textState.text.isNotBlank() && textState.text.length <= MAX_MESSAGE_LENGTH

    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 4.dp // Add some elevation
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image Picker Button
            IconButton(onClick = {
                imagePickerLauncher.launch("image/*") // Launch image picker
            }) {
                Icon(
                    imageVector = Icons.Filled.Photo,
                    contentDescription = "Pick Image",
                    tint = MaterialTheme.colorScheme.primary // Optional tint
                )
            }
            OutlinedTextField(
                value = textState,
                onValueChange = { textState = it },
                placeholder = { Text("Type a message...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    // Using TextFieldDefaults.colors for M3
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent, // Optional: if you ever disable it
                    // cursorColor = MaterialTheme.colorScheme.primary // Optional: customize cursor
                ),
                maxLines = 5 // Optional: allow multi-line input up to a point
            )
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {

            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textState,
                    onValueChange = { // Only update textState if the new text is within the limit
                        if (it.text.length <= MAX_MESSAGE_LENGTH) {
                            textState = it
                        }
                    },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        // Using TextFieldDefaults.colors for M3
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent, // Optional: if you ever disable it
                        // cursorColor = MaterialTheme.colorScheme.primary // Optional: customize cursor
                    ),
                    maxLines = 5,// Optional: allow multi-line input up to a point
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (isEnabled) {
                                chatViewModel.sendMessage(textState.text)
                                textState = TextFieldValue("")
                            }
                        }
                    )
                )

                IconButton(
                    onClick = {
                        val messageText = textState.text
                        if (messageText.isNotBlank()) {
                            chatViewModel.sendMessage(messageText)
                            textState = TextFieldValue("") // Clear input after sending
                        }
                    },
                    enabled = textState.text.isNotBlank() // Disable button if text is blank

            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message"
                    // tint = if (textState.text.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled) // M2 style
                )
            }
            // Optional: Character counter
            Text(
                text = "${textState.text.length} / $MAX_MESSAGE_LENGTH",
                style = MaterialTheme.typography.labelSmall,
                color = if (textState.text.length > MAX_MESSAGE_LENGTH) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp, end = 8.dp) // Adjust padding as needed
            )
        }}
    }}

}
