package com.example.capstonesample.sync

import android.content.Context
import com.example.capstonesample.data.api.RetrofitClient
import com.example.capstonesample.data.api.TimeLogCreateRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class QueuedTimeLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val token: String,
    val request: TimeLogCreateRequest,
    val createdAt: Long = System.currentTimeMillis()
)

object OfflineSyncManager {

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
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun queueTimeLog(context: Context, token: String, request: TimeLogCreateRequest) {
        val currentQueue = getPendingTimeLogs(context).toMutableList()
        currentQueue.add(QueuedTimeLog(token = token, request = request))
        saveQueue(context, currentQueue)
    }

    fun getPendingTimeLogs(context: Context): List<QueuedTimeLog> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PENDING_TIMELOGS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<QueuedTimeLog>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getPendingCount(context: Context): Int {
        return getPendingTimeLogs(context).size
    }

    private fun saveQueue(context: Context, queue: List<QueuedTimeLog>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PENDING_TIMELOGS, gson.toJson(queue)).apply()
    }

    suspend fun syncPendingTimeLogs(context: Context): Int {
        val queue = getPendingTimeLogs(context)
        if (queue.isEmpty()) return 0

        val remaining = mutableListOf<QueuedTimeLog>()
        var syncedCount = 0

        for (item in queue) {
            try {
                val authToken = if (item.token.startsWith("Bearer ")) item.token else "Bearer ${item.token}"
                val response = RetrofitClient.api.createTimeLog(
                    token = authToken,
                    request = item.request
                )
                if (response.isSuccessful && (response.body()?.success == true || response.code() in 200..299)) {
                    syncedCount++
                } else if (response.code() in 400..499) {
                    // Conflict or client error: drop to avoid blocking queue
                    syncedCount++
                } else {
                    remaining.add(item)
                }
            } catch (_: Exception) {
                remaining.add(item)
            }
        }

        saveQueue(context, remaining)
        return syncedCount
    }
}
