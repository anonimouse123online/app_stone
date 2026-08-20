package com.example.capstonesample.data.model

import com.google.gson.annotations.SerializedName


data class ProjectDocumentsResponse(

    val success: Boolean = false,

    val data: List<ProjectDocumentResponse> = emptyList()
)


data class ProjectDocumentResponse(

    val id: Int? = null,

    val name: String? = null,

    val type: String? = null,

    val category: String? = null,

    @SerializedName("uploaded_at")
    val uploadedAt: String? = null
)