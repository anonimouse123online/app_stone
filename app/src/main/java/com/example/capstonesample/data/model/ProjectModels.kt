package com.example.capstonesample.data.model

import com.google.gson.annotations.SerializedName


data class JoinProjectRequest(

    @SerializedName("invite_code")
    val inviteCode: String
)


data class JoinProjectResponse(

    val success: Boolean,

    val message: String,

    @SerializedName("project_id")
    val projectId: String? = null
)


data class ProjectsResponse(

    val success: Boolean,

    val data: List<ProjectResponse> = emptyList()
)


data class SingleProjectResponse(

    val success: Boolean,

    val data: ProjectResponse? = null,

    val message: String? = null
)


data class ProjectResponse(

    val id: String? = null,

    val code: String? = null,

    val name: String? = null,

    val location: String? = null,

    val scope: String? = null,

    val client: String? = null,

    val budget: String? = null,

    val phase: String? = null,

    val status: String? = null,

    val progress: Double? = 0.0,

    @SerializedName("start_date")
    val startDate: String? = null,

    @SerializedName("due_date")
    val dueDate: String? = null,

    val manager: String? = null
)