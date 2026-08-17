package com.example.capstonesample.data.repository

import android.content.Context
import com.example.capstonesample.data.api.ApiService
import com.example.capstonesample.data.local.TaskDao
import com.example.capstonesample.data.local.TaskEntity
import com.example.capstonesample.network.NetworkMonitor
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val context: Context,
    private val api: ApiService,
    private val taskDao: TaskDao
) {

    // UI always reads from local database
    fun getTasks(): Flow<List<TaskEntity>> {
        return taskDao.getTasks()
    }

    // Try to refresh from server
    suspend fun refreshTasks(token: String) {

        // No internet?
        // Just keep using saved database.
        if (!NetworkMonitor.isInternetAvailable(context)) {
            return
        }

        try {

            val response = api.getTasks(
                token = "Bearer $token"
            )

            if (response.isSuccessful) {

                val tasks = response.body() ?: emptyList()

                val localTasks = tasks.map { task ->

                    TaskEntity(
                        id = task.id,
                        title = task.title,
                        description = task.description,
                        status = task.status
                    )
                }

                taskDao.insertTasks(localTasks)
            }

        } catch (e: Exception) {

            // Don't crash the app.
            // Keep displaying cached tasks.

            println("Task sync failed: ${e.message}")
        }
    }
}