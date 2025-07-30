package com.kalpi.prochat.ui.chat

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults // Import this
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions // For imeAction
import androidx.compose.ui.text.input.ImeAction // For imeAction
import androidx.compose.material3.Text // For character counter (optional)
import androidx.compose.foundation.layout.Column // If adding character counter
import androidx.compose.foundation.text.KeyboardActions


/**
 * Composable for the chat input area, including a text field and a send button.
 *
 * @param chatViewModel The ViewModel to handle sending messages.
 * @param modifier Modifier for this composable.
 */

private const val MAX_MESSAGE_LENGTH = 300
@Composable
fun ChatInput(
    chatViewModel: ChatViewModel, // Pass the ViewModel
    modifier: Modifier = Modifier
) {
    // Local state for the TextFieldValue, managed within ChatInput
    var textState by remember { mutableStateOf(TextFieldValue("")) }

    val isEnabled = textState.text.isNotBlank() && textState.text.length <= MAX_MESSAGE_LENGTH

    // Determine colors based on the enabled state (if you're using this logic from our previous discussion)
    val buttonContainerColor = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val buttonContentColor = if (isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

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
                maxLines = 5, // Optional: allow multi-line input up to a point
                // Makes the keyboard's send button also trigger send
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
                enabled = isEnabled // Disable button if text is blank


            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Send message"
                    // tint = if (textState.text.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled) // M2 style
                )
            }
        }

        // Optional: Character counter
//        Text(
//            text = "${textState.text.length} / $MAX_MESSAGE_LENGTH",
//            style = MaterialTheme.typography.labelSmall,
//            color = if (textState.text.length > MAX_MESSAGE_LENGTH) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
//            modifier = Modifier
//                .align(Alignment.End)
//                .padding(top = 4.dp, end = 8.dp) // Adjust padding as needed
//        )

    }
}
