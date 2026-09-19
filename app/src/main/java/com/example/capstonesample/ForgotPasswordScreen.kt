package com.example.capstonesample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstonesample.data.api.RetrofitClient
import com.example.capstonesample.data.model.ForgotPasswordRequest
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit
) {

    var email by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

    val orange = Color(0xFFF15A24)
    val background = Color(0xFFFDFCFB)
    val fieldBackground = Color(0xFFFAFAFA)
    val fieldBorder = Color(0xFFE8DFDA)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            // BACK BUTTON
            IconButton(
                onClick = onBackClick
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = "Forgot Password?",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B1B1B)
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Enter your email address and we'll send you instructions to reset your password.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFF6F6A67)
            )

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 5.dp
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {

                    Text(
                        text = "Email Address",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF292929)
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    OutlinedTextField(
                        value = email,

                        onValueChange = {
                            email = it
                            message = ""
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),

                        placeholder = {
                            Text(
                                text = "Enter your email address",
                                fontSize = 13.sp
                            )
                        },

                        enabled = !isLoading,

                        singleLine = true,

                        shape = RoundedCornerShape(14.dp),

                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = fieldBackground,
                            unfocusedContainerColor = fieldBackground,
                            focusedBorderColor = orange,
                            unfocusedBorderColor = fieldBorder,
                            cursorColor = orange
                        )
                    )

                    Spacer(
                        modifier = Modifier.height(22.dp)
                    )

                    Button(
                        onClick = {

                            if (email.isBlank()) {
                                message = "Please enter your email address."
                                return@Button
                            }

                            scope.launch {

                                isLoading = true
                                message = ""

                                val cleanEmail =
                                    email.trim().lowercase()

                                try {

                                    val response =
                                        RetrofitClient.api.forgotPassword(
                                            ForgotPasswordRequest(
                                                email = cleanEmail
                                            )
                                        )

                                    if (response.isSuccessful) {

                                        message =
                                            response.body()?.message
                                                ?: "Password reset instructions have been sent to your email."

                                    } else {

                                        message =
                                            when (response.code()) {

                                                400 ->
                                                    "Please enter a valid email address."

                                                404 ->
                                                    "Account was not found."

                                                429 ->
                                                    "Too many attempts. Please try again later."

                                                500 ->
                                                    "Server error. Please try again later."

                                                else ->
                                                    "Unable to send reset request (${response.code()})."
                                            }
                                    }

                                } catch (e: ConnectException) {

                                    message =
                                        "Unable to connect to the server."

                                } catch (e: SocketTimeoutException) {

                                    message =
                                        "Server connection timed out."

                                } catch (e: UnknownHostException) {

                                    message =
                                        "No internet connection."

                                } catch (e: Exception) {

                                    e.printStackTrace()

                                    message =
                                        "Error: ${e.message ?: "Unknown error"}"
                                }

                                isLoading = false
                            }
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),

                        enabled = !isLoading,

                        shape = RoundedCornerShape(14.dp),

                        colors = ButtonDefaults.buttonColors(
                            containerColor = orange
                        )
                    ) {

                        if (isLoading) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )

                        } else {

                            Text(
                                text = "Send Reset Link",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (message.isNotBlank()) {

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        Text(
                            text = message,
                            fontSize = 12.sp,
                            color =
                                if (
                                    message.contains("sent", true)
                                ) {
                                    Color(0xFF2E7D32)
                                } else {
                                    Color(0xFFD32F2F)
                                }
                        )
                    }
                }
            }
        }
    }
}