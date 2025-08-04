package com.kalpi.prochat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kalpi.prochat.ui.presentations.viewmodel.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import retrofit2.HttpException

class LifecycleFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "chat_notifications"
        private const val TAG = "FCM"
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onNewToken(token: String) {
        Log.d(TAG, "New token received: $token")
        sendRegistrationToServer(token)
    }

    // A new function to send the token to your backend/Firestore
    private fun sendRegistrationToServer(token: String?) {
        if (token == null) return

        val uniqueUserId = ChatViewModel.getOrCreateUserId(this)

        // This part is crucial, as you now call your server's API instead of Firestore.
        serviceScope.launch{
            try {
                val response = RetrofitClient.apiService.saveFcmToken(
                    FcmTokenRequest(userId = uniqueUserId, fcmToken = token)
                )
                if (response.isSuccessful) {
                    Log.d(TAG, "FCM token successfully sent to server for user: $uniqueUserId")
                } else {
                    Log.e(TAG, "Failed to send FCM token to server: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error while sending FCM token", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // This is the correct way to handle a data payload message from FCM.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")

            // We extract the required data from the payload.
            val type = remoteMessage.data["type"]
            val incomingRoomId = remoteMessage.data["roomId"] ?: "default_room"

            // Only show a notification if the app is NOT currently in this chat room
            if (type == "chat_message" && AppState.currentChatRoomId != incomingRoomId) {
                val messagePreview = remoteMessage.data["messagePreview"] ?: "You have a new message."
                val senderId = remoteMessage.data["senderId"] ?: "Unknown Sender"

                // Now, we use this data to build and display a notification.
                sendNotification(incomingRoomId, messagePreview, senderId)
            }
        }

        // This block is for handling a notification payload.
        // We will focus on data payloads for your task, as they are more robust.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }
    }

    /**
     * Creates and displays a push notification with a deep link.
     */
    private fun sendNotification(roomId: String, messagePreview: String, senderId: String) {
        try {
            // The deep link URI for the chat room, matching the one in your AndroidManifest.
            val deepLinkUri = Uri.parse("prochat://chatroom/$roomId")

            // Create an intent to handle the deep link.
            val intent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            // A PendingIntent is needed to launch the activity from the notification.
            val pendingIntent = PendingIntent.getActivity(
                this,
                roomId.hashCode(),
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            // Create a notification channel for Android O and above.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Chat Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for incoming chat messages"
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Build the notification.
            val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app's icon
                .setContentTitle("New message from $senderId")
                .setContentText(messagePreview)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            // Display the notification.
            notificationManager.notify(roomId.hashCode(), notificationBuilder.build())
            Log.d(TAG, "Notification sent for room: $roomId")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification: ${e.message}", e)
        }
    }

    override fun onDestroy(){
        super.onDestroy()
        serviceJob.cancel()
    }
}