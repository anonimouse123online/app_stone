package com.example.capstonesample.data.repository

import android.content.Context
import com.example.capstonesample.data.api.ApiService
import com.example.capstonesample.data.api.TaskResponse
import com.example.capstonesample.data.local.TaskDao
import com.example.capstonesample.data.local.TaskEntity
import com.example.capstonesample.network.NetworkMonitor
import kotlinx.coroutines.flow.Flow


class TaskRepository(

    private val context: Context,

    private val api: ApiService,

    private val taskDao: TaskDao

) {

    // ============================================================
    // READ TASKS FROM ROOM
    // ============================================================

    fun getTasks(): Flow<List<TaskEntity>> {
        return taskDao.getTasks()
    }


    // ============================================================
    // DOWNLOAD ASSIGNED TASKS
    // ============================================================

    suspend fun refreshTasks(

        token: String,

        assigneeId: String,

        projectId: String? = null

    ) {

        if (
            token.isBlank() ||
            token.startsWith("LOCAL_")
        ) {

            println("📴 No online JWT. Using cached tasks.")
            return
        }


        if (
            !NetworkMonitor.isInternetAvailable(context)
        ) {

            println("📴 No internet. Using cached tasks.")
            return
        }


        try {

            println("======================================")
            println("🌐 DOWNLOADING TASKS")
            println("ASSIGNEE = $assigneeId")
            println("PROJECT = $projectId")
            println("======================================")


            val response =
                api.getTasks(

                    token = "Bearer $token",

                    assigneeId = assigneeId,

                    projectId = projectId
                )


            println("===== ANDROID TASK API DEBUG =====")
            println("HTTP = ${response.code()}")
            println("SUCCESS = ${response.isSuccessful}")
            println("BODY = ${response.body()}")
            println("==================================")


            if (!response.isSuccessful) {

                val error =
                    response
                        .errorBody()
                        ?.string()

                println(
                    "❌ TASK API ERROR = $error"
                )

                return
            }


            // ====================================================
            // IMPORTANT:
            // response.body() is TasksResponse,
            // NOT List<TaskResponse>
            // ====================================================

            val result =
                response.body()


            if (result == null) {

                println(
                    "❌ TASK RESPONSE BODY IS NULL"
                )

                return
            }


            if (!result.success) {

                println(
                    "❌ TASK RESPONSE success=false"
                )

                return
            }


            val serverTasks: List<TaskResponse> =
                result.data


            println("======================================")
            println(
                "📥 SERVER TASK COUNT = ${serverTasks.size}"
            )
            println("======================================")


            serverTasks.forEach { task ->

                println(
                    "TASK ID = ${task.id}"
                )

                println(
                    "TITLE = ${task.title}"
                )

                println(
                    "STATUS = ${task.status}"
                )

                println(
                    "PROJECT ID = ${task.projectId}"
                )

                println(
                    "PROJECT NAME = ${task.projectName}"
                )

                println(
                    "ASSIGNEE ID = ${task.assigneeId}"
                )

                println(
                    "ASSIGNEE NAME = ${task.assigneeName}"
                )

                println(
                    "DUE DATE = ${task.dueDate}"
                )

                println(
                    "PHASE = ${task.phase}"
                )

                println(
                    "PRIORITY = ${task.priority}"
                )

                println(
                    "PROGRESS = ${task.progress}"
                )

                println(
                    "--------------------------------------"
                )
            }


            // ====================================================
            // API -> ROOM
            // ====================================================

            val localTasks: List<TaskEntity> =
                serverTasks.map { task ->

                    TaskEntity(

                        id =
                            task.id,

                        title =
                            task.title,

                        description =
                            task.description,

                        status =
                            task.status,

                        projectId =
                            task.projectId,

                        projectName =
                            task.projectName,

                        assigneeId =
                            task.assigneeId,

                        assigneeName =
                            task.assigneeName,

                        dueDate =
                            task.dueDate,

                        phase =
                            task.phase,

                        priority =
                            task.priority,

                        progress =
                            task.progress,

                        isSynced =
                            true
                    )
                }


            println(
                "ROOM TASK COUNT BEFORE INSERT = ${localTasks.size}"
            )


            taskDao.deleteAllTasks()


            if (localTasks.isNotEmpty()) {

                taskDao.insertTasks(
                    localTasks
                )
            }


            println(
                "✅ SAVED ${localTasks.size} TASK(S) INTO ROOM"
            )


        } catch (e: Exception) {

            e.printStackTrace()

            println(
                "❌ TASK SYNC EXCEPTION = ${e.message}"
            )
        }
    }
}