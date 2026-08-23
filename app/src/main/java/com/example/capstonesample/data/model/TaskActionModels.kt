package com.example.capstonesample.data.model

import com.google.gson.annotations.SerializedName


data class CompleteTaskRequest(
    val status: String = "Completed"
)


data class CompleteTaskResponse(
    val success: Boolean,
    val message: String,
    val data: CompletedTaskData? = null
)


data class CompletedTaskData(

    val id: String? = null,

    @SerializedName("task_name")
    val taskName: String? = null,

    val status: String? = null,

    @SerializedName("completed_at")
    val completedAt: String? = null
)


data class UploadTaskReportRequest(

    @SerializedName("task_id")
    val taskId: String,

    @SerializedName("project_code")
    val projectCode: String,

    val title: String,

    @SerializedName("report_text")
    val reportText: String,

    @SerializedName("report_type")
    val reportType: String = "AI Field Report"
)


data class UploadTaskReportResponse(

    val success: Boolean,

    val message: String,

    val data: UploadedTaskReport? = null
)


data class UploadedTaskReport(

    val id: String? = null,

    @SerializedName("task_id")
    val taskId: String? = null,

    @SerializedName("project_code")
    val projectCode: String? = null,

    val title: String? = null,

    @SerializedName("report_text")
    val reportText: String? = null,

    @SerializedName("report_type")
    val reportType: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null
)