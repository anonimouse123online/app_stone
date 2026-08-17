package com.example.capstonesample.data.api

import com.example.capstonesample.data.model.JoinProjectRequest
import com.example.capstonesample.data.model.JoinProjectResponse
import com.example.capstonesample.data.model.LoginRequest
import com.example.capstonesample.data.model.LoginResponse
import com.example.capstonesample.data.model.ProjectResponse
import com.example.capstonesample.data.model.SignupRequest
import com.example.capstonesample.data.model.SignupResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST


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


    // ============================================================
    // TASKS
    // ============================================================

    @GET("tasks")
    suspend fun getTasks(
        @Header("Authorization") token: String
    ): Response<List<TaskResponse>>


    // ============================================================
    // PROJECTS
    // ============================================================

    @GET("projects")
    suspend fun getProjects(
        @Header("Authorization") token: String
    ): Response<List<ProjectResponse>>


    // ============================================================
    // JOIN PROJECT USING ADMIN INVITE CODE
    // ============================================================

    @POST("projects/join")
    suspend fun joinProject(
        @Header("Authorization") token: String,
        @Body request: JoinProjectRequest
    ): Response<JoinProjectResponse>
}


// ============================================================
// TASK RESPONSE
// ============================================================

data class TaskResponse(
    val id: Int,
    val title: String,
    val description: String?,
    val status: String?
)