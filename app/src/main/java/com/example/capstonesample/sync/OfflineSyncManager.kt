package com.example.capstonesample.sync

import android.content.Context
import android.util.Log
import com.example.capstonesample.data.api.RetrofitClient
import com.example.capstonesample.data.api.TimeLogCreateRequest
import com.example.capstonesample.security.TokenManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class QueuedTimeLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val token: String,
    val request: TimeLogCreateRequest,
    val createdAt: Long = System.currentTimeMillis()
)

object OfflineSyncManager {

    private const val TAG = "OfflineSyncManager"
    private const val PREFS_NAME = "sitepulse_offline_sync"
    private const val KEY_PENDING_TIMELOGS = "pending_timelogs"
    private const val KEY_CACHED_PROJECTS = "cached_projects"
    private val gson = Gson()

    fun cacheProjects(context: Context, projects: List<com.example.capstonesample.data.model.ProjectResponse>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CACHED_PROJECTS, gson.toJson(projects)).apply()
    }

    fun getCachedProjects(context: Context): List<com.example.capstonesample.data.model.ProjectResponse> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CACHED_PROJECTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<com.example.capstonesample.data.model.ProjectResponse>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing cached projects", e)
            emptyList()
        }
    }

    fun queueTimeLog(context: Context, token: String, request: TimeLogCreateRequest) {
        val currentQueue = getPendingTimeLogs(context).toMutableList()
        currentQueue.add(QueuedTimeLog(token = token, request = request))
        saveQueue(context, currentQueue)
        Log.i(TAG, "Queued offline time log for project '${request.projectName}' (${request.totalWorkHours}h). Total queue size: ${currentQueue.size}")
    }

    fun getPendingTimeLogs(context: Context): List<QueuedTimeLog> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PENDING_TIMELOGS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<QueuedTimeLog>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading pending timelogs queue", e)
            emptyList()
        }
    }

    fun getPendingCount(context: Context): Int {
        return getPendingTimeLogs(context).size
    }

    fun hasPendingLogs(context: Context): Boolean {
        return getPendingCount(context) > 0
    }

    private fun saveQueue(context: Context, queue: List<QueuedTimeLog>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PENDING_TIMELOGS, gson.toJson(queue)).apply()
    }

    suspend fun syncPendingTimeLogs(context: Context): Int {
        val queue = getPendingTimeLogs(context)
        if (queue.isEmpty()) return 0

        Log.i(TAG, "Starting synchronization of ${queue.size} pending time log(s)...")

        val remaining = mutableListOf<QueuedTimeLog>()
        var syncedCount = 0

        // Resolve active fresh JWT token from TokenManager if available
        val savedToken = TokenManager.getToken(context)
        val activeJwt = if (!savedToken.isNullOrBlank() && !savedToken.startsWith("LOCAL_")) {
            savedToken
        } else null

        for (item in queue) {
            // Determine best token to use: active server JWT takes priority over stale/offline token
            val candidateToken = activeJwt ?: item.token
            if (candidateToken.isBlank() || candidateToken.startsWith("LOCAL_")) {
                // Device has only an offline local session and no valid server JWT yet.
                // Keep log in queue so it can sync once user authenticates online.
                Log.w(TAG, "No valid online JWT token available to sync log for '${item.request.projectName}'. Retaining in queue.")
                remaining.add(item)
                continue
            }

            val authToken = if (candidateToken.startsWith("Bearer ")) candidateToken else "Bearer $candidateToken"

            try {
                val response = RetrofitClient.api.createTimeLog(
                    token = authToken,
                    request = item.request
                )

                if (response.isSuccessful && (response.body()?.success == true || response.code() in 200..299)) {
                    syncedCount++
                    Log.i(TAG, "Successfully synced time log for '${item.request.projectName}'")
                } else if (response.code() == 409) {
                    // Conflict / already exists on server: mark as completed so queue does not get blocked
                    syncedCount++
                    Log.i(TAG, "Time log for '${item.request.projectName}' already recorded on server (409 Conflict). Removing from queue.")
                } else if (response.code() in 401..403) {
                    // Auth failure / token expired: DO NOT DROP! Keep in queue so it retries upon login!
                    Log.w(TAG, "Authentication error (${response.code()}) while syncing. Retaining in queue for retry.")
                    remaining.add(item)
                } else {
                    // Server error or unexpected response: retain in queue to avoid losing work
                    Log.w(TAG, "Server returned HTTP ${response.code()} for '${item.request.projectName}'. Retaining in queue.")
                    remaining.add(item)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network exception while syncing time log for '${item.request.projectName}': ${e.message}")
                remaining.add(item)
            }
        }

        saveQueue(context, remaining)
        Log.i(TAG, "Sync complete. Synced: $syncedCount, Remaining in queue: ${remaining.size}")
        return syncedCount
    }
}
