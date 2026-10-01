package com.example.capstonesample.data.api

import com.google.gson.annotations.SerializedName


// ============================================================
// TIME LOG CREATE REQUEST
// ============================================================

data class TimeLogCreateRequest(

    @SerializedName("project_name")
    val projectName: String,

    @SerializedName("engineer_name")
    val engineerName: String? = null,

    val date: String,

    @SerializedName("work_on_site")
    val workOnSite: Int = 0,

    val supervisors: Int = 0,

    @SerializedName("sub_contractors")
    val subContractors: Int = 0,

    @SerializedName("total_work_hours")
    val totalWorkHours: String = "0",

    val weather: String = "Sunny",

    val temperature: Double? = null,

    @SerializedName("work_completed")
    val workCompleted: String = "",

    @SerializedName("materials_delivered")
    val materialsDelivered: String = "",

    @SerializedName("equipment_used")
    val equipmentUsed: String = "",

    @SerializedName("additional_notes")
    val additionalNotes: String = "",

    @SerializedName("has_incident")
    val hasIncident: Boolean = false
)


// ============================================================
// TIME LOG CREATE RESPONSE
// ============================================================

data class TimeLogCreateResponse(

    val success: Boolean = false,

    val message: String? = null,

    val data: TimeLogCreatedData? = null
)


// ============================================================
// CREATED TIME LOG DATA
// ============================================================

data class TimeLogCreatedData(

    val id: String? = null,

    @SerializedName("project_name")
    val projectName: String? = null,

    @SerializedName("engineer_name")
    val engineerName: String? = null,

    val date: String? = null,

    @SerializedName("work_on_site")
    val workOnSite: Int? = null,

    val supervisors: Int? = null,

    @SerializedName("sub_contractors")
    val subContractors: Int? = null,

    @SerializedName("total_work_hours")
    val totalWorkHours: String? = null,

    val weather: String? = null,

    val temperature: Double? = null,

    @SerializedName("work_completed")
    val workCompleted: String? = null,

    @SerializedName("materials_delivered")
    val materialsDelivered: String? = null,

    @SerializedName("equipment_used")
    val equipmentUsed: String? = null,

    @SerializedName("additional_notes")
    val additionalNotes: String? = null,

    @SerializedName("has_incident")
    val hasIncident: Boolean? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null
)