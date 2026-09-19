package com.example.capstonesample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.capstonesample.data.api.RetrofitClient
import com.example.capstonesample.data.local.AppDatabase
import com.example.capstonesample.data.model.LoginRequest
import com.example.capstonesample.data.model.ForgotPasswordRequest
import com.example.capstonesample.security.PasswordUtils
import com.example.capstonesample.security.TokenManager
import com.example.capstonesample.data.model.VerifyResetCodeRequest
import com.example.capstonesample.data.model.ResetPasswordRequest

import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException


// ============================================================
// LOGIN SCREEN
// ============================================================

@Composable
fun LoginScreen(

    onLoginClick: (
        token: String,
        fullName: String,
        email: String,
        role: String
    ) -> Unit,

    onSignupClick: () -> Unit = {}

) {

    // ============================================================
    // FORM STATE
    // ============================================================

    var username by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var rememberMe by remember {
        mutableStateOf(false)
    }

    var message by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    // Forgot password dialog
    var showForgotPassword by remember {
        mutableStateOf(false)
    }


    // ============================================================
    // CONTEXT / COROUTINE
    // ============================================================

    val scope =
        rememberCoroutineScope()

    val context =
        LocalContext.current


    // ============================================================
    // LOCAL ROOM DATABASE
    // ============================================================

    val database =
        remember {

            AppDatabase.getDatabase(
                context
            )
        }

    val userDao =
        remember {

            database.userDao()
        }


    // ============================================================
    // COLORS
    // ============================================================

    val orange =
        Color(0xFFF15A24)

    val headerBeige =
        Color(0xFFF1E2D9)

    val fieldBackground =
        Color(0xFFFAFAFA)

    val fieldBorder =
        Color(0xFFE8DFDA)

    val grayText =
        Color(0xFF818181)

    val blue =
        Color(0xFF007AFF)

    val screenBackground =
        Color(0xFFFDFCFB)

    val cardBorder =
        Color(0xFFF0EAE7)


    // ============================================================
    // MAIN UI
    // ============================================================

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBackground)
    ) {

        // ========================================================
        // TOP BACKGROUND
        // ========================================================

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .clip(
                    RoundedCornerShape(
                        bottomStart = 32.dp,
                        bottomEnd = 32.dp
                    )
                )
                .background(headerBeige)
        )


        // ========================================================
        // SCROLLABLE CONTENT
        // ========================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 20.dp
                )
        ) {

            Spacer(
                modifier = Modifier.height(32.dp)
            )


            // ====================================================
            // BRAND
            // ====================================================

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(
                            color = orange,
                            shape = RoundedCornerShape(50)
                        )
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = "SITEPULSE",
                    color = orange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
            }


            Spacer(
                modifier = Modifier.height(22.dp)
            )


            // ====================================================
            // WELCOME TEXT
            // ====================================================

            Text(
                text = "Welcome back",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B1B1B)
            )


            Spacer(
                modifier = Modifier.height(7.dp)
            )


            Text(
                text =
                    "Sign in to continue managing your field projects.",
                color = Color(0xFF6F6A67),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )


            Spacer(
                modifier = Modifier.height(27.dp)
            )


            // ====================================================
            // LOGIN CARD
            // ====================================================

            Card(
                modifier = Modifier
                    .fillMaxWidth(),

                shape =
                    RoundedCornerShape(22.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor = Color.White
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 5.dp
                    ),

                border =
                    androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = cardBorder
                    )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 20.dp,
                            vertical = 24.dp
                        )
                ) {

                    // ============================================
                    // LOGIN TITLE
                    // ============================================

                    Text(
                        text = "Sign in",
                        color = Color(0xFF1F1F1F),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )


                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )


                    Text(
                        text =
                            "Enter your account details below.",
                        color = grayText,
                        fontSize = 12.sp
                    )


                    Spacer(
                        modifier = Modifier.height(24.dp)
                    )


                    // ============================================
                    // EMAIL
                    // ============================================

                    Text(
                        text = "Username / Email",
                        color = Color(0xFF292929),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )


                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )


                    OutlinedTextField(

                        value = username,

                        onValueChange = {

                            username = it
                            message = ""
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),

                        placeholder = {

                            Text(
                                text =
                                    "Enter your email address",
                                color =
                                    Color(0xFF999999),
                                fontSize =
                                    13.sp
                            )
                        },

                        enabled =
                            !isLoading,

                        singleLine =
                            true,

                        shape =
                            RoundedCornerShape(14.dp),

                        colors =
                            OutlinedTextFieldDefaults.colors(

                                focusedContainerColor =
                                    fieldBackground,

                                unfocusedContainerColor =
                                    fieldBackground,

                                disabledContainerColor =
                                    fieldBackground,

                                focusedBorderColor =
                                    orange,

                                unfocusedBorderColor =
                                    fieldBorder,

                                disabledBorderColor =
                                    fieldBorder,

                                cursorColor =
                                    orange,

                                focusedTextColor =
                                    Color.Black,

                                unfocusedTextColor =
                                    Color.Black
                            )
                    )


                    Spacer(
                        modifier = Modifier.height(18.dp)
                    )


                    // ============================================
                    // PASSWORD
                    // ============================================

                    Text(
                        text = "Password",
                        color = Color(0xFF292929),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )


                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )


                    OutlinedTextField(

                        value = password,

                        onValueChange = {

                            password = it
                            message = ""
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),

                        placeholder = {

                            Text(
                                text =
                                    "Enter your password",
                                color =
                                    Color(0xFF999999),
                                fontSize =
                                    13.sp
                            )
                        },

                        enabled =
                            !isLoading,

                        visualTransformation =

                            if (passwordVisible) {

                                VisualTransformation.None

                            } else {

                                PasswordVisualTransformation()
                            },

                        trailingIcon = {

                            IconButton(

                                onClick = {

                                    passwordVisible =
                                        !passwordVisible
                                },

                                enabled =
                                    !isLoading

                            ) {

                                Icon(

                                    imageVector =

                                        if (passwordVisible) {

                                            Icons.Default.VisibilityOff

                                        } else {

                                            Icons.Default.Visibility
                                        },

                                    contentDescription =
                                        "Toggle password visibility",

                                    tint =
                                        Color(0xFF777777),

                                    modifier =
                                        Modifier.size(21.dp)
                                )
                            }
                        },

                        singleLine =
                            true,

                        shape =
                            RoundedCornerShape(14.dp),

                        colors =
                            OutlinedTextFieldDefaults.colors(

                                focusedContainerColor =
                                    fieldBackground,

                                unfocusedContainerColor =
                                    fieldBackground,

                                disabledContainerColor =
                                    fieldBackground,

                                focusedBorderColor =
                                    orange,

                                unfocusedBorderColor =
                                    fieldBorder,

                                disabledBorderColor =
                                    fieldBorder,

                                cursorColor =
                                    orange,

                                focusedTextColor =
                                    Color.Black,

                                unfocusedTextColor =
                                    Color.Black
                            )
                    )


                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )


                    // ============================================
                    // REMEMBER ME / FORGOT PASSWORD
                    // ============================================

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically,

                            modifier =
                                Modifier.clickable(
                                    enabled =
                                        !isLoading
                                ) {

                                    rememberMe =
                                        !rememberMe
                                }
                        ) {

                            Checkbox(

                                checked =
                                    rememberMe,

                                onCheckedChange = {

                                    rememberMe = it
                                },

                                enabled =
                                    !isLoading,

                                modifier =
                                    Modifier.size(20.dp),

                                colors =
                                    CheckboxDefaults.colors(

                                        checkedColor =
                                            orange,

                                        uncheckedColor =
                                            Color.Gray,

                                        checkmarkColor =
                                            Color.White
                                    )
                            )


                            Spacer(
                                modifier =
                                    Modifier.width(8.dp)
                            )


                            Text(
                                text =
                                    "Remember me",
                                fontSize =
                                    12.sp,
                                color =
                                    Color(0xFF444444)
                            )
                        }


                        Spacer(
                            modifier =
                                Modifier.weight(1f)
                        )


                        // ========================================
                        // FIXED FORGOT PASSWORD BUTTON
                        // ========================================

                        Text(
                            text =
                                "Forgot Password?",

                            fontSize =
                                12.sp,

                            color =
                                blue,

                            fontWeight =
                                FontWeight.SemiBold,

                            modifier =
                                Modifier
                                    .clickable(
                                        enabled =
                                            !isLoading
                                    ) {

                                        showForgotPassword =
                                            true
                                    }
                                    .padding(
                                        horizontal =
                                            4.dp,

                                        vertical =
                                            6.dp
                                    )
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(23.dp)
                    )


                    // ============================================
                    // SIGN IN BUTTON
                    // ============================================

                    Button(

                        onClick = {

                            // ====================================
                            // VALIDATE
                            // ====================================

                            if (
                                username.isBlank() ||
                                password.isBlank()
                            ) {

                                message =
                                    "Please enter your email and password."

                                return@Button
                            }


                            // ====================================
                            // LOGIN
                            // ====================================

                            scope.launch {

                                isLoading =
                                    true

                                message =
                                    ""


                                val cleanEmail =
                                    username
                                        .trim()
                                        .lowercase()


                                // ================================
                                // GET LOCAL USER
                                // ================================

                                val localUser =

                                    try {

                                        userDao.getUserByEmail(
                                            cleanEmail
                                        )

                                    } catch (
                                        e: Exception
                                    ) {

                                        e.printStackTrace()

                                        null
                                    }


                                // ================================
                                // TRY ONLINE LOGIN FIRST
                                // ================================

                                try {

                                    println(
                                        "===================================="
                                    )

                                    println(
                                        "🌐 TRYING ONLINE LOGIN"
                                    )

                                    println(
                                        "EMAIL = $cleanEmail"
                                    )

                                    println(
                                        "===================================="
                                    )


                                    val response =
                                        RetrofitClient.api.login(

                                            LoginRequest(
                                                email =
                                                    cleanEmail,

                                                password =
                                                    password
                                            )
                                        )


                                    println(
                                        "LOGIN HTTP = ${response.code()}"
                                    )

                                    println(
                                        "LOGIN SUCCESS = ${response.isSuccessful}"
                                    )


                                    // ============================
                                    // ONLINE LOGIN SUCCESS
                                    // ============================

                                    if (
                                        response.isSuccessful
                                    ) {

                                        val result =
                                            response.body()

                                        val jwt =
                                            result?.token

                                        val serverUser =
                                            result?.user


                                        println(
                                            "TOKEN EXISTS = ${!jwt.isNullOrBlank()}"
                                        )

                                        println(
                                            "SERVER USER = $serverUser"
                                        )


                                        if (
                                            !jwt.isNullOrBlank()
                                        ) {

                                            println(
                                                "✅ ONLINE LOGIN SUCCESS"
                                            )


                                            // ====================
                                            // SAVE JWT
                                            // ====================

                                            TokenManager.saveToken(
                                                context = context,
                                                token = jwt
                                            )


                                            println(
                                                "🔐 JWT TOKEN SAVED"
                                            )


                                            // ====================
                                            // MARK LOCAL USER SYNCED
                                            // ====================

                                            if (
                                                localUser != null
                                            ) {

                                                try {

                                                    userDao.markUserSynced(

                                                        email =
                                                            cleanEmail,

                                                        serverUserId =
                                                            serverUser?.id
                                                    )

                                                } catch (
                                                    e: Exception
                                                ) {

                                                    e.printStackTrace()

                                                    println(
                                                        "Failed to update local sync status: ${e.message}"
                                                    )
                                                }
                                            }


                                            // ====================
                                            // PROFILE INFORMATION
                                            // ====================

                                            val profileFullName =
                                                serverUser?.name
                                                    ?: localUser?.fullName
                                                    ?: ""

                                            val profileEmail =
                                                serverUser?.email
                                                    ?: localUser?.email
                                                    ?: cleanEmail

                                            val profileRole =
                                                serverUser?.role
                                                    ?: localUser?.role
                                                    ?: "engineer"


                                            println(
                                                "PROFILE NAME = $profileFullName"
                                            )

                                            println(
                                                "PROFILE EMAIL = $profileEmail"
                                            )

                                            println(
                                                "PROFILE ROLE = $profileRole"
                                            )


                                            // ====================
                                            // PASS TOKEN + PROFILE
                                            // ====================

                                            onLoginClick(
                                                jwt,
                                                profileFullName,
                                                profileEmail,
                                                profileRole
                                            )


                                            isLoading =
                                                false

                                            return@launch
                                        }


                                        message =
                                            "Login succeeded but the server did not return an authentication token."

                                        isLoading =
                                            false

                                        return@launch
                                    }


                                    // ============================
                                    // SERVER REPLIED WITH ERROR
                                    // ============================

                                    val serverError =

                                        try {

                                            response
                                                .errorBody()
                                                ?.string()

                                        } catch (
                                            _: Exception
                                        ) {

                                            null
                                        }


                                    println(
                                        "❌ SERVER LOGIN FAILED"
                                    )

                                    println(
                                        "HTTP = ${response.code()}"
                                    )

                                    println(
                                        "SERVER ERROR = $serverError"
                                    )


                                    message =

                                        when (
                                            response.code()
                                        ) {

                                            400 ->
                                                "Invalid login request."

                                            401 ->
                                                "Incorrect email or password."

                                            403 ->
                                                "Your account is not authorized."

                                            404 ->
                                                "Account was not found."

                                            500 ->
                                                "The server encountered an error."

                                            else ->
                                                "Login failed (${response.code()})."
                                        }


                                    isLoading =
                                        false

                                    return@launch


                                } catch (
                                    e: ConnectException
                                ) {

                                    println(
                                        "📴 SERVER CONNECTION REFUSED"
                                    )

                                    println(
                                        "TRYING OFFLINE LOGIN..."
                                    )


                                } catch (
                                    e: SocketTimeoutException
                                ) {

                                    println(
                                        "📴 SERVER TIMEOUT"
                                    )

                                    println(
                                        "TRYING OFFLINE LOGIN..."
                                    )


                                } catch (
                                    e: UnknownHostException
                                ) {

                                    println(
                                        "📴 NO NETWORK / SERVER HOST"
                                    )

                                    println(
                                        "TRYING OFFLINE LOGIN..."
                                    )


                                } catch (
                                    e: Exception
                                ) {

                                    e.printStackTrace()

                                    message =
                                        "Login error: ${
                                            e.message
                                                ?: "Unknown error"
                                        }"

                                    isLoading =
                                        false

                                    return@launch
                                }


                                // ================================
                                // OFFLINE LOGIN
                                // ================================

                                if (
                                    localUser == null
                                ) {

                                    message =
                                        "No offline account is saved on this device."

                                    isLoading =
                                        false

                                    return@launch
                                }


                                try {

                                    val enteredHash =
                                        PasswordUtils.hashPassword(
                                            password
                                        )


                                    if (
                                        enteredHash ==
                                        localUser.passwordHash
                                    ) {

                                        println(
                                            "✅ OFFLINE LOGIN SUCCESS"
                                        )


                                        onLoginClick(

                                            "LOCAL_${localUser.id}",

                                            localUser.fullName,

                                            localUser.email,

                                            localUser.role
                                        )


                                    } else {

                                        message =
                                            "Incorrect email or password."
                                    }


                                } catch (
                                    e: Exception
                                ) {

                                    e.printStackTrace()

                                    message =
                                        "Unable to verify offline login."
                                }


                                isLoading =
                                    false
                            }
                        },

                        enabled =
                            !isLoading,

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),

                        shape =
                            RoundedCornerShape(14.dp),

                        colors =
                            ButtonDefaults.buttonColors(

                                containerColor =
                                    orange,

                                disabledContainerColor =
                                    orange.copy(
                                        alpha = 0.6f
                                    )
                            )

                    ) {

                        if (
                            isLoading
                        ) {

                            CircularProgressIndicator(

                                modifier =
                                    Modifier.size(20.dp),

                                strokeWidth =
                                    2.dp,

                                color =
                                    Color.White
                            )

                        } else {

                            Text(
                                text =
                                    "Sign In",
                                color =
                                    Color.White,
                                fontSize =
                                    15.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }


                    // ============================================
                    // MESSAGE
                    // ============================================

                    if (
                        message.isNotEmpty()
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )


                        Surface(
                            modifier =
                                Modifier.fillMaxWidth(),

                            color =

                                if (
                                    message.contains(
                                        "Invalid",
                                        ignoreCase = true
                                    ) ||

                                    message.contains(
                                        "Incorrect",
                                        ignoreCase = true
                                    ) ||

                                    message.contains(
                                        "failed",
                                        ignoreCase = true
                                    ) ||

                                    message.contains(
                                        "Unable",
                                        ignoreCase = true
                                    ) ||

                                    message.contains(
                                        "not found",
                                        ignoreCase = true
                                    ) ||

                                    message.contains(
                                        "not authorized",
                                        ignoreCase = true
                                    )
                                ) {

                                    Color(0xFFFFF1F0)

                                } else {

                                    Color(0xFFF7F7F7)
                                },

                            shape =
                                RoundedCornerShape(10.dp)
                        ) {

                            Text(

                                text =
                                    message,

                                modifier =
                                    Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 10.dp
                                    ),

                                color =

                                    if (
                                        message.contains(
                                            "Invalid",
                                            ignoreCase = true
                                        ) ||

                                        message.contains(
                                            "Incorrect",
                                            ignoreCase = true
                                        ) ||

                                        message.contains(
                                            "failed",
                                            ignoreCase = true
                                        ) ||

                                        message.contains(
                                            "Unable",
                                            ignoreCase = true
                                        ) ||

                                        message.contains(
                                            "not found",
                                            ignoreCase = true
                                        ) ||

                                        message.contains(
                                            "not authorized",
                                            ignoreCase = true
                                        )
                                    ) {

                                        Color(0xFFD32F2F)

                                    } else {

                                        grayText
                                    },

                                fontSize =
                                    11.sp,

                                lineHeight =
                                    16.sp
                            )
                        }
                    }


                    Spacer(
                        modifier =
                            Modifier.height(24.dp)
                    )


                    // ============================================
                    // DIVIDER
                    // ============================================

                    HorizontalDivider(
                        color =
                            Color(0xFFF0EBE8)
                    )


                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )


                    // ============================================
                    // SIGN UP
                    // ============================================

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.Center,

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text =
                                "Don’t have an account? ",
                            color =
                                Color(0xFF666666),
                            fontSize =
                                12.sp
                        )


                        Text(

                            text =
                                "Create account",

                            color =
                                blue,

                            fontSize =
                                12.sp,

                            fontWeight =
                                FontWeight.Bold,

                            modifier =
                                Modifier.clickable {

                                    if (
                                        !isLoading
                                    ) {

                                        onSignupClick()
                                    }
                                }
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )


            // ====================================================
            // SECURITY INFORMATION
            // ====================================================

            Surface(
                modifier =
                    Modifier.fillMaxWidth(),

                color =
                    Color(0xFFF8F3F1),

                shape =
                    RoundedCornerShape(16.dp),

                border =
                    androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = Color(0xFFF1E6E1)
                    )
            ) {

                Row(
                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 14.dp
                        ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(

                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                orange.copy(
                                    alpha = 0.12f
                                ),
                                RoundedCornerShape(
                                    10.dp
                                )
                            ),

                        contentAlignment =
                            Alignment.Center

                    ) {

                        Icon(

                            imageVector =
                                Icons.Outlined.Security,

                            contentDescription =
                                null,

                            tint =
                                orange,

                            modifier =
                                Modifier.size(19.dp)
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.width(12.dp)
                    )


                    Column {

                        Text(
                            text =
                                "Secure access",
                            color =
                                Color(0xFF343434),
                            fontSize =
                                12.sp,
                            fontWeight =
                                FontWeight.SemiBold
                        )


                        Spacer(
                            modifier =
                                Modifier.height(3.dp)
                        )


                        Text(

                            text =
                                "Online authentication is used when the server is available. Offline access uses your secure local account.",

                            color =
                                Color(0xFF858585),

                            fontSize =
                                10.sp,

                            lineHeight =
                                14.sp
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(28.dp)
            )
        }
    }


    // ============================================================
    // FORGOT PASSWORD DIALOG
    // ============================================================

    if (
        showForgotPassword
    ) {

        ForgotPasswordDialog(

            onDismiss = {

                showForgotPassword =
                    false
            },

            onPasswordReset = {

                showForgotPassword =
                    false

                message =
                    "Password changed successfully. You can now sign in."
            }
        )
    }
}


