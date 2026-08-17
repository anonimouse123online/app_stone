package com.example.capstonesample.data.model


// ============================================================
// LOGIN
// ============================================================

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val message: String?,
    val token: String?,
    val redirectTo: String?,
    val user: UserResponse?
)

data class UserResponse(

    // Backend uses UUID, NOT Int
    val id: String?,

    val name: String?,
    val email: String?,
    val role: String?
)


// ============================================================
// SIGNUP
// ============================================================

data class SignupRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String = "Engineer"
)

data class SignupResponse(
    val message: String?,
    val user: SignupUser?
)

data class SignupUser(

    // Backend uses UUID
    val id: String?,

    val name: String?,
    val email: String?,
    val role: String?
)