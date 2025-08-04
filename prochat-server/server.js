const express = require('express');
const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

// --- 1. Initialize Firebase Admin SDK ---
try {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
  console.log("Firebase Admin SDK initialized successfully.");
} catch (error) {
  console.error("Error initializing Firebase Admin SDK:", error);
  process.exit(1); // Exit if Firebase initialization fails
}

const firestore = admin.firestore();
const messaging = admin.messaging();
const app = express();
app.use(express.json()); // Middleware to parse JSON bodies

const PORT = process.env.PORT || 3000;

// --- 2. Endpoint to Save FCM Token ---
app.post('/api/save-fcm-token', async (req, res) => {
  const { userId, fcmToken } = req.body;

  if (!userId || !fcmToken) {
    return res.status(400).send({ error: 'User ID and FCM token are required.' });
  }

  try {
    // Store the token in a 'users' collection
    await firestore.collection('users').doc(userId).set({
      fcmToken: fcmToken
    }, { merge: true });

    console.log(`FCM token for user ${userId} saved successfully.`);
    res.status(200).send({ success: true, message: 'FCM token saved.' });
  } catch (error) {
    console.error('Error saving FCM token:', error);
    res.status(500).send({ error: 'Failed to save token.' });
  }
});

// --- 3. Endpoint to Handle New Chat Messages and Send Notifications ---
app.post('/api/send-chat-message', async (req, res) => {
  const { senderId, roomId, messageText } = req.body;

  if (!senderId || !roomId || !messageText) {
    return res.status(400).send({ error: 'Sender ID, Room ID, and message text are required.' });
  }

  try {
    // Save the new message to Firestore
    const newMessageRef = firestore.collection('chatRooms').doc(roomId).collection('messages').doc();
    await newMessageRef.set({
      text: messageText,
      senderId: senderId,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      // Add other fields you might have
    });

    // Get the FCM tokens of all participants (excluding the sender)
    const roomDoc = await firestore.collection('chatRooms').doc(roomId).get();
    const participants = roomDoc.data()?.participants;

    const recipientTokens = [];
    if (participants) {
      const recipientIds = participants.filter(uid => uid !== senderId);
      for (const uid of recipientIds) {
        const userDoc = await firestore.collection('users').doc(uid).get();
        const token = userDoc.data()?.fcmToken;
        if (token) {
          recipientTokens.push(token);
        }
      }
    }

    // Construct and send the FCM message to all recipient tokens
    if (recipientTokens.length > 0) {
      const payload = {
        notification: {
          title: 'New Message',
          body: messageText,
        },
        data: {
          type: 'chat_message',
          roomId: roomId,
          messagePreview: messageText,
          senderId: senderId,
        },
        tokens: recipientTokens, // The list of recipient tokens
      };

      const response = await MessagingEachForMulticast(payload);
      console.log(`Notification sent to ${response.successCount} devices.`);
      // Optional: Handle failed tokens here if needed
    }

    res.status(200).send({ success: true, message: 'Message sent and notification triggered.' });

  } catch (error) {
    console.error('Error sending chat message and notification:', error);
    res.status(500).send({ error: 'Failed to process message.' });
  }
});

app.listen(PORT, () => {
  console.log(`Server is running on http://localhost:${PORT}`);
});