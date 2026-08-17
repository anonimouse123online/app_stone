package com.example.capstonesample.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: Int,

    val title: String,

    val description: String?,

    val status: String?,
    val isSynced: Boolean = false
)