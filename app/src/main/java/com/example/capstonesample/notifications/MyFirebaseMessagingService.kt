package com.example.capstonesample.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM_SERVICE"
        private const val PREFS_NAME = "sitepulse_fcm"
        private const val KEY_FCM_TOKEN = "fcm_token"

        fun getSavedToken(context: Context): String? {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_FCM_TOKEN, null)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received: $token")
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FCM_TOKEN, token)
            .apply()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "FCM message received. From: ${remoteMessage.from}, Data: ${remoteMessage.data}")

        val title = remoteMessage.data["title"]
            ?: remoteMessage.notification?.title
            ?: "SitePulse"

        val message = remoteMessage.data["message"]
            ?: remoteMessage.data["body"]
            ?: remoteMessage.notification?.body
            ?: "You have a new notification."

        val type = remoteMessage.data["type"]
            ?: "notification"

        val notificationId = remoteMessage.data["notificationId"]
            ?: remoteMessage.data["id"]

        val conversationId = remoteMessage.data["conversationId"]

        // Show notification via NotificationHelper (will alert even if app is closed)
        NotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            message = message,
            type = type,
            notificationId = notificationId,
            conversationId = conversationId
        )
    }
}