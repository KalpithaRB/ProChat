# ProChat - Android Chat Application

## 📌 Overview

ProChat is a modern Android chat application built with Kotlin, Jetpack Compose, and Firebase, designed to demonstrate scalable real-time communication and clean architecture principles. The app integrates essential chat features like text and image messaging, real-time synchronization, and push notifications, with a roadmap toward advanced features like presence and typing indicators.

---

## ✨ Features Implemented 

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
 
- **FCM Setup (Client)**
  - Firebase Cloud Messaging integrated in Android app. Device successfully retrieves FCM token and handles notifications via FirebaseMessagingService.
  - Notifications can be received and displayed on the device from Firebase Console.

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
*   **Image Sharing with Upload Progress:**
    *   Share images within chats.
    *   Visual progress indicator (percentage and circular bar) displayed during image uploads.
*   **File Size Validation:**
    *   Client-side validation to ensure uploaded images do not exceed a 5MB limit, providing immediate feedback to the user.
*   **Message Input with Character Limit:**
    *   Text input field with a character counter and a maximum message length of 300 characters.
  ---
 
📌 Firebase Choice: Cloud Firestore

Reasoning:

🔍 Rich Queries: Enables future expansion (search, filtering)

🚀 Scalability: Optimized for large datasets and multi-region deployments

🧱 Structured Data Model: Document-collection mapping suits /chatrooms/{roomId}/messages/{messageId}

📴 Robust Offline Support: Smooth experience during network disruptions

🌍 Multi-region Replication: Improves latency for global users

---

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

⚠️ Firebase Storage not used due to Blaze Plan requirements

🔁 Cloudinary used for media storage as alternative

🔓 Server Integration Pending: Currently, notifications can only be triggered through the Firebase Console for testing. Full server-side integration (storing tokens and sending notifications dynamically) will complete Phase 2.

🔁 Deep Linking: Tapping a notification does not yet navigate to the relevant chatroom. This will be addressed with server-triggered data payloads.

🔓Read/Unread State: Updating lastReadTimestamp or marking messages as isRead upon entering a chatroom is still in progress.

🔁 Stretch Features: Presence tracking and typing indicators are planned but deferred until core notification and read/unread flows are finalized.

---

📸 Image Upload Flow
Image selected via ActivityResultLauncher

File size validated (Max 5MB)

Uploads to Cloudinary with public_id

Image URL stored in Firestore chat message

Shown in chat using Coil with CircularProgressIndicator during upload

🔁 Replaced Firebase Storage with Cloudinary due to Spark Plan limitations

---

Day 6 – Push Notification + Read Status
Feature Matrix
Feature	Status	Notes
FCM Setup (Client)	✅	Firebase Cloud Messaging integrated in Android app. Device successfully retrieves FCM token and handles notifications via FirebaseMessagingService.
Test Notifications from Firebase Console	✅	Notifications can be received and displayed on the device from Firebase Console.
Server-Triggered Notifications	⚠️	Pending Phase 2. Server needs to store tokens and send targeted notifications using Firebase Admin SDK.
Data Payload Support (roomId, messagePreview, senderId)	⚠️	To be implemented in Phase 2 when server logic is updated.
Deep Link to Chatroom on Notification Tap	⚠️	Pending implementation.
Read/Unread State Update on Screen Entry	⚠️	Will be integrated alongside deep linking and server payload handling.
Presence (/presence/{userId})	❌	Stretch task, not yet implemented.
Typing Indicator (/typing/{roomId}/{userId})	❌

---
How it Works 
How It Works: Push Notification Flow

[Android App] --(Generates FCM Token)--> [Firebase Cloud Messaging (FCM)]

[Server] <--(Receives and stores tokens)-- [Android App]

[Server] --(Sends message with data payload)--> [FCM] --(Delivers notification)--> [Target Device]

[Android App] --(Handles notification via FirebaseMessagingService)--> 
    - Display notification (even in background)
    - Deep link to correct chatroom (planned)
    - Update read/unread state (planned)
    
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

* *Setup Instructions (Specific to Push Notifications)*
Enable Firebase Cloud Messaging (FCM):

Ensure FCM is enabled in your Firebase project.

Add the google-services.json file to your app/ directory (already required for previous features).

Android Dependencies:

`implementation "com.google.firebase:firebase-messaging:<latest_version>"`
Add apply plugin: `'com.google.gms.google-services'` at the bottom of your app-level build.gradle.

Service Implementation:

Create a FirebaseMessagingService to handle token generation and incoming notifications.

Override onNewToken() to retrieve the device's FCM token.

Temporarily, you can send test notifications via the Firebase Console until server-side integration is complete.

---
# Day 2 - Offline Support & Local Cache

---

### What was done

This is a **partial implementation** of a real-time chat application with a focus on **offline-first functionality**. The core features are a robust chat system that functions without an active network connection, a dynamic UI that reflects message status, and a dependency-managed architecture.

The implementation included:
* **Offline-first Architecture**: Messages are immediately saved to a local Room database. The app attempts to sync with Firestore (the remote backend) when a network connection is available.
* **Dynamic Message Status**: The UI now displays a dynamic status for each message, including:
    * **`SENDING`**: A clock icon ⏱️ (indicating the message is waiting for a network connection to upload).
    * **`SENT`**: A single checkmark ✔️ (indicating the message has been successfully uploaded).
    * **`FAILED`**: An error icon ⚠️ (indicating the message failed to upload and can be retried).
* **Network Status UI**: A UI banner appears at the top of the chat screen to clearly inform the user when they are offline.
* **Dependency Management**: We used a **manual dependency injection (DI)** approach with a custom `ChatViewModelFactory` to manage and provide dependencies like `ChatRepository` and `NetworkStatusObserver` to the `ChatViewModel`.
* **Media and File Handling**: The system was updated to support sending and retrying images, audio, and other files.

***

### Key decisions

* **UI/State Management**: We implemented a `ChatUiState` sealed class to manage the UI's state, allowing the screen to gracefully handle loading, errors, and content. The UI observes a single `uiState` `StateFlow` to drive all rendering.
* **Data Flow**: The data flow is `Remote > Repository > Local Database > ViewModel > UI`. All incoming and outgoing messages are first processed by the `ChatRepository` and persisted in a local Room database, ensuring data is always available locally.
* **Network Status**: The `NetworkStatusObserver` was created to monitor the device's connectivity. The `ChatViewModel` observes this status to automatically retry failed messages when the network becomes available.
* **File Provider**: The `FileProvider` was configured in the `AndroidManifest.xml` to securely share exported chat files, such as ZIPs and text documents.

***

### How to run/test

1.  **Run the app** on a physical device or emulator.
2.  **Go Offline**: Navigate to a chat room and turn off Wi-Fi and mobile data (use airplane mode for a guaranteed offline state).
3.  **Send a Message**: Type and send a message. The message should appear with a `SENDING` status icon (⏱️).
4.  **Go Online**: Turn the network back on.
5.  **Verify Status**: The message status should automatically update to `SENT` (✔️).

***

### Test coverage explanation

While we have manually verified the core functionality, the implementation is still partially successful due to a critical bug. This bug is causing message status icons to revert unexpectedly, preventing a consistent and reliable display of message states after multiple messages are sent and the network status changes.

The app is functional in that it saves and sends messages, but the UI feedback for message status is not stable. Further debugging is required to pinpoint and fix this issue, ensuring the UI accurately reflects the true status of each message as it's processed by the network.