// ============================================================
// FORGOT PASSWORD DIALOG
// ============================================================

@Composable
fun ForgotPasswordDialog(
    onDismiss: () -> Unit,
    onPasswordReset: () -> Unit
) {

    // ============================================================
    // STEPS
    // 1 = EMAIL
    // 2 = OTP
    // 3 = NEW PASSWORD
    // ============================================================

    var step by remember {
        mutableStateOf(1)
    }

    var email by remember {
        mutableStateOf("")
    }

    var otp by remember {
        mutableStateOf("")
    }

    var newPassword by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    var newPasswordVisible by remember {
        mutableStateOf(false)
    }

    var confirmPasswordVisible by remember {
        mutableStateOf(false)
    }

    var message by remember {
        mutableStateOf("")
    }

    var isError by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }


    val scope =
        rememberCoroutineScope()


    val orange =
        Color(0xFFF15A24)

    val fieldBackground =
        Color(0xFFFAFAFA)

    val fieldBorder =
        Color(0xFFE8DFDA)


    AlertDialog(

        onDismissRequest = {

            if (!isLoading) {
                onDismiss()
            }
        },

        containerColor =
            Color.White,

        shape =
            RoundedCornerShape(22.dp),


        // ========================================================
        // TITLE
        // ========================================================

        title = {

            Column {

                Text(
                    text =
                        when (step) {

                            1 ->
                                "Forgot Password?"

                            2 ->
                                "Verify Code"

                            else ->
                                "Create New Password"
                        },

                    fontSize =
                        23.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        Color(0xFF1B1B1B)
                )


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                Text(
                    text =
                        when (step) {

                            1 ->
                                "Enter the email address registered with your SitePulse account."

                            2 ->
                                "Enter the 6-digit verification code sent to $email."

                            else ->
                                "Enter a new password for your SitePulse account."
                        },

                    fontSize =
                        13.sp,

                    lineHeight =
                        19.sp,

                    color =
                        Color(0xFF777777),

                    fontWeight =
                        FontWeight.Normal
                )
            }
        },


        // ========================================================
        // CONTENT
        // ========================================================

        text = {

            Column(
                modifier =
                    Modifier.fillMaxWidth()
            ) {


                // ====================================================
                // STEP 1 - EMAIL
                // ====================================================

                if (
                    step == 1
                ) {

                    Text(
                        text =
                            "Email Address",

                        fontSize =
                            13.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        color =
                            Color(0xFF292929)
                    )


                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )


                    OutlinedTextField(

                        value =
                            email,

                        onValueChange = {

                            email = it

                            message = ""
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        placeholder = {

                            Text(
                                text =
                                    "Enter your email address"
                            )
                        },

                        enabled =
                            !isLoading,

                        singleLine =
                            true,

                        shape =
                            RoundedCornerShape(14.dp),

                        colors =
                            OutlinedTextFieldDefaults.colors(

                                focusedContainerColor =
                                    fieldBackground,

                                unfocusedContainerColor =
                                    fieldBackground,

                                focusedBorderColor =
                                    orange,

                                unfocusedBorderColor =
                                    fieldBorder,

                                cursorColor =
                                    orange
                            )
                    )
                }


                // ====================================================
                // STEP 2 - OTP
                // ====================================================

                if (
                    step == 2
                ) {

                    Text(
                        text =
                            "Verification Code",

                        fontSize =
                            13.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        color =
                            Color(0xFF292929)
                    )


                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )


                    OutlinedTextField(

                        value =
                            otp,

                        onValueChange = {

                            // Only allow numbers
                            if (
                                it.length <= 6 &&
                                it.all { char ->
                                    char.isDigit()
                                }
                            ) {

                                otp = it
                            }

                            message = ""
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        placeholder = {

                            Text(
                                text =
                                    "Enter 6-digit code"
                            )
                        },

                        enabled =
                            !isLoading,

                        singleLine =
                            true,

                        shape =
                            RoundedCornerShape(14.dp),

                        colors =
                            OutlinedTextFieldDefaults.colors(

                                focusedContainerColor =
                                    fieldBackground,

                                unfocusedContainerColor =
                                    fieldBackground,

                                focusedBorderColor =
                                    orange,

                                unfocusedBorderColor =
                                    fieldBorder,

                                cursorColor =
                                    orange
                            )
                    )


                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )


                    Text(
                        text =
                            "The verification code expires after 10 minutes.",

                        fontSize =
                            11.sp,

                        color =
                            Color(0xFF777777)
                    )
                }


                // ====================================================
                // STEP 3 - NEW PASSWORD
                // ====================================================

                if (
                    step == 3
                ) {

                    Text(
                        text =
                            "New Password",

                        fontSize =
                            13.sp,

                        fontWeight =
                            FontWeight.SemiBold
                    )


                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )


                    OutlinedTextField(

                        value =
                            newPassword,

                        onValueChange = {

                            newPassword = it

                            message = ""
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        placeholder = {

                            Text(
                                text =
                                    "Enter new password"
                            )
                        },

                        enabled =
                            !isLoading,

                        singleLine =
                            true,

                        visualTransformation =

                            if (
                                newPasswordVisible
                            ) {

                                VisualTransformation.None

                            } else {

                                PasswordVisualTransformation()
                            },

                        trailingIcon = {

                            IconButton(

                                onClick = {

                                    newPasswordVisible =
                                        !newPasswordVisible
                                }

                            ) {

                                Icon(

                                    imageVector =

                                        if (
                                            newPasswordVisible
                                        ) {

                                            Icons.Default.VisibilityOff

                                        } else {

                                            Icons.Default.Visibility
                                        },

                                    contentDescription =
                                        "Show password"
                                )
                            }
                        },

                        shape =
                            RoundedCornerShape(14.dp),

                        colors =
                            OutlinedTextFieldDefaults.colors(

                                focusedBorderColor =
                                    orange,

                                unfocusedBorderColor =
                                    fieldBorder,

                                cursorColor =
                                    orange
                            )
                    )


                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )


                    Text(
                        text =
                            "Confirm New Password",

                        fontSize =
                            13.sp,

                        fontWeight =
                            FontWeight.SemiBold
                    )


                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )


                    OutlinedTextField(

                        value =
                            confirmPassword,

                        onValueChange = {

                            confirmPassword = it

                            message = ""
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        placeholder = {

                            Text(
                                text =
                                    "Confirm new password"
                            )
                        },

                        enabled =
                            !isLoading,

                        singleLine =
                            true,

                        visualTransformation =

                            if (
                                confirmPasswordVisible
                            ) {

                                VisualTransformation.None

                            } else {

                                PasswordVisualTransformation()
                            },

                        trailingIcon = {

                            IconButton(

                                onClick = {

                                    confirmPasswordVisible =
                                        !confirmPasswordVisible
                                }

                            ) {

                                Icon(

                                    imageVector =

                                        if (
                                            confirmPasswordVisible
                                        ) {

                                            Icons.Default.VisibilityOff

                                        } else {

                                            Icons.Default.Visibility
                                        },

                                    contentDescription =
                                        "Show password"
                                )
                            }
                        },

                        shape =
                            RoundedCornerShape(14.dp),

                        colors =
                            OutlinedTextFieldDefaults.colors(

                                focusedBorderColor =
                                    orange,

                                unfocusedBorderColor =
                                    fieldBorder,

                                cursorColor =
                                    orange
                            )
                    )


                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )


                    Text(
                        text =
                            "Password must contain at least 8 characters.",

                        fontSize =
                            11.sp,

                        color =
                            Color(0xFF777777)
                    )
                }


                // ====================================================
                // STATUS MESSAGE
                // ====================================================

                if (
                    message.isNotBlank()
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(14.dp)
                    )


                    Surface(

                        modifier =
                            Modifier.fillMaxWidth(),

                        color =

                            if (
                                isError
                            ) {

                                Color(0xFFFFF1F0)

                            } else {

                                Color(0xFFF1F8F3)
                            },

                        shape =
                            RoundedCornerShape(10.dp)

                    ) {

                        Text(

                            text =
                                message,

                            modifier =
                                Modifier.padding(10.dp),

                            color =

                                if (
                                    isError
                                ) {

                                    Color(0xFFD32F2F)

                                } else {

                                    Color(0xFF2E7D32)
                                },

                            fontSize =
                                12.sp
                        )
                    }
                }
            }
        },


        // ========================================================
        // MAIN BUTTON
        // ========================================================

        confirmButton = {

            Button(

                enabled =
                    !isLoading,

                shape =
                    RoundedCornerShape(12.dp),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            orange
                    ),

                onClick = {


                    // =================================================
                    // STEP 1 - SEND OTP
                    // =================================================

                    if (
                        step == 1
                    ) {

                        val cleanEmail =
                            email
                                .trim()
                                .lowercase()


                        if (
                            cleanEmail.isBlank()
                        ) {

                            isError =
                                true

                            message =
                                "Please enter your email address."

                            return@Button
                        }


                        if (
                            !android.util.Patterns.EMAIL_ADDRESS
                                .matcher(
                                    cleanEmail
                                )
                                .matches()
                        ) {

                            isError =
                                true

                            message =
                                "Please enter a valid email address."

                            return@Button
                        }


                        scope.launch {

                            isLoading =
                                true

                            message =
                                ""


                            try {

                                val response =
                                    RetrofitClient.api
                                        .forgotPassword(

                                            ForgotPasswordRequest(
                                                email =
                                                    cleanEmail
                                            )
                                        )


                                if (
                                    response.isSuccessful
                                ) {

                                    email =
                                        cleanEmail

                                    step =
                                        2

                                    isError =
                                        false

                                    message =
                                        "Verification code sent successfully."


                                } else {

                                    isError =
                                        true

                                    message =

                                        when (
                                            response.code()
                                        ) {

                                            404 ->
                                                "Account not found."

                                            429 ->
                                                "Too many requests. Please try again later."

                                            else ->
                                                "Unable to send verification code."
                                        }
                                }


                            } catch (
                                e: Exception
                            ) {

                                e.printStackTrace()

                                isError =
                                    true

                                message =
                                    "Unable to connect to the server."

                            } finally {

                                isLoading =
                                    false
                            }
                        }
                    }


                    // =================================================
                    // STEP 2 - VERIFY OTP
                    // =================================================

                    else if (
                        step == 2
                    ) {

                        if (
                            otp.length != 6
                        ) {

                            isError =
                                true

                            message =
                                "Please enter the 6-digit verification code."

                            return@Button
                        }


                        scope.launch {

                            isLoading =
                                true

                            message =
                                ""


                            try {

                                val response =
                                    RetrofitClient.api
                                        .verifyResetCode(

                                            VerifyResetCodeRequest(

                                                email =
                                                    email,

                                                code =
                                                    otp
                                            )
                                        )


                                if (
                                    response.isSuccessful
                                ) {

                                    step =
                                        3

                                    isError =
                                        false

                                    message =
                                        "Code verified successfully."


                                } else {

                                    isError =
                                        true

                                    message =

                                        response
                                            .errorBody()
                                            ?.string()
                                            ?.takeIf {
                                                it.isNotBlank()
                                            }
                                            ?: "Incorrect or expired verification code."
                                }


                            } catch (
                                e: Exception
                            ) {

                                e.printStackTrace()

                                isError =
                                    true

                                message =
                                    "Unable to verify the code."

                            } finally {

                                isLoading =
                                    false
                            }
                        }
                    }


                    // =================================================
                    // STEP 3 - RESET PASSWORD
                    // =================================================

                    else {

                        if (
                            newPassword.length < 8
                        ) {

                            isError =
                                true

                            message =
                                "Password must contain at least 8 characters."

                            return@Button
                        }


                        if (
                            newPassword !=
                            confirmPassword
                        ) {

                            isError =
                                true

                            message =
                                "Passwords do not match."

                            return@Button
                        }


                        scope.launch {

                            isLoading =
                                true

                            message =
                                ""


                            try {

                                val response =
                                    RetrofitClient.api
                                        .resetPassword(

                                            ResetPasswordRequest(

                                                email =
                                                    email,

                                                code =
                                                    otp,

                                                newPassword =
                                                    newPassword
                                            )
                                        )


                                if (
                                    response.isSuccessful
                                ) {

                                    isError =
                                        false

                                    message =
                                        "Password successfully changed."


                                    kotlinx.coroutines.delay(
                                        1000
                                    )


                                    onPasswordReset()


                                } else {

                                    isError =
                                        true

                                    message =
                                        "Unable to reset your password."
                                }


                            } catch (
                                e: Exception
                            ) {

                                e.printStackTrace()

                                isError =
                                    true

                                message =
                                    "Unable to connect to the server."

                            } finally {

                                isLoading =
                                    false
                            }
                        }
                    }
                }

            ) {


                if (
                    isLoading
                ) {

                    CircularProgressIndicator(

                        modifier =
                            Modifier.size(18.dp),

                        strokeWidth =
                            2.dp,

                        color =
                            Color.White
                    )


                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )
                }


                Text(

                    text =

                        if (
                            isLoading
                        ) {

                            "Please wait..."

                        } else {

                            when (
                                step
                            ) {

                                1 ->
                                    "Send Code"

                                2 ->
                                    "Verify Code"

                                else ->
                                    "Reset Password"
                            }
                        },

                    color =
                        Color.White,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        },


        // ========================================================
        // CANCEL / BACK
        // ========================================================

        dismissButton = {

            TextButton(

                enabled =
                    !isLoading,

                onClick = {

                    if (
                        step == 1
                    ) {

                        onDismiss()

                    } else {

                        step =
                            step - 1

                        message =
                            ""
                    }
                }

            ) {

                Text(
                    text =

                        if (
                            step == 1
                        ) {

                            "Cancel"

                        } else {

                            "Back"
                        },

                    color =
                        Color(0xFF666666)
                )
            }
        }
    )
}
