package com.example.capstonesample.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(

    @PrimaryKey
    val id: String,

    val title: String,

    val description: String? = null,

    val status: String? = null,

    val projectId: String? = null,

    val projectName: String? = null,

    val assigneeId: String? = null,

    val assigneeName: String? = null,

    val dueDate: String? = null,

    val phase: String? = null,

    val priority: String? = null,

    val progress: Int? = null,

    val isSynced: Boolean = true
)