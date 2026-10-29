package com.example.capstonesample.data.model

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
data class CreateIssueRequest(
    val title: String,
    val category: String,
    val priority: String,
    val location: String?,
    val description: String,
    val assigned_to: String? = null
)

fun CreateIssueRequest.toMultipartFields(): Map<String, RequestBody> = buildMap {
    val textType = "text/plain".toMediaType()
    put("title", title.toRequestBody(textType))
    put("category", category.toRequestBody(textType))
    put("priority", priority.toRequestBody(textType))
    put("description", description.toRequestBody(textType))
    location?.let { put("location", it.toRequestBody(textType)) }
    assigned_to?.let { put("assigned_to", it.toRequestBody(textType)) }
}

data class IssueImage(
    val url: String,
    val filename: String
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
    val status: String?,
    val images: List<IssueImage> = emptyList()
)
