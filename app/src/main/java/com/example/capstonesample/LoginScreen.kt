package com.example.capstonesample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.capstonesample.data.model.SignupRequest
import com.example.capstonesample.security.PasswordUtils

import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException


@Composable
fun LoginScreen(
    onLoginClick: (String) -> Unit,
    onSignupClick: () -> Unit = {}
) {

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


    val scope =
        rememberCoroutineScope()

    val context =
        LocalContext.current

    val database =
        remember {
            AppDatabase.getDatabase(context)
        }

    val userDao =
        remember {
            database.userDao()
        }


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


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        Column(
            modifier =
                Modifier.fillMaxSize()
        ) {


            // ============================================================
            // HEADER
            // ============================================================

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color =
                            headerBeige,

                        shape =
                            RoundedCornerShape(
                                bottomStart = 24.dp,
                                bottomEnd = 24.dp
                            )
                    )
                    .padding(
                        start = 22.dp,
                        end = 22.dp,
                        top = 22.dp,
                        bottom = 24.dp
                    )
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                orange,
                                RoundedCornerShape(50)
                            )
                    )

                    Spacer(
                        modifier =
                            Modifier.width(7.dp)
                    )

                    Text(
                        text = "SITEPULSE",
                        color = orange,
                        fontSize = 12.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(7.dp)
                )


                Text(
                    text =
                        "Welcome back !",

                    fontSize =
                        27.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        Color.Black
                )


                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )


                Text(
                    text =
                        "Real Time Field-Tracking and Issue Reporting",

                    color =
                        Color.Gray,

                    fontSize =
                        13.sp
                )
            }


            // ============================================================
            // LOGIN CONTENT
            // ============================================================

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(
                        horizontal = 22.dp
                    )
            ) {

                Spacer(
                    modifier =
                        Modifier.height(106.dp)
                )


                // ========================================================
                // EMAIL
                // ========================================================

                Text(
                    text =
                        "Username / Email",

                    color =
                        Color.Black,

                    fontSize =
                        13.sp,

                    fontWeight =
                        FontWeight.SemiBold
                )


                Spacer(
                    modifier =
                        Modifier.height(7.dp)
                )


                OutlinedTextField(
                    value =
                        username,

                    onValueChange = {
                        username = it
                        message = ""
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),

                    placeholder = {

                        Text(
                            text =
                                "Enter your email",

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
                        RoundedCornerShape(10.dp),

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
                                orange,

                            focusedTextColor =
                                Color.Black,

                            unfocusedTextColor =
                                Color.Black
                        )
                )


                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )


                // ========================================================
                // PASSWORD
                // ========================================================

                Text(
                    text =
                        "Password",

                    color =
                        Color.Black,

                    fontSize =
                        13.sp,

                    fontWeight =
                        FontWeight.SemiBold
                )


                Spacer(
                    modifier =
                        Modifier.height(7.dp)
                )


                OutlinedTextField(
                    value =
                        password,

                    onValueChange = {
                        password = it
                        message = ""
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),

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

                        if (passwordVisible)

                            VisualTransformation.None

                        else

                            PasswordVisualTransformation(),

                    trailingIcon = {

                        IconButton(
                            onClick = {
                                passwordVisible =
                                    !passwordVisible
                            }
                        ) {

                            Icon(
                                imageVector =

                                    if (passwordVisible)

                                        Icons.Default.VisibilityOff

                                    else

                                        Icons.Default.Visibility,

                                contentDescription =
                                    "Toggle password",

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
                        RoundedCornerShape(10.dp),

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
                                orange,

                            focusedTextColor =
                                Color.Black,

                            unfocusedTextColor =
                                Color.Black
                        )
                )


                Spacer(
                    modifier =
                        Modifier.height(13.dp)
                )


                // ========================================================
                // REMEMBER / FORGOT
                // ========================================================

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
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
                            Modifier.width(7.dp)
                    )


                    Text(
                        text =
                            "Remember me",

                        fontSize =
                            12.sp,

                        color =
                            Color.Black
                    )


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
                            FontWeight.Medium,

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
                        Modifier.height(18.dp)
                )


                // ========================================================
                // SIGN IN BUTTON
                // ========================================================

                Button(
                    onClick = {

                        if (
                            username.isBlank() ||
                            password.isBlank()
                        ) {

                            message =
                                "Please enter your email and password."

                            return@Button
                        }


                        scope.launch {

                            isLoading = true
                            message = ""


                            val cleanEmail =
                                username
                                    .trim()
                                    .lowercase()


                            // ====================================================
                            // STEP 1
                            // GET LOCAL USER
                            // ====================================================

                            val localUser =
                                try {

                                    userDao.getUserByEmail(
                                        cleanEmail
                                    )

                                } catch (e: Exception) {

                                    e.printStackTrace()

                                    null
                                }


                            // ====================================================
                            // STEP 2
                            // VERIFY LOCAL PASSWORD IF USER EXISTS
                            // ====================================================

                            if (localUser != null) {

                                val enteredHash =
                                    PasswordUtils.hashPassword(
                                        password
                                    )

                                if (
                                    enteredHash !=
                                    localUser.passwordHash
                                ) {

                                    message =
                                        "Invalid email or password."

                                    isLoading =
                                        false

                                    return@launch
                                }
                            }


                            // ====================================================
                            // STEP 3
                            // SYNC LOCAL ACCOUNT IF NEEDED
                            // ====================================================

                            if (
                                localUser != null &&
                                !localUser.isSynced
                            ) {

                                try {

                                    val signupResponse =
                                        RetrofitClient.api.signup(

                                            SignupRequest(
                                                name =
                                                    localUser.fullName,

                                                email =
                                                    cleanEmail,

                                                password =
                                                    password,

                                                role =
                                                    "Engineer"
                                            )
                                        )


                                    println(
                                        "SIGNUP HTTP = ${signupResponse.code()}"
                                    )


                                    if (
                                        signupResponse.isSuccessful
                                    ) {

                                        val serverUser =
                                            signupResponse
                                                .body()
                                                ?.user


                                        userDao.markUserSynced(
                                            email =
                                                cleanEmail,

                                            serverUserId =
                                                serverUser?.id
                                        )


                                        println(
                                            "✅ ACCOUNT SYNCED"
                                        )

                                    } else {

                                        val signupError =
                                            try {

                                                signupResponse
                                                    .errorBody()
                                                    ?.string()

                                            } catch (e: Exception) {

                                                null
                                            }


                                        println(
                                            "SIGNUP SERVER RESPONSE = $signupError"
                                        )

                                        // 409 can simply mean the user
                                        // already exists on the backend.
                                        //
                                        // Continue to /auth/login.
                                    }


                                } catch (e: Exception) {

                                    println(
                                        "SIGNUP CONNECTION ERROR = ${e.message}"
                                    )

                                    // Do not return.
                                    // Login below will determine whether
                                    // backend is actually reachable.
                                }
                            }


                            // ====================================================
                            // STEP 4
                            // SERVER LOGIN
                            // ====================================================

                            try {

                                println(
                                    "===================================="
                                )

                                println(
                                    "🌐 ATTEMPTING SERVER LOGIN"
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
                                    "========== SERVER LOGIN =========="
                                )

                                println(
                                    "HTTP STATUS = ${response.code()}"
                                )

                                println(
                                    "SUCCESS = ${response.isSuccessful}"
                                )


                                // ================================================
                                // LOGIN SUCCESS
                                // ================================================

                                if (
                                    response.isSuccessful
                                ) {

                                    val result =
                                        response.body()

                                    val jwt =
                                        result?.token


                                    println(
                                        "TOKEN EXISTS = ${!jwt.isNullOrBlank()}"
                                    )

                                    println(
                                        "SERVER USER = ${result?.user}"
                                    )


                                    if (
                                        !jwt.isNullOrBlank()
                                    ) {

                                        println(
                                            "✅ REAL SERVER JWT RECEIVED"
                                        )

                                        println(
                                            "✅ ONLINE LOGIN"
                                        )


                                        // Mark local user as synced
                                        if (
                                            localUser != null
                                        ) {

                                            try {

                                                userDao.markUserSynced(
                                                    email =
                                                        cleanEmail,

                                                    serverUserId =
                                                        result
                                                            ?.user
                                                            ?.id
                                                )

                                            } catch (
                                                e: Exception
                                            ) {

                                                e.printStackTrace()
                                            }
                                        }


                                        // ========================================
                                        // REAL JWT GOES TO MAIN ACTIVITY
                                        // ========================================

                                        onLoginClick(
                                            jwt
                                        )


                                        isLoading =
                                            false

                                        return@launch
                                    }


                                    message =
                                        "Server login succeeded but no authentication token was returned."

                                    isLoading =
                                        false

                                    return@launch
                                }


                                // ================================================
                                // SERVER REPLIED WITH AN ERROR
                                // ================================================

                                val serverError =
                                    try {

                                        response
                                            .errorBody()
                                            ?.string()

                                    } catch (
                                        e: Exception
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
                                    "ERROR = $serverError"
                                )


                                // HTTP error means backend was reached.
                                // DO NOT call this "offline".

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
                                            "Account not found on the server."

                                        500 ->
                                            "The server encountered an error."

                                        else ->
                                            "Server login failed (${response.code()})."
                                    }


                                isLoading =
                                    false

                                return@launch


                            } catch (
                                e: ConnectException
                            ) {

                                // ================================================
                                // SERVER CONNECTION REFUSED
                                // ================================================

                                e.printStackTrace()

                                println(
                                    "❌ SERVER CONNECTION REFUSED"
                                )


                                if (
                                    localUser != null
                                ) {

                                    println(
                                        "📴 USING LOCAL LOGIN"
                                    )

                                    onLoginClick(
                                        "LOCAL_${localUser.id}"
                                    )

                                } else {

                                    message =
                                        "Cannot connect to the SitePulse server."
                                }


                                isLoading =
                                    false

                                return@launch


                            } catch (
                                e: SocketTimeoutException
                            ) {

                                // ================================================
                                // SERVER TIMEOUT
                                // ================================================

                                e.printStackTrace()

                                println(
                                    "❌ SERVER TIMED OUT"
                                )


                                if (
                                    localUser != null
                                ) {

                                    println(
                                        "📴 USING LOCAL LOGIN"
                                    )

                                    onLoginClick(
                                        "LOCAL_${localUser.id}"
                                    )

                                } else {

                                    message =
                                        "Server connection timed out."
                                }


                                isLoading =
                                    false

                                return@launch


                            } catch (
                                e: UnknownHostException
                            ) {

                                // ================================================
                                // NETWORK / HOST UNAVAILABLE
                                // ================================================

                                e.printStackTrace()

                                println(
                                    "❌ NETWORK/HOST UNAVAILABLE"
                                )


                                if (
                                    localUser != null
                                ) {

                                    println(
                                        "📴 USING LOCAL LOGIN"
                                    )

                                    onLoginClick(
                                        "LOCAL_${localUser.id}"
                                    )

                                } else {

                                    message =
                                        "No network connection is available."
                                }


                                isLoading =
                                    false

                                return@launch


                            } catch (
                                e: Exception
                            ) {

                                // ================================================
                                // OTHER ERROR
                                // ================================================

                                e.printStackTrace()

                                println(
                                    "❌ UNEXPECTED LOGIN ERROR"
                                )

                                println(
                                    "${e.javaClass.simpleName}: ${e.message}"
                                )


                                // Do NOT automatically switch to offline mode here.
                                message =
                                    "Login error: ${
                                        e.message
                                            ?: "Unknown error"
                                    }"


                                isLoading =
                                    false

                                return@launch
                            }
                        }
                    },

                    enabled =
                        !isLoading,

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),

                    shape =
                        RoundedCornerShape(11.dp),

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


                // ========================================================
                // MESSAGE
                // ========================================================

                if (
                    message.isNotEmpty()
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )


                    Text(
                        text =
                            message,

                        modifier =
                            Modifier.fillMaxWidth(),

                        color =

                            if (
                                message.contains(
                                    "Invalid",
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
                                ) ||
                                message.contains(
                                    "Cannot connect",
                                    ignoreCase = true
                                )
                            )

                                Color(0xFFD32F2F)

                            else

                                grayText,

                        fontSize =
                            11.sp
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(23.dp)
                )


                // ========================================================
                // SIGN UP
                // ========================================================

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
                            Color.Black,

                        fontSize =
                            12.sp
                    )


                    Text(
                        text =
                            "Sign up",

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


                Spacer(
                    modifier =
                        Modifier.weight(1f)
                )


                // ========================================================
                // SECURITY BOX
                // ========================================================

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = 18.dp
                        )
                        .background(
                            color =
                                Color(0xFFF5EFEF),

                            shape =
                                RoundedCornerShape(
                                    10.dp
                                )
                        )
                        .padding(
                            horizontal = 16.dp,
                            vertical = 12.dp
                        ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(27.dp)
                            .background(
                                orange,
                                RoundedCornerShape(
                                    6.dp
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
                                Color.White,

                            modifier =
                                Modifier.size(16.dp)
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.width(11.dp)
                    )


                    Text(
                        text =
                            "Online authentication is used when the server is available.\n" +
                                    "Offline access uses your secure local account.",

                        color =
                            Color(0xFF858585),

                        fontSize =
                            9.sp,

                        lineHeight =
                            12.sp
                    )
                }
            }
        }
    }
}