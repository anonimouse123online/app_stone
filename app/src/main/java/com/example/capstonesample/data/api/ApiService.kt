package com.example.capstonesample.data.api

import com.example.capstonesample.data.model.DashboardResponse
import com.example.capstonesample.data.model.JoinProjectRequest
import com.example.capstonesample.data.model.JoinProjectResponse
import com.example.capstonesample.data.model.LoginRequest
import com.example.capstonesample.data.model.LoginResponse
import com.example.capstonesample.data.model.ProjectsResponse
import com.example.capstonesample.data.model.SingleProjectResponse
import com.example.capstonesample.data.model.SignupRequest
import com.example.capstonesample.data.model.SignupResponse
import com.example.capstonesample.data.model.ConversationsResponse
import com.example.capstonesample.data.model.UserSearchResponse
import com.example.capstonesample.data.model.CreateConversationRequest
import com.example.capstonesample.data.model.CreateConversationResponse
import com.example.capstonesample.data.model.MessagesResponse
import com.example.capstonesample.data.model.SendMessageRequest
import com.example.capstonesample.data.model.SendMessageResponse
import com.example.capstonesample.data.model.CompleteTaskRequest
import com.example.capstonesample.data.model.CompleteTaskResponse
import com.example.capstonesample.data.model.UploadTaskReportRequest
import com.example.capstonesample.data.model.UploadTaskReportResponse
import com.example.capstonesample.data.model.CreateIssueRequest
import com.example.capstonesample.data.model.CreateIssueResponse





import com.google.gson.annotations.SerializedName

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.PATCH


interface ApiService {


    // ============================================================
    // AUTH
    // ============================================================

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>


    @POST("auth/signup")
    suspend fun signup(
        @Body request: SignupRequest
    ): Response<SignupResponse>

    @GET("projects/{code}/documents")
    suspend fun getProjectDocuments(
        @Path("code") code: String
    ): Response<ProjectDocumentsResponse>

    @POST("timelogs")
    suspend fun createTimeLog(
        @Header("Authorization") token: String,
        @Body request: TimeLogCreateRequest
    ): Response<TimeLogCreateResponse>

    @POST("tasks/{taskId}/reports")
    suspend fun uploadTaskReport(
        @Path("taskId") taskId: String,
        @Body request: UploadTaskReportRequest
    ): Response<UploadTaskReportResponse>


    @PATCH("tasks/{taskId}/complete")
    suspend fun completeTask(
        @Path("taskId") taskId: String,
        @Body request: CompleteTaskRequest = CompleteTaskRequest()
    ): Response<CompleteTaskResponse>

    @POST("projects/{projectCode}/issues")
    suspend fun createProjectIssue(
        @Path("projectCode") projectCode: String,
        @Body request: CreateIssueRequest
    ): Response<CreateIssueResponse>



    // ============================================================
    // DASHBOARD
    // ============================================================



    // ============================================================
    // TASKS
    // ============================================================

    @GET("tasks")
    suspend fun getTasks(

        @Header("Authorization")
        token: String,

        @Query("assignee_id")
        assigneeId: String? = null,

        @Query("project_id")
        projectId: String? = null

    ): Response<TasksResponse>


    // ============================================================
    // GET ALL PROJECTS
    // ============================================================

    @GET("projects")
    suspend fun getProjects(

        @Header("Authorization")
        token: String

    ): Response<ProjectsResponse>

    @GET("messages/conversations")
    suspend fun getConversations(): Response<ConversationsResponse>


    // ============================================================
    // GET ONE PROJECT
    // ============================================================

    @GET(
        "messages/conversations/{conversationId}"
    )
    suspend fun getMessages(

        @Path("conversationId")
        conversationId: Int

    ): Response<MessagesResponse>


    @POST("messages")
    suspend fun sendMessage(

        @Body request:
        SendMessageRequest

    ): Response<SendMessageResponse>

    @GET("projects/{code}")
    suspend fun getProjectByCode(

        @Header("Authorization")
        token: String,

        @Path("code")
        code: String

    ): Response<SingleProjectResponse>

    @GET("users/search")
    suspend fun searchUsers(
        @Query("q") query: String
    ): Response<UserSearchResponse>


    @POST("messages/conversations")
    suspend fun createConversation(
        @Body request: CreateConversationRequest
    ): Response<CreateConversationResponse>




    // ============================================================
    // JOIN PROJECT
    // ============================================================

    @POST("projects/join")
    suspend fun joinProject(

        @Header("Authorization")
        token: String,

        @Body request: JoinProjectRequest

    ): Response<JoinProjectResponse>


    // ============================================================
    // PROJECTS JOINED BY CURRENT USER
    // ============================================================

    @GET("projects/joined")
    suspend fun getJoinedProjects(

        @Header("Authorization")
        token: String

    ): Response<ProjectsResponse>

    @GET("dashboard")
    suspend fun getDashboard(
        @Header("Authorization") token: String
    ): Response<DashboardResponse>
    @GET("notifications")
    suspend fun getNotifications(
        @Header("Authorization") token: String
    ): Response<NotificationResponse>
}


// ============================================================
// TASKS API RESPONSE
//
// Backend:
//
// {
//     "success": true,
//     "data": [
//         {...}
//     ]
// }
// ============================================================
// ============================================================
// PROJECT DOCUMENTS API RESPONSE
//
// Backend:
//
// {
//     "success": true,
//     "data": [
//         {
//             "id": 1,
//             "name": "Structural Plan.pdf",
//             "type": "PDF",
//             "category": "Design & Engineering",
//             "uploaded_at": "2026-08-18T..."
//         }
//     ]
// }
// ============================================================

data class ProjectDocumentsResponse(

    val success: Boolean = false,

    val data: List<ProjectDocumentResponse> = emptyList()
)


// ============================================================
// PROJECT DOCUMENT RESPONSE
// ============================================================

data class ProjectDocumentResponse(

    val id: String? = null,

    val name: String? = null,

    val type: String? = null,

    val category: String? = null,

    @SerializedName("uploaded_at")
    val uploadedAt: String? = null
)
data class TasksResponse(

    val success: Boolean = false,

    val data: List<TaskResponse> = emptyList()
)


// ============================================================
// TASK RESPONSE
// ============================================================

data class TaskResponse(

    // PostgreSQL UUID
    val id: String,

    // Backend may return:
    // task_name OR title
    @SerializedName(
        value = "task_name",
        alternate = ["title"]
    )
    val title: String,

    val description: String? = null,

    val status: String? = null,

    @SerializedName("project_id")
    val projectId: String? = null,

    @SerializedName("project_code")
    val projectCode: String? = null,

    @SerializedName("assignee_id")
    val assigneeId: String? = null,

    @SerializedName(
        value = "project_name",
        alternate = ["project"]
    )
    val projectName: String? = null,

    @SerializedName(
        value = "assignee_name",
        alternate = ["assignee"]
    )
    val assigneeName: String? = null,

    @SerializedName(
        value = "due_date",
        alternate = ["dueDate"]
    )
    val dueDate: String? = null,

    val phase: String? = null,

    val priority: String? = null,

    @SerializedName(
        value = "progress_pct",
        alternate = ["progress"]
    )
    val progress: Int? = null
)