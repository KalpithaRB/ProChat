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
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import com.kalpi.prochat.ui.presentations.viewmodel.ChatViewModel



/**
 * Composable for the chat input area, including a text field and a send button.
 *
 * @param chatViewModel The ViewModel to handle sending messages.
 * @param modifier Modifier for this composable.
 */

// Define your file size limit (5MB in bytes)
private const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB


private const val MAX_MESSAGE_LENGTH = 300

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInput(
    onSendMessage: (String) -> Unit,
    onSendImageMessage: (Uri) -> Unit,
    onSendFileMessage: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    var textState by remember { mutableStateOf(TextFieldValue("")) }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            Log.d("ChatInput", "Image URI selected: $selectedUri")
            val fileSize = getFileSizeFromUri(context, selectedUri) // Corrected placement
            Log.d("ChatInput", "Selected image URI: $selectedUri, Size: $fileSize bytes")

            if (fileSize == -1L) {
                Log.w("ChatInput", "Could not determine file size for $selectedUri.")
                Toast.makeText(context, "Could not determine image size.", Toast.LENGTH_SHORT).show()
            } else if (fileSize > MAX_FILE_SIZE_BYTES) {
                Log.w("ChatInput", "Image $selectedUri is too large: $fileSize bytes. Max allowed: $MAX_FILE_SIZE_BYTES bytes.")
                Toast.makeText(context, "Image is too large (max 5MB). Please select a smaller one.", Toast.LENGTH_LONG).show()
            } else {
                Log.d("ChatInput", "Image size OK. Calling ViewModel to prepare and send.")
                onSendImageMessage(selectedUri)
            }
        } ?: run {
            Log.d("ChatInput", "No image selected or image picker cancelled.")
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            Log.d("ChatInput", "File URI selected: $selectedUri")
            val fileSize = getFileSizeFromUri(context, selectedUri)
            Log.d("ChatInput", "Selected file URI: $selectedUri, Size: $fileSize bytes")

            if (fileSize == -1L) {
                Log.w("ChatInput", "Could not determine file size for $selectedUri.")
                Toast.makeText(context, "Could not determine file size.", Toast.LENGTH_SHORT).show()
            } else if (fileSize > MAX_FILE_SIZE_BYTES) {
                Log.w("ChatInput", "File $selectedUri is too large: $fileSize bytes. Max allowed: $MAX_FILE_SIZE_BYTES bytes.")
                Toast.makeText(context, "File is too large (max 5MB). Please select a smaller one.", Toast.LENGTH_LONG).show()
            } else {
                Log.d("ChatInput", "File size OK. Calling ViewModel to prepare and send.")
                onSendFileMessage(selectedUri)
            }
        } ?: run {
            Log.d("ChatInput", "No file selected or file picker cancelled.")
        }
    }

    val isSendEnabled = textState.text.isNotBlank() && textState.text.length <= MAX_MESSAGE_LENGTH

    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically // Align items vertically in the center of the Row
        ) {
            // Image Picker Button
            IconButton(onClick = {
                imagePickerLauncher.launch("image/*")
            }) {
                Icon(
                    imageVector = Icons.Filled.Photo,
                    contentDescription = "Pick Image",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = {
                filePickerLauncher.launch("*/*")
            }) {
                Icon(
                    imageVector = Icons.Filled.Attachment,
                    contentDescription = "Attach File",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp)) // Add some space between icon and text field

            // Column for TextField and Character Counter
            Column(
                modifier = Modifier.weight(1f) // TextField and counter will take available space
            ) {
                OutlinedTextField(
                    value = textState,
                    onValueChange = {
                        // Only update textState if the new text is within the limit or being deleted
                        if (it.text.length <= MAX_MESSAGE_LENGTH) {
                            textState = it
                        } else if (it.text.length < textState.text.length) {
                            // Allow deletion even if over limit (though it shouldn't get there with the check)
                            textState = it
                        }
                    },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.fillMaxWidth(), // TextField takes full width of the Column
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Send // Show send button on keyboard
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (isSendEnabled) {
                                onSendMessage(textState.text)
                                textState = TextFieldValue("")
                            }
                        }
                    )
                )
                // Character counter
                Text(
                    text = "${textState.text.length} / $MAX_MESSAGE_LENGTH",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (textState.text.length > MAX_MESSAGE_LENGTH) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp, end = 8.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Send Button
            IconButton(
                onClick = {
                    if (isSendEnabled) {
                        onSendMessage(textState.text)
                        textState = TextFieldValue("")
                    }
                },
                enabled = isSendEnabled
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message",
                    tint = if (isSendEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) // Standard disabled tint
                )
            }
        }
    }
}
