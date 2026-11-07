package com.example.capstonesample.data.model

import com.example.capstonesample.data.api.TaskResponse
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun isFinishedWorkStatus(status: String?, progress: Int? = null): Boolean {
    if (progress != null && progress >= 100) return true
    return status?.trim()?.lowercase(Locale.ROOT) in
        setOf("completed", "done", "approved", "verified", "closed", "cancelled", "finished")
}

fun isTaskOverdue(dueDate: String?): Boolean {
    if (dueDate.isNullOrBlank()) return false
    val trimmed = dueDate.trim()
    if (trimmed.equals("No due date", ignoreCase = true)) return false

    return try {
        // Pure calendar date: YYYY-MM-DD
        if (trimmed.matches(Regex("""^\d{4}-\d{2}-\d{2}$"""))) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
            val due = sdf.parse(trimmed) ?: return false
            val calToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            due.before(calToday.time)
        } else {
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
            )
            var parsedDate: Date? = null
            for (fmt in formats) {
                try {
                    val sdf = SimpleDateFormat(fmt, Locale.ROOT)
                    if (fmt.endsWith("'Z'")) sdf.timeZone = TimeZone.getTimeZone("UTC")
                    val d = sdf.parse(trimmed)
                    if (d != null) {
                        parsedDate = d
                        break
                    }
                } catch (_: Exception) {}
            }
            if (parsedDate != null) {
                parsedDate.before(Date())
            } else {
                false
            }
        }
    } catch (_: Exception) {
        false
    }
}

fun workStatusLabel(status: String?, dueDate: String? = null, progress: Int? = null): String {
    val norm = status?.trim()?.lowercase(Locale.ROOT)
    if (norm == "locked") {
        return "Locked"
    }
    if (isFinishedWorkStatus(status, progress)) {
        return when (status?.trim()?.lowercase(Locale.ROOT)) {
            "completed" -> "Completed"
            "done" -> "Done"
            "approved" -> "Approved"
            "verified" -> "Verified"
            "closed" -> "Closed"
            "cancelled" -> "Cancelled"
            "finished" -> "Completed"
            else -> if (progress != null && progress >= 100) "Completed" else status.orEmpty().trim()
        }
    }
    if (!dueDate.isNullOrBlank() && isTaskOverdue(dueDate)) {
        return "Delayed"
    }
    return when (status?.trim()?.lowercase(Locale.ROOT)) {
        null, "", "pending" -> "Pending"
        "ongoing", "in progress", "in-progress" -> "Ongoing"
        "delayed", "late" -> "Delayed"
        else -> status.orEmpty().trim()
    }
}

data class UpdateTaskWorkStatusRequest(val status: String)

data class UpdateSubtaskWorkStatusRequest(
    @SerializedName("subtask_id") val subtaskId: String,
    val status: String
)

data class TaskUpdateResponse(
    val success: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val data: TaskResponse? = null
)
