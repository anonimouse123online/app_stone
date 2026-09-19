package com.example.capstonesample.data.model

data class VerifyResetCodeRequest(
    val email: String,
    val code: String
)

data class VerifyResetCodeResponse(
    val success: Boolean = false,
    val message: String? = null
)