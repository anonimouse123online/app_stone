package com.example.capstonesample.data.api

import com.google.gson.annotations.SerializedName

data class NotificationResponse(
    val success: Boolean,
    val data: List<NotificationData> = emptyList()
)

data class NotificationData(
    val id: String,

    val title: String,

    val message: String,

    val audience: String? = null,

    @SerializedName("created_by")
    val createdBy: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("sender_name")
    val senderName: String? = null
)