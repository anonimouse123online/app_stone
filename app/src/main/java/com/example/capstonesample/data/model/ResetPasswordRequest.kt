package com.example.capstonesample.data.model

data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String
)

data class ResetPasswordResponse(
    val success: Boolean = false,
    val message: String? = null
)