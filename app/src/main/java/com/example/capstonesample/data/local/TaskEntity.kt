package com.example.capstonesample.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.capstonesample.data.api.TaskResponse

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

fun TaskEntity.toTaskResponse(): TaskResponse = TaskResponse(
    id = id,
    title = title,
    description = description,
    materialsRequired = null,
    allocatedMaterials = null,
    resources = null,
    siteInstructions = null,
    status = status,
    projectId = projectId,
    projectCode = null,
    assigneeId = assigneeId,
    projectName = projectName,
    assigneeName = assigneeName,
    dueDate = dueDate,
    originalDueDate = null,
    isExtended = null,
    phase = phase,
    priority = priority,
    progress = progress,
    subtasks = null
)

fun TaskResponse.toTaskEntity(isSynced: Boolean = true): TaskEntity = TaskEntity(
    id = id,
    title = title,
    description = description,
    status = status,
    projectId = projectId,
    projectName = projectName,
    assigneeId = assigneeId,
    assigneeName = assigneeName,
    dueDate = dueDate,
    phase = phase,
    priority = priority,
    progress = progress,
    isSynced = isSynced
)