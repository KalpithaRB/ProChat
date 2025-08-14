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
## Day 5 – Unit Testing, Refactoring & Error Handling
---

### What Was Done

We expanded our unit testing of the `ChatViewModel` to cover several core chat functionalities. This included testing the entire lifecycle of a message, from sending to retrying, as well as specific features like marking a message as read and handling chatroom deletion. The key test functions we implemented and analyzed were:

-   `sendMessage_shouldAddMessageWithSendingStatus_thenUpdateToSent`: Validates the full process of sending a text message, including the UI state transitions from `SENDING` to `SENT`.
-   `retryFailedMessages_shouldResendMessagesWithFailedStatus`: Tests the functionality for retrying messages that previously failed to send. It confirms the status correctly changes from `FAILED` to `SENDING` and then `SENT`.
-   `prepareAndSendImageMessage_shouldUploadFileAndSendFinalMessage`: A variation of the file upload test, specifically for images, which verifies that the temporary local URI is replaced by a remote URL upon successful upload.
-   `markLastMessageAsRead_shouldUpdateStatusOfLastIncomingMessage`: Checks the logic for updating a message's status. It ensures that an incoming message's status is correctly updated from `DELIVERED` to `READ`.
-   `deleteChatroom_shouldCallRepositoryAndEmitSuccessOnSharedFlow`: Verifies that the ViewModel correctly initiates the chatroom deletion process and emits a success signal on a `SharedFlow` for UI observation.

***

### Key Decisions

1.  **Using `Fake` Repositories**: Instead of using mock objects for every call, we used `FakeChatRepository` and `FakeChatRoomRepository`. These are classes that implement the repository interfaces but are designed for testing. This decision allowed us to manage a simulated internal state (like a list of messages) and provided a more realistic testing environment. We used `spyk` on these fakes to both simulate their behavior and verify that their methods were called. 
2.  **Verifying State Transitions with `Turbine`**: For tests like `sendMessage` and `retryFailedMessages`, we used multiple `awaitItem()` calls to verify the state transitions. For example, in `sendMessage`, we first awaited the `SENDING` state and then the final `SENT` state after calling `advanceUntilIdle()`. This ensures that the ViewModel's state is correctly reflecting the asynchronous operations.
3.  **Asserting on SharedFlows**: For a one-time event like `deleteChatroom_shouldCallRepositoryAndEmitSuccessOnSharedFlow`, we used `Turbine` to collect from the `deletionSuccess` `SharedFlow`. This is an ideal way to test `SharedFlows`, as it allows you to `awaitItem()` and verify that the event was correctly emitted.
4.  **Flexible Verification**: In some tests, like `prepareAndSendImageMessage`, we opted for `coVerify(atLeast = 1)` instead of `exactly = 1`. This decision provides more flexibility for scenarios where a function might be called multiple times internally, as long as it's called at least once. This is a pragmatic choice to avoid test brittleness caused by minor implementation details.
5.  **Comprehensive Mocking**: We meticulously mocked all dependencies, including static utility methods (`FileDetailsUtilKt`) and Android system services (`Log`, `ConnectivityManager`), using `mockkStatic` and `mockk`. This ensured a fully isolated and reliable unit test environment, preventing external factors from influencing the test results.

***

### How to Run/Test

The unit tests are designed to be executed within the standard Android testing framework.

-   **Android Studio**: To run all tests in the `ChatViewModelTest` class, right-click the class name and select "Run 'ChatViewModelTest'". You can also run individual test functions by right-clicking their names.
-   **Gradle Command**: To run the entire test class from the terminal, you would use:
    `./gradlew :app:testDebugUnitTest --tests "com.kalpi.prochat.ui.presentations.viewmodel.ChatViewModelTest"`

***

### Test Coverage Explanation

The added test functions significantly expand our unit test coverage for the `ChatViewModel`. We're now covering:

-   **Happy Paths**: `sendMessage` and `prepareAndSendImageMessage` cover the most common success cases for both text and file messages.
-   **Error Handling and Retry Logic**: `retryFailedMessages` is a crucial test for a chat application. It ensures that the app can recover from network failures and that the UI state correctly reflects this process.
-   **User Actions**: `markLastMessageAsRead` and `deleteChatroom` test the ViewModel's response to specific user-initiated actions, confirming that the correct repository methods are called and the UI state is updated.

By using `Fake` repositories and `Turbine`, we have created robust and comprehensive tests that validate not only the ViewModel's public API but also its internal state changes over time. This approach provides a high degree of confidence that the ViewModel is working correctly.

