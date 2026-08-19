package com.example.capstonesample.data.model

import com.google.gson.annotations.SerializedName


data class DashboardResponse(
    val success: Boolean = false,
    val data: DashboardData? = null
)


data class DashboardData(

    val user: DashboardUser? = null,

    @SerializedName(
        value = "health_score",
        alternate = ["healthScore"]
    )
    val healthScore: Int = 0,

    val projects: List<DashboardProject> = emptyList(),

    val activities: List<DashboardActivity> = emptyList(),

    val stats: DashboardStats? = null
)


data class DashboardUser(

    val id: String? = null,

    @SerializedName(
        value = "full_name",
        alternate = ["name", "fullName"]
    )
    val name: String = "",

    val email: String? = null,

    val role: String = ""
)


data class DashboardProject(

    val id: String? = null,

    @SerializedName(
        value = "project_name",
        alternate = ["name"]
    )
    val name: String = "",

    @SerializedName(
        value = "project_type",
        alternate = ["type"]
    )
    val type: String? = null,

    @SerializedName(
        value = "progress_pct",
        alternate = ["progress"]
    )
    val progress: Int = 0,

    val status: String? = null
)


data class DashboardActivity(

    val id: String? = null,

    val title: String = "",

    val description: String? = null,

    val type: String? = null
)


data class DashboardStats(

    @SerializedName(
        value = "on_track",
        alternate = ["onTrack"]
    )
    val onTrack: Int = 0,

    val delayed: Int = 0,

    @SerializedName(
        value = "total_projects",
        alternate = ["totalProjects"]
    )
    val totalProjects: Int = 0
)