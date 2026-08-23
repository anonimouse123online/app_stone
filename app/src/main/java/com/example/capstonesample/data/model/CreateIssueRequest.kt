package com.example.capstonesample.data.model

data class CreateIssueRequest(
    val title: String,
    val category: String,
    val priority: String,
    val location: String?,
    val description: String,
    val assigned_to: String? = null
)

data class CreateIssueResponse(
    val success: Boolean,
    val message: String?,
    val data: CreatedIssueData?
)

data class CreatedIssueData(
    val id: String?,
    val project_code: String?,
    val title: String?,
    val category: String?,
    val priority: String?,
    val location: String?,
    val description: String?,
    val status: String?
)