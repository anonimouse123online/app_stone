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
import com.example.capstonesample.security.PasswordUtils
import com.example.capstonesample.security.TokenManager

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
    // KEEPING YOUR ORIGINAL COLOR LOGIC
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
                shape = RoundedCornerShape(22.dp),

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
                    // EMAIL LABEL
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


                    // ============================================
                    // EMAIL FIELD
                    // ============================================

                    OutlinedTextField(

                        value =
                            username,

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
                    // PASSWORD LABEL
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


                    // ============================================
                    // PASSWORD FIELD
                    // ============================================

                    OutlinedTextField(

                        value =
                            password,

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
                                Modifier.clickable {

                                    if (!isLoading) {

                                        message =
                                            "Password recovery is not available yet."
                                    }
                                }
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
}