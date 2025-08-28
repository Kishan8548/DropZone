package com.example.dropzone.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.dropzone.MainActivity
import com.example.dropzone.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FCMService"
    private val CHANNEL_ID = "dropzone_channel_id"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "From: ${remoteMessage.from}")

        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
            // You can process the data payload here if needed.
        }

        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            showNotification(it.title, it.body)
        }
    }

    private fun sendRegistrationToServer(token: String?) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null && token != null) {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(userId)
            val data = hashMapOf(
                "fcmToken" to token
            )
            userRef.update(data as Map<String, Any>)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token successfully updated for user: $userId")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error updating FCM token for user: $userId", e)
                }
        }
    }

    // Shows the notification to the user.
    private fun showNotification(title: String?, messageBody: String?) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // For Android Oreo and above, we need to create a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "DropZone Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}