package com.example.capstonesample

import android.content.Context
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.example.capstonesample.data.api.RetrofitClient
import com.example.capstonesample.data.api.SubtaskItem
import com.example.capstonesample.data.model.isFinishedWorkStatus
import com.example.capstonesample.security.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

object NavBadgeState {

    var ongoingProjectsCount by mutableIntStateOf(0)
    var unreadMessagesCount by mutableIntStateOf(0)
    var ongoingTasksCount by mutableIntStateOf(0)

    fun getUserIdFromJwt(token: String?): String? {
        if (token.isNullOrBlank() || token.startsWith("LOCAL_")) {
            return null
        }
        return try {
            val cleanToken = token.removePrefix("Bearer ").trim()
            val parts = cleanToken.split(".")
            if (parts.size < 2) return null
            val decodedBytes = Base64.decode(
                parts[1],
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
            val json = JSONObject(String(decodedBytes, Charsets.UTF_8))
            if (json.has("id")) json.getString("id") else null
        } catch (_: Exception) {
            null
        }
    }

    fun isOngoingProject(status: String?): Boolean {
        val s = status?.trim()?.lowercase(Locale.ROOT) ?: return true
        if (s == "completed" || s == "done" || s == "archived" || s == "cancelled") {
            return false
        }
        return s == "ongoing" || s == "active" || s == "on track" || s == "in progress" || s == "planning"
    }

    fun isCompletedTask(
        status: String?,
        progress: Int? = null,
        subtasks: List<SubtaskItem>? = null
    ): Boolean {
        if (isFinishedWorkStatus(status, progress)) return true
        val s = status?.trim()?.lowercase(Locale.ROOT) ?: ""
        if (s in setOf("completed", "done", "approved", "verified", "closed", "finish", "finished")) {
            return true
        }
        if (progress != null && progress >= 100) {
            return true
        }
        if (!subtasks.isNullOrEmpty() && subtasks.all {
            it.completed || it.status.equals("completed", ignoreCase = true) || (it.progress ?: 0) >= 100
        }) {
            return true
        }
        return false
    }

    fun isOngoingTask(
        status: String?,
        dueDate: String? = null,
        progress: Int? = null,
        subtasks: List<SubtaskItem>? = null
    ): Boolean {
        // RULE 1: Never count completed tasks in ongoing badge!
        if (isCompletedTask(status, progress, subtasks)) return false

        val s = status?.trim()?.lowercase(Locale.ROOT) ?: ""
        // RULE 2: Never count cancelled, archived, or locked tasks
        if (s in setOf("cancelled", "canceled", "archived", "rejected", "locked")) return false

        // RULE 3: Active open task (pending, ongoing, in progress, delayed, to do, ready)
        return true
    }

    suspend fun refresh(context: Context? = null, token: String? = null) {
        withContext(Dispatchers.IO) {
            val effectiveToken = if (!token.isNullOrBlank()) {
                token
            } else if (context != null) {
                TokenManager.getToken(context) ?: ""
            } else {
                ""
            }

            val authHeader = if (effectiveToken.isNotBlank()) {
                if (effectiveToken.startsWith("Bearer ")) effectiveToken else "Bearer $effectiveToken"
            } else ""

            val currentUserId = getUserIdFromJwt(effectiveToken)

            // 1. REFRESH UNREAD MESSAGES
            try {
                val convResp = RetrofitClient.api.getConversations()
                if (convResp.isSuccessful && convResp.body()?.success == true) {
                    val list = convResp.body()?.conversations ?: emptyList()
                    val totalUnread = list.sumOf { it.unreadCount.coerceAtLeast(0) }
                    withContext(Dispatchers.Main) {
                        unreadMessagesCount = totalUnread
                    }
                }
            } catch (_: Exception) {}

            // 2. REFRESH ONGOING PROJECTS
            if (authHeader.isNotBlank()) {
                try {
                    val projResp = RetrofitClient.api.getJoinedProjects(authHeader)
                    if (projResp.isSuccessful && projResp.body()?.success == true) {
                        val list = projResp.body()?.data ?: emptyList()
                        val count = list.count { isOngoingProject(it.status) }
                        withContext(Dispatchers.Main) {
                            ongoingProjectsCount = count
                        }
                    } else {
                        val allProjResp = RetrofitClient.api.getProjects(authHeader)
                        if (allProjResp.isSuccessful && allProjResp.body()?.success == true) {
                            val list = allProjResp.body()?.data ?: emptyList()
                            val count = list.count { isOngoingProject(it.status) }
                            withContext(Dispatchers.Main) {
                                ongoingProjectsCount = count
                            }
                        }
                    }
                } catch (_: Exception) {}

                // 3. REFRESH ONGOING TASKS (ONLY LOGGED-IN ENGINEER'S ONGOING TASKS, NEVER COMPLETED)
                try {
                    val taskResp = RetrofitClient.api.getTasks(
                        token = authHeader,
                        assigneeId = currentUserId
                    )
                    if (taskResp.isSuccessful && taskResp.body()?.success == true) {
                        val list = taskResp.body()?.data ?: emptyList()
                        val filteredList = if (!currentUserId.isNullOrBlank()) {
                            list.filter { it.assigneeId.isNullOrBlank() || it.assigneeId.equals(currentUserId, ignoreCase = true) }
                        } else {
                            list
                        }
                        val count = filteredList.count { isOngoingTask(it.status, it.dueDate, it.progress, it.subtasks) }
                        withContext(Dispatchers.Main) {
                            ongoingTasksCount = count
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
