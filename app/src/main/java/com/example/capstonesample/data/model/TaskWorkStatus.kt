package com.example.capstonesample.data.model

import com.example.capstonesample.data.api.TaskResponse
import com.google.gson.annotations.SerializedName
import java.util.Locale

fun isFinishedWorkStatus(status: String?): Boolean =
    status?.trim()?.lowercase(Locale.ROOT) in
        setOf("completed", "done", "approved", "verified", "closed", "cancelled")

fun workStatusLabel(status: String?): String = when (status?.trim()?.lowercase(Locale.ROOT)) {
    null, "", "pending" -> "Pending"
    "ongoing", "in progress", "in-progress" -> "Ongoing"
    else -> status.orEmpty().trim()
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
