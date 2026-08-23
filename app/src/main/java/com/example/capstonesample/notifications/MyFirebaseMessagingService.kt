package com.example.capstonesample.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.util.Log

import androidx.core.app.NotificationCompat

import com.example.capstonesample.MainActivity
import com.example.capstonesample.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MyFirebaseMessagingService : FirebaseMessagingService() {


    companion object {

        private const val CHANNEL_ID =
            "sitepulse_notifications"

        private const val CHANNEL_NAME =
            "SitePulse Notifications"

        private const val CHANNEL_DESCRIPTION =
            "Project updates, announcements, and SitePulse notifications"
    }


    // ============================================================
    // NEW FIREBASE TOKEN
    // ============================================================

    override fun onNewToken(token: String) {

        super.onNewToken(token)

        Log.d(
            "FCM_TOKEN",
            "New token: $token"
        )
    }


    // ============================================================
    // RECEIVE FIREBASE MESSAGE
    // ============================================================

    override fun onMessageReceived(
        remoteMessage: RemoteMessage
    ) {

        super.onMessageReceived(remoteMessage)


        Log.d(
            "FCM_MESSAGE",
            "Message received: ${remoteMessage.data}"
        )


        val title =
            remoteMessage.data["title"]
                ?: remoteMessage.notification?.title
                ?: "SitePulse"


        val message =
            remoteMessage.data["message"]
                ?: remoteMessage.notification?.body
                ?: "You have a new notification."


        val type =
            remoteMessage.data["type"]
                ?: "notification"


        val notificationId =
            remoteMessage.data["notificationId"]


        showNotification(
            title = title,
            message = message,
            type = type,
            notificationId = notificationId
        )
    }


    // ============================================================
    // SHOW NOTIFICATION
    // ============================================================

    private fun showNotification(
        title: String,
        message: String,
        type: String,
        notificationId: String?
    ) {

        createNotificationChannel()


        // ========================================================
        // OPEN SITEPULSE WHEN NOTIFICATION IS CLICKED
        // ========================================================

        val intent =
            Intent(
                this,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP

                putExtra(
                    "notification_id",
                    notificationId
                )

                putExtra(
                    "notification_type",
                    type
                )
            }


        val pendingIntent =
            PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )


        // ========================================================
        // SITEPULSE LOGO
        // ========================================================

        val largeIcon =
            BitmapFactory.decodeResource(
                resources,
                R.drawable.logo
            )


        // ========================================================
        // DEFAULT NOTIFICATION SOUND
        // ========================================================

        val sound =
            RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_NOTIFICATION
            )


        // ========================================================
        // BUILD NOTIFICATION
        // ========================================================

        val notification =
            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )

                // Small status-bar icon
                .setSmallIcon(
                    R.drawable.logo
                )

                // Large SitePulse logo
                .setLargeIcon(
                    largeIcon
                )

                // Title
                .setContentTitle(
                    title
                )

                // Short text
                .setContentText(
                    message
                )

                // Expanded notification text
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(title)
                        .bigText(message)
                        .setSummaryText("SitePulse")
                )

                // High priority / heads-up notification
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )

                // Notification category
                .setCategory(
                    NotificationCompat.CATEGORY_MESSAGE
                )

                // Visible on lock screen
                .setVisibility(
                    NotificationCompat.VISIBILITY_PUBLIC
                )

                // Sound
                .setSound(
                    sound
                )

                // Remove when tapped
                .setAutoCancel(
                    true
                )

                // Show timestamp
                .setShowWhen(
                    true
                )

                // Open SitePulse
                .setContentIntent(
                    pendingIntent
                )

                // Action button
                .addAction(
                    R.drawable.logo,
                    "Open SitePulse",
                    pendingIntent
                )

                .build()


        // ========================================================
        // DISPLAY
        // ========================================================

        val notificationManager =
            getSystemService(
                NOTIFICATION_SERVICE
            ) as NotificationManager


        val id =
            notificationId
                ?.hashCode()
                ?: System.currentTimeMillis()
                    .toInt()


        notificationManager.notify(
            id,
            notification
        )
    }


    // ============================================================
    // CREATE NOTIFICATION CHANNEL
    // ============================================================

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {

                    description =
                        CHANNEL_DESCRIPTION

                    enableVibration(
                        true
                    )

                    setShowBadge(
                        true
                    )
                }


            val notificationManager =
                getSystemService(
                    NotificationManager::class.java
                )


            notificationManager
                .createNotificationChannel(
                    channel
                )
        }
    }
}