# ProChat - Android Chat Application

## 📌 Overview

**ProChat** is a modern Android chat application built using **Kotlin** and **Jetpack Compose**, following the **MVVM (Model-View-ViewModel)** architecture. This document outlines the Day 1 deliverables, focused on building the **chat screen UI** using dummy data and managing screen state effectively with Compose and StateFlow.

---

## ✨ Features Implemented (Day 1)

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

---

## 🧠 MVVM Architecture Summary

**MVVM (Model-View-ViewModel)** helps separate concerns for better testability and maintainability.

- **Model**: 
  - `ChatMessage` data class represents individual chat items.
  - Dummy data generation mimics realistic timestamp gaps.

- **ViewModel**:
  - Holds app state via `MutableStateFlow<ChatUiState>`
  - Handles dummy data generation, sorting, and grouping by date
  - Exposes a `uiState` that the `ChatScreen` observes

- **View (UI)**:
  - `ChatScreen` observes `uiState` and composes UI accordingly
  - Messages and date separators are rendered dynamically
  - User input and button interactions are handled within Composables

---

## 🧩 UI Logic & Components

### Chat UI Rendering

- `LazyColumn` renders a mixed list of messages and date separators
- Each message bubble:
  - Aligns based on sender (`currentUser` vs. `otherUser`)
  - Has styled corners and background colors for a speech-bubble look
  - Timestamps formatted using `SimpleDateFormat("hh:mm a")`

### Date Separator Logic

- Uses a helper function to compare dates (`isSameDay()`)
- Inserts separators when the day changes in the dummy message list
- Labels like "Today", "Yesterday", or formatted full date

---

## 🚀 Running the App

### Prerequisites

- Android Studio (Hedgehog / Iguana or newer)
- Kotlin plugin
- Emulator or Android device (with USB debugging)

### Setup Steps

```bash
git clone https://github.com/KalpithaRB/ProChat
cd ProChat
