package com.example.capstonesample.data.model

data class FieldAnnotation(

    val id: Long =
        System.currentTimeMillis(),

    val label: String,

    val stage: String,

    val progress: Int?,

    // Normalized coordinates from 0f to 1f.
    // These remain valid even when the photo is displayed
    // at a different screen size.
    val left: Float,

    val top: Float,

    val right: Float,

    val bottom: Float
)
