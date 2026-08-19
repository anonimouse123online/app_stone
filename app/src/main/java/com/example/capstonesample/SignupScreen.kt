package com.example.capstonesample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.example.capstonesample.data.local.UserEntity
import com.example.capstonesample.data.model.SignupRequest
import com.example.capstonesample.security.PasswordUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun SignupScreen(
    onSignupClick: () -> Unit = {},
    onLoginClick: () -> Unit = {}
) {

    var fullName by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var confirmPasswordVisible by remember {
        mutableStateOf(false)
    }

    var acceptedTerms by remember {
        mutableStateOf(false)
    }

    var message by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(false)
    }


    // ============================================================
    // ROOM DATABASE
    // ============================================================

    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()

    val database =
        remember {
            AppDatabase.getDatabase(context)
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

    val blue =
        Color(0xFF007AFF)

    val fieldBackground =
        Color(0xFFFAFAFA)

    val fieldBorder =
        Color(0xFFE4DEDA)

    val grayText =
        Color(0xFF7C7C7C)


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp)
        ) {

            Spacer(
                modifier = Modifier.height(20.dp)
            )


            // ============================================================
            // TOP BAR
            // ============================================================

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Row(
                    modifier =
                        Modifier.clickable {

                            if (!isLoading) {
                                onLoginClick()
                            }
                        },

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            Icons.AutoMirrored
                                .Filled.ArrowBack,

                        contentDescription =
                            "Back",

                        tint =
                            Color.Black,

                        modifier =
                            Modifier.size(20.dp)
                    )


                    Spacer(
                        modifier =
                            Modifier.width(6.dp)
                    )


                    Text(
                        text = "Back",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight =
                            FontWeight.Medium
                    )
                }


                Spacer(
                    modifier =
                        Modifier.weight(1f)
                )


                Row(
                    modifier =
                        Modifier.clickable {

                            if (!isLoading) {
                                onLoginClick()
                            }
                        }
                ) {

                    Text(
                        text =
                            "Already have an account? ",
                        color = blue,
                        fontSize = 12.sp
                    )

                    Text(
                        text = "Sign in",
                        color = blue,
                        fontSize = 12.sp,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(54.dp)
            )


            // ============================================================
            // TITLE
            // ============================================================

            Text(
                text = "Register",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )


            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )


            Text(
                text =
                    "Get started with SitePulse—create an account to\n" +
                            "streamline your site reporting and project tracking.",

                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = grayText
            )


            Spacer(
                modifier =
                    Modifier.height(50.dp)
            )


            // ============================================================
            // FULL NAME
            // ============================================================

            Text(
                text = "Full Name",
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.SemiBold,
                color = Color.Black
            )


            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )


            OutlinedTextField(
                value = fullName,

                onValueChange = {

                    fullName = it
                    message = ""
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),

                placeholder = {

                    Text(
                        text =
                            "Enter your full name",

                        fontSize =
                            14.sp,

                        color =
                            Color.Gray
                    )
                },

                enabled =
                    !isLoading,

                singleLine = true,

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
                    Modifier.height(12.dp)
            )


            // ============================================================
            // EMAIL
            // ============================================================

            Text(
                text = "Username / Email",
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.SemiBold,
                color = Color.Black
            )


            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )


            OutlinedTextField(
                value = email,

                onValueChange = {

                    email = it
                    message = ""
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),

                placeholder = {

                    Text(
                        text =
                            "Enter your valid email",

                        fontSize =
                            14.sp,

                        color =
                            Color.Gray
                    )
                },

                enabled =
                    !isLoading,

                singleLine = true,

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
                    Modifier.height(12.dp)
            )


            // ============================================================
            // PASSWORD
            // ============================================================

            Text(
                text = "Password",
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.SemiBold,
                color = Color.Black
            )


            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )


            OutlinedTextField(
                value = password,

                onValueChange = {

                    password = it
                    message = ""
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),

                placeholder = {

                    Text(
                        text =
                            "Enter your password",

                        fontSize =
                            14.sp,

                        color =
                            Color.Gray
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

                                    Icons.Default
                                        .VisibilityOff

                                else

                                    Icons.Default
                                        .Visibility,

                            contentDescription =
                                "Show password",

                            tint =
                                Color.Gray
                        )
                    }
                },

                singleLine = true,

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
                    Modifier.height(12.dp)
            )


            // ============================================================
            // CONFIRM PASSWORD
            // ============================================================

            Text(
                text = "Confirm Password",
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.SemiBold,
                color = Color.Black
            )


            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )


            OutlinedTextField(
                value = confirmPassword,

                onValueChange = {

                    confirmPassword = it
                    message = ""
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),

                placeholder = {

                    Text(
                        text =
                            "Confirm your password",

                        fontSize =
                            14.sp,

                        color =
                            Color.Gray
                    )
                },

                enabled =
                    !isLoading,

                visualTransformation =

                    if (confirmPasswordVisible)

                        VisualTransformation.None

                    else

                        PasswordVisualTransformation(),

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
                                )

                                    Icons.Default
                                        .VisibilityOff

                                else

                                    Icons.Default
                                        .Visibility,

                            contentDescription =
                                "Show confirm password",

                            tint =
                                Color.Gray
                        )
                    }
                },

                singleLine = true,

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
                    Modifier.height(12.dp)
            )


            // ============================================================
            // TERMS
            // ============================================================

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Checkbox(
                    checked =
                        acceptedTerms,

                    onCheckedChange = {

                        acceptedTerms = it
                        message = ""
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
                        "I accept the Terms of Service and Privacy Policy",

                    fontSize =
                        12.sp,

                    color =
                        Color.Black
                )
            }


            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )


            Text(
                text =
                    "By proceeding with registration, you acknowledge and consent to\n" +
                            "the collection and verify geolocation for audit trails.",

                color =
                    Color.Gray,

                fontSize =
                    10.sp,

                lineHeight =
                    14.sp
            )


            // ============================================================
            // MESSAGE
            // ============================================================

            if (
                message.isNotEmpty()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                Text(
                    text = message,

                    color =

                        if (
                            message.contains(
                                "success",
                                ignoreCase = true
                            ) ||
                            message.contains(
                                "locally",
                                ignoreCase = true
                            )
                        )

                            Color(0xFF198754)

                        else

                            Color(0xFFD32F2F),

                    fontSize =
                        11.sp
                )
            }


            Spacer(
                modifier =
                    Modifier.weight(1f)
            )


            // ============================================================
            // CREATE ACCOUNT
            // ============================================================

            Button(

                onClick = {

                    when {

                        fullName.isBlank() -> {

                            message =
                                "Please enter your full name."
                        }


                        email.isBlank() -> {

                            message =
                                "Please enter your email."
                        }


                        !email.contains("@") -> {

                            message =
                                "Please enter a valid email address."
                        }


                        password.isBlank() -> {

                            message =
                                "Please enter your password."
                        }


                        password.length < 6 -> {

                            message =
                                "Password must contain at least 6 characters."
                        }


                        password !=
                                confirmPassword -> {

                            message =
                                "Passwords do not match."
                        }


                        !acceptedTerms -> {

                            message =
                                "Please accept the Terms of Service and Privacy Policy."
                        }


                        else -> {

                            scope.launch {

                                isLoading =
                                    true

                                message =
                                    ""


                                val cleanEmail =
                                    email
                                        .trim()
                                        .lowercase()


                                try {

                                    // ==================================
                                    // CHECK LOCAL DATABASE
                                    // ==================================

                                    val exists =
                                        userDao.userExists(
                                            cleanEmail
                                        )


                                    if (exists) {

                                        message =
                                            "An account with this email already exists."

                                        isLoading =
                                            false

                                        return@launch
                                    }


                                    // ==================================
                                    // HASH PASSWORD
                                    // ==================================

                                    val hashedPassword =
                                        PasswordUtils
                                            .hashPassword(
                                                password
                                            )


                                    // ==================================
                                    // STEP 1
                                    // ALWAYS CREATE LOCAL ACCOUNT FIRST
                                    // ==================================

                                    val localUser =
                                        UserEntity(
                                            fullName = fullName.trim(),
                                            email = cleanEmail,
                                            passwordHash = hashedPassword,

                                            role = "engineer",

                                            isSynced = false,
                                            serverUserId = null
                                        )


                                    userDao.insertUser(
                                        localUser
                                    )


                                    // Local registration is already successful
                                    message =
                                        "Account created locally."


                                    // ==================================
                                    // STEP 2
                                    // TRY SERVER REGISTRATION
                                    // ==================================

                                    try {

                                        val response =
                                            RetrofitClient
                                                .api
                                                .signup(
                                                    SignupRequest(
                                                        name = fullName.trim(),
                                                        email = cleanEmail,
                                                        password = password,
                                                        role = "engineer"
                                                    )
                                                )


                                        if (
                                            response
                                                .isSuccessful
                                        ) {

                                            val serverUser =
                                                response
                                                    .body()
                                                    ?.user


                                            userDao
                                                .markUserSynced(

                                                    email =
                                                        cleanEmail,

                                                    serverUserId =
                                                        serverUser
                                                            ?.id
                                                )


                                            message =
                                                "Account created and synced successfully."

                                        } else {

                                            // Account remains local.
                                            // We can sync later.

                                            message =
                                                "Account created locally. Server sync will happen later."
                                        }


                                    } catch (
                                        e: Exception
                                    ) {

                                        // No internet/server.
                                        // This DOES NOT fail registration.

                                        e.printStackTrace()

                                        message =
                                            "Account created locally. You can sign in offline."
                                    }


                                    // Give the user a moment to see status.
                                    delay(700)


                                    // Go back to login
                                    onSignupClick()


                                } catch (
                                    e: Exception
                                ) {

                                    e.printStackTrace()

                                    message =
                                        "Unable to create local account: ${e.message}"
                                }


                                isLoading =
                                    false
                            }
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
                    ButtonDefaults
                        .buttonColors(

                            containerColor =
                                orange,

                            disabledContainerColor =
                                orange.copy(
                                    alpha = 0.6f
                                )
                        )

            ) {


                if (isLoading) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(20.dp),

                        color =
                            Color.White,

                        strokeWidth =
                            2.dp
                    )

                } else {

                    Text(
                        text =
                            "Create Account",

                        fontWeight =
                            FontWeight.Bold,

                        fontSize =
                            14.sp,

                        color =
                            Color.White
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )
        }
    }
}