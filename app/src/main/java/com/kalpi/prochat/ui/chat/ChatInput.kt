package com.kalpi.prochat.ui.chat

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

/**
 * Composable for the chat input area, including a text field and a send button.
 *
 * @param chatViewModel The ViewModel to handle sending messages.
 * @param modifier Modifier for this composable.
 */
@Composable
fun ChatInput(
    chatViewModel: ChatViewModel, // Pass the ViewModel
    modifier: Modifier = Modifier
) {
    // Local state for the TextFieldValue, managed within ChatInput
    var textState by remember { mutableStateOf(TextFieldValue("")) }

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
                onValueChange = { textState = it },
                placeholder = { Text("Type a message...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors( // Using TextFieldDefaults.colors for M3
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent, // Optional: if you ever disable it
                    // cursorColor = MaterialTheme.colorScheme.primary // Optional: customize cursor
                ),
                maxLines = 5 // Optional: allow multi-line input up to a point
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
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Send message"
                    // tint = if (textState.text.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled) // M2 style
                )
            }
        }
    }
}
