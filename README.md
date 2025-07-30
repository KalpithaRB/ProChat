# ProChat - Android Chat Application

## 📌 Overview

**ProChat** is a modern Android chat application built using **Kotlin** and **Jetpack Compose**, following the **MVVM (Model-View-ViewModel)** architecture. This document outlines the Day 1 deliverables, focused on building the **chat screen UI** using dummy data and managing screen state effectively with Compose and StateFlow.

---

## ✨ Features Implemented (Day 1 & Day 2)

- **Chat Screen UI**
  - Scrollable message list using `LazyColumn`
  - Input field for message typing
  - Send button (clears input on click)

- **Message Display**
  - Left/right aligned message bubbles (based on sender)
  - Message timestamps displayed clearly
  - System messages (e.g., "User has joined") with distinct styling
  - Date separators like “Today”, “Yesterday”, and formatted dates

- **UI State Management**
  - `Loading`, `Content`, and `Error` states using sealed classes
  - All UI driven from a single `ViewModel` exposing `StateFlow<ChatUiState>`

- **Dummy Data**
  - Messages are generated locally using hardcoded values
  - Includes a mix of user messages, system messages, and simulated time shifts
 
  
*   **Message Sending:** 
    *   Users can type messages and send them. Messages are stored in Firebase Firestore.
*   **Real-time UI Updates (Local):**
    *   The UI immediately reflects a message being sent (e.g., with a clock icon).
*   **Message Status Indicators:**
    *   **Sending:** A clock icon indicates the message is currently being sent.
    *   **Sent:** A tick mark confirms the message has been successfully saved to the server.
    *   **Failed:** A red error icon ("!") appears if a message fails to send (e.g., due to network issues).
*   **Retry Failed Messages:**
    *   Users can tap on a failed message to attempt sending it again.
*   **ViewModel & Repository Pattern:**
    *   Business logic is separated using `ViewModel` for UI state and `Repository` for data operations with Firebase.
*   **Basic Message Bubble Styling:**
    *   Message bubbles are styled to distinguish between the current user's messages and messages from others. 
*   **Chat Input:**
    *   A dedicated input field with a send button that enables/disables based on whether there's text to send.

  ---
 
📌 Firebase Choice: Cloud Firestore

Reasoning:

🔍 Rich Queries: Enables future expansion (search, filtering)

🚀 Scalability: Optimized for large datasets and multi-region deployments

🧱 Structured Data Model: Document-collection mapping suits /chatrooms/{roomId}/messages/{messageId}

📴 Robust Offline Support: Smooth experience during network disruptions

🌍 Multi-region Replication: Improves latency for global users


---

## 🧱 MVVM Architecture Overview

The app follows the **Model-View-ViewModel (MVVM)** pattern to ensure separation of concerns, modularity, and easier testing.

### 1. **Model Layer**

Handles the data and domain logic independently of the UI.

* **`ChatMessage.kt`**
  A data class representing each message with properties like `id`, `senderId`, `text`, `timestamp`, and `messageType` (e.g., `USER` or `SYSTEM`).

* **`ChatItem.kt`**
  A sealed interface used to unify chat message items and date separators for display in a `LazyColumn`.

  * `Message`: Wraps a `ChatMessage`.
  * `DateSeparator`: Represents headers like "Today" or "Yesterday" using formatted timestamps.

* **`ChatUiState.kt`**
  A sealed interface for screen states:

  * `Loading`: Shows progress indicator.
  * `Content`: Displays messages.
  * `Error`: Shows an error message.

---

### 2. **View Layer (Jetpack Compose)**

Responsible for rendering the UI and reacting to state changes from the ViewModel.

* **`ChatScreen.kt`**
  Observes `ChatUiState` from the ViewModel and conditionally renders:

  * A loading indicator, list of messages, or error text.
    Uses `Scaffold`, `TopAppBar`, and `LazyColumn` to structure the layout.

* **`MessageBubble()`**
  Renders individual messages with alignment, styling, and timestamp formatting based on `senderId` and `messageType`.

* **`DateSeparatorItem()`**
  Displays date dividers like "Today" or formatted full dates.

* **`ChatInput()`**
  A message input field with a send button, managing local input state and sending callbacks upward.

---

### 3. **ViewModel Layer**

Acts as the state holder and logic handler between Model and View.

* **`ChatViewModel.kt`**

  * Exposes UI state using `StateFlow<ChatUiState>`.
  * On init, loads dummy `ChatMessage` data, sorts them by timestamp, and inserts `DateSeparator`s as needed.
  * Emits the final structured list wrapped in `ChatUiState.Content`.

The ViewModel contains no direct references to UI components, making it testable and lifecycle-aware.

---
## Dummy Data Structure 

### Dummy Data Generation

