package com.example.capstonesample.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.capstonesample.data.api.RetrofitClient
import com.example.capstonesample.notifications.NotificationHelper
import com.example.capstonesample.security.TokenManager

/**
 * Background worker managed by Android WorkManager.
 * Runs periodically even when the SitePulse application is completely closed or killed.
 * Fetches new system notifications and unread chat messages from the backend server
 * and displays heads-up alerts with sound and vibration in the notification tray.
 */
class NotificationSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "NotificationSyncWorker"
        private const val PREFS_NAME = "sitepulse_notif_tracker"
        private const val KEY_INITIALIZED = "tracker_initialized"
        private const val KEY_SEEN_NOTIF_IDS = "seen_notification_ids"
        private const val KEY_LAST_CONV_MSG_TIMES = "last_conv_msg_prefix_"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "NotificationSyncWorker running in background...")
        RetrofitClient.init(applicationContext)

        val token = TokenManager.getToken(applicationContext)
        if (token.isNullOrBlank()) {
            Log.d(TAG, "No logged in user session. Skipping notification check.")
            return Result.success()
        }

        NotificationHelper.createNotificationChannel(applicationContext)

        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isInitialized = prefs.getBoolean(KEY_INITIALIZED, false)
        val seenIds = prefs.getStringSet(KEY_SEEN_NOTIF_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()

        // 1. CHECK ADMIN / SYSTEM NOTIFICATIONS
        try {
            val response = RetrofitClient.api.getNotifications("Bearer $token")
            if (response.isSuccessful) {
                val notifications = response.body()?.data ?: emptyList()

                if (!isInitialized) {
                    // First run baseline: record existing notifications so we don't spam historical items
                    val currentIds = notifications.map { it.id }.toSet()
                    prefs.edit()
                        .putBoolean(KEY_INITIALIZED, true)
                        .putStringSet(KEY_SEEN_NOTIF_IDS, currentIds)
                        .apply()
                    Log.d(TAG, "Baseline established with ${currentIds.size} existing notifications.")
                } else {
                    // Find any newly posted notifications that haven't been shown yet
                    val newNotifications = notifications.filter { !seenIds.contains(it.id) }

                    for (item in newNotifications) {
                        Log.d(TAG, "New notification found: ${item.title}")
                        NotificationHelper.showNotification(
                            context = applicationContext,
                            title = item.title,
                            message = item.message,
                            type = "admin_notification",
                            notificationId = item.id
                        )
                        seenIds.add(item.id)
                    }

                    // Save updated seen list
                    prefs.edit()
                        .putStringSet(KEY_SEEN_NOTIF_IDS, seenIds)
                        .apply()
                }
            } else {
                Log.w(TAG, "Failed to fetch notifications: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking notifications: ${e.message}")
        }

        // 2. CHECK UNREAD CHAT MESSAGES
        try {
            val convResponse = RetrofitClient.api.getConversations()
            Log.d(TAG, "Conversations check: HTTP ${convResponse.code()}, count: ${convResponse.body()?.conversations?.size}")
            if (convResponse.isSuccessful) {
                val conversations = convResponse.body()?.conversations ?: emptyList()

                for (conv in conversations) {
                    val unreadCount = conv.unreadCount ?: 0
                    val lastMsgTime = conv.lastMessageTime ?: ""
                    val convId = conv.conversationId
                    val prefKey = "$KEY_LAST_CONV_MSG_TIMES$convId"
                    val lastSeenTime = prefs.getString(prefKey, "") ?: ""

                    if (unreadCount > 0 && lastMsgTime.isNotBlank() && lastMsgTime != lastSeenTime) {
                        // We have an unread message that arrived since our last check!
                        val senderTitle = conv.fullName?.takeIf { it.isNotBlank() }
                            ?: conv.groupName?.takeIf { it.isNotBlank() }
                            ?: "New Message"

                        val body = conv.lastMessage ?: "You have a new message."

                        NotificationHelper.showNotification(
                            context = applicationContext,
                            title = senderTitle,
                            message = body,
                            type = "chat_message",
                            notificationId = "msg_${convId}_${lastMsgTime.hashCode()}",
                            conversationId = convId.toString()
                        )

                        // Update last seen message timestamp for this conversation
                        prefs.edit().putString(prefKey, lastMsgTime).apply()
                    } else if (lastMsgTime.isNotBlank() && lastMsgTime != lastSeenTime) {
                        // Sync baseline timestamp if no unread messages
                        prefs.edit().putString(prefKey, lastMsgTime).apply()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking chat messages: ${e.message}")
        }

        return Result.success()
    }
}
