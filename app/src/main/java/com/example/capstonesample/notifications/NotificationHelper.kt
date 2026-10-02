package com.example.capstonesample.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.capstonesample.MainActivity
import com.example.capstonesample.R

object NotificationHelper {

    const val CHANNEL_ID = "sitepulse_notifications"
    const val CHANNEL_NAME = "SitePulse Notifications"
    const val CHANNEL_DESCRIPTION = "Project updates, announcements, and SitePulse notifications"

    private const val TAG = "NotificationHelper"

    /**
     * Creates the notification channel required for Android 8.0+ (API 26+)
     * with HIGH importance so that notifications pop up as heads-up alerts.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setSound(soundUri, audioAttributes)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Displays a system notification that will alert the user even if the app
     * is not open, running in the background, or device is locked.
     */
    fun showNotification(
        context: Context,
        title: String,
        message: String,
        type: String = "notification",
        notificationId: String? = null,
        conversationId: String? = null
    ) {
        try {
            createNotificationChannel(context)

            // Intent to open MainActivity when user taps the notification
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("notification_id", notificationId)
                putExtra("notification_type", type)
                putExtra("conversation_id", conversationId)
                if (type == "chat_message" || conversationId != null) {
                    putExtra("navigate_to", "chat")
                } else {
                    putExtra("navigate_to", "notifications")
                }
            }

            val requestCode = (notificationId?.hashCode() ?: System.currentTimeMillis().toInt())
            val pendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Decode large logo icon safely
            val largeIcon = try {
                BitmapFactory.decodeResource(context.resources, R.drawable.logo)
            } catch (e: Exception) {
                null
            }

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(title)
                        .bigText(message)
                        .setSummaryText("SitePulse")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSound(soundUri)
                .setVibrate(longArrayOf(0, 250, 150, 250))
                .setAutoCancel(true)
                .setShowWhen(true)
                .setContentIntent(pendingIntent)
                .addAction(
                    R.drawable.ic_notification,
                    "Open SitePulse",
                    pendingIntent
                )

            if (largeIcon != null) {
                builder.setLargeIcon(largeIcon)
            }

            val notificationManager = NotificationManagerCompat.from(context)
            val id = notificationId?.hashCode() ?: System.currentTimeMillis().toInt()

            notificationManager.notify(id, builder.build())
            Log.d(TAG, "Notification posted successfully: $title ($id)")
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission missing: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show notification: ${e.message}", e)
        }
    }
}