*   For Day 1, all chat messages are hardcoded as a list of ChatMessage objects within the ChatViewModel's loadDummyMessagesWithSeparators() function.

*   Each ChatMessage is instantiated with:
    *   id: A unique UUID.randomUUID().toString().
    *   senderId: Set to ChatViewModel.CURRENT_USER_ID for messages from the current user, ChatViewModel.OTHER_USER_ID for received messages, or "system" for system notifications.
    *   text: The actual message content.
    *   timestamp: Generated using System.currentTimeMillis() with various offsets to simulate messages sent at different times and on different days, enabling the demonstration of date separators.
    *   messageType: Set to MessageType.USER or MessageType.SYSTEM.
      
---

## 🧩 UI Logic & Components

*   **Message List Rendering (LazyColumn in ChatScreen)**:
    *   The LazyColumn efficiently displays the heterogeneous list of ChatItems obtained from (uiState as ChatUiState.Content).items.
    *   key parameter in items(...):
          Each ChatItem is given a unique key (message.id for messages, separator.timestamp.toString() for separators). This helps Compose identify items correctly during recompositions, especially when items are added, removed, or reordered.
    *   contentType parameter in items(...):
         This provides a hint to LazyColumn about the type of item at a given position ("message" or "separator"). It allows LazyColumn to optimize item reuse, as it will only try to reuse an item if its content type matches.
        
*   **Dynamic Message Bubble Styling (MessageBubble)**:
    *   The alignment of the bubble (and its content) within the row is controlled by checking if message.senderId == ChatViewModel.CURRENT_USER_ID. The bubble is pushed to Alignment.CenterEnd for the current user and Alignment.CenterStart for others.
    *   RoundedCornerShape is dynamically applied to give a "speech bubble" effect, with one corner being less rounded depending on whether the message is sent or received.
    *   Different background colors from MaterialTheme.colorScheme (primaryContainer vs. secondaryContainer) are used for sent vs. received messages.
    *   System messages (MessageType.SYSTEM) receive a distinct styling, typically centered with a different background and text style to stand out from user messages.
      
*   **Timestamp Formatting (MessageBubble)**:
      The Long timestamp from ChatMessage is formatted into a human-readable time (e.g., "09:30 AM") using SimpleDateFormat("hh:mm a", Locale.getDefault()). This formatted time is displayed within each message bubble.

  
*   **Date Separator Logic (ChatViewModel & ChatItem.kt)**:
    *   **Processing in ViewModel**: In ChatViewModel, after fetching or generating the raw ChatMessage list, it's crucial to sort them by timestamp first.
    *   The sorted list is then iterated. A ChatItem.DateSeparator is inserted into the processedChatItems list *before* its corresponding message if the current message's day is different from the day of the previously processed message (or if it's the very first message).
    *   **Helper Functions (ChatItem.kt)**:
        *   isSameDay(timestamp1: Long, timestamp2: Long): Boolean: Compares two timestamps to check if they fall on the same calendar day by comparing their YEAR and DAY_OF_YEAR using Calendar.
        *   formatDateSeparator(timestamp: Long): String: Formats a timestamp into a display string for the separator. It checks if the date is "Today", "Yesterday", or otherwise formats it as "MMMM d, yyyy" (e.g., "August 23, 2024").
     
          
*   **UI State Handling (ChatScreen)**: A when expression on the uiState (collected from the ViewModel) determines what is displayed:
    *   ChatUiState.Loading: A CircularProgressIndicator is shown.
    *   ChatUiState.Content: The LazyColumn with messages and separators is displayed. An additional check for state.items.isEmpty() allows showing a "No messages yet" prompt.
    *   ChatUiState.Error: A Text composable displays the state.errorMessage.
      
---

Known Issues:

🔓 Firestore rules are currently open for development (allow read, write: if true;). These will be secured in upcoming phases.

⚠️ Real-time receiving of messages (addSnapshotListener) will be implemented in Day 3

---

## 🚀 Running the App

### Prerequisites

- Android Studio (Hedgehog / Iguana or newer)
- Kotlin plugin
- Emulator or Android device (with USB debugging)

### Setup Steps
* Clone the repository.
* Ensure you have Android Studio installed.
* **Firebase Setup:**
    *   Create a Firebase project at [https://console.firebase.google.com/](https://console.firebase.google.com/).
    *   Add an Android app to your Firebase project with the package name `<!-- com.kalpi.prochat -->`.
    *   Download the `google-services.json` file from your Firebase project settings and place it in the `app/` directory of this Android Studio project.
    *   In the Firebase console, enable **Firestore Database**. When prompted, create the database in **Native Mode** and choose a region. For initial testing, the default security rules (or temporarily open rules) can be used, as mentioned above.
* Open the project in Android Studio.
* Build and run the app on an emulator or physical device

