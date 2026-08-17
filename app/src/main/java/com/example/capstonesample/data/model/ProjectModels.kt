package com.example.capstonesample.data.model

import com.google.gson.annotations.SerializedName


// ============================================================
// JOIN PROJECT REQUEST
// Backend expects:
// {
//    "invite_code": "E9F7-DEB3"
// }
// ============================================================

data class JoinProjectRequest(

    @SerializedName("invite_code")
    val inviteCode: String
)


// ============================================================
// JOIN PROJECT RESPONSE
// ============================================================

data class JoinProjectResponse(
    val success: Boolean,
    val message: String,

    // Backend /projects/join currently returns project_id
    // instead of a complete project object.
    @SerializedName("project_id")
    val projectId: String? = null
)


// ============================================================
// PROJECT RESPONSE
// Used when fetching projects
// ============================================================

data class ProjectResponse(

    // KEEP Int for now if your GET /projects API
    // currently returns the PostgreSQL numeric project ID.
    val id: Int,

    val category: String?,
    val name: String,
    val manager: String?,
    val progress: Int?,
    val status: String?,

    @SerializedName("dueDate")
    val dueDate: String?,

    @SerializedName("remainingText")
    val remainingText: String?
)