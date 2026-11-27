package com.example.capstonesample.data.model

import com.google.gson.annotations.SerializedName


data class JoinProjectRequest(

    @SerializedName("invite_code")
    val inviteCode: String,

    @SerializedName("position")
    val position: String
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

    val manager: String? = null,

    @SerializedName("geofence_latitude")
    val geofenceLatitude: Double? = null,

    @SerializedName("geofence_longitude")
    val geofenceLongitude: Double? = null,

    @SerializedName("geofence_radius")
    val geofenceRadius: Double? = null,

    @SerializedName("geofence_enabled")
    val geofenceEnabled: Boolean? = null
)

data class VerifyGeofenceRequest(
    val latitude: Double,
    val longitude: Double
)

data class VerifyGeofenceResponse(
    val success: Boolean = false,
    val data: GeofenceVerificationData? = null,
    val message: String? = null
)

data class GeofenceVerificationData(
    @SerializedName("geofence_enabled") val geofenceEnabled: Boolean = false,
    @SerializedName("has_coordinates") val hasCoordinates: Boolean = false,
    @SerializedName("is_inside") val isInside: Boolean = true,
    @SerializedName("distance_meters") val distanceMeters: Double? = null,
    @SerializedName("allowed_radius_meters") val allowedRadiusMeters: Double? = null,
    val status: String? = null,
    val message: String? = null
)

data class ProjectGeofenceResponse(
    val success: Boolean = false,
    val data: ProjectGeofenceData? = null,
    val message: String? = null
)

data class ProjectGeofenceData(
    val id: String? = null,
    val code: String? = null,
    val name: String? = null,
    val location: String? = null,
    @SerializedName("geofence_latitude") val geofenceLatitude: Double? = null,
    @SerializedName("geofence_longitude") val geofenceLongitude: Double? = null,
    @SerializedName("geofence_radius") val geofenceRadius: Double? = null,
    @SerializedName("geofence_enabled") val geofenceEnabled: Boolean? = null
)
