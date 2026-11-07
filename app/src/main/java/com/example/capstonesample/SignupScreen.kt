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
    // COLORS (MATCHING LOGIN SCREEN)
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
        // TOP BACKGROUND (BEIGE CURVED HEADER)
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {

            Spacer(
                modifier = Modifier.height(32.dp)
            )


            // ====================================================
            // BRAND
            // ====================================================

            Row(
                verticalAlignment = Alignment.CenterVertically
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
            // WELCOME / HEADER TEXT
            // ====================================================

            Text(
                text = "Create an account",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B1B1B)
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text = "Join SitePulse to streamline your site reporting and project tracking.",
                color = Color(0xFF6F6A67),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )


            Spacer(
                modifier = Modifier.height(27.dp)
            )


            // ====================================================
            // REGISTER CARD
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
                    // CARD TITLE
                    // ============================================

                    Text(
                        text = "Register",
                        color = Color(0xFF1F1F1F),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        text = "Enter your account details below.",
                        color = grayText,
                        fontSize = 12.sp
                    )

                    Spacer(
                        modifier = Modifier.height(24.dp)
                    )


                    // ============================================
                    // FULL NAME
                    // ============================================

                    Text(
                        text = "Full Name",
                        color = Color(0xFF292929),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            message = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        placeholder = {
                            Text(
                                text = "Enter your full name",
                                color = Color(0xFF999999),
                                fontSize = 13.sp
                            )
                        },
                        enabled = !isLoading,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = fieldBackground,
                            unfocusedContainerColor = fieldBackground,
                            disabledContainerColor = fieldBackground,
                            focusedBorderColor = orange,
                            unfocusedBorderColor = fieldBorder,
                            disabledBorderColor = fieldBorder,
                            cursorColor = orange,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )


                    Spacer(
                        modifier = Modifier.height(18.dp)
                    )


                    // ============================================
                    // EMAIL / USERNAME
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
                                text = "Enter your valid email",
                                color = Color(0xFF999999),
                                fontSize = 13.sp
                            )
                        },
                        enabled = !isLoading,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = fieldBackground,
                            unfocusedContainerColor = fieldBackground,
                            disabledContainerColor = fieldBackground,
                            focusedBorderColor = orange,
                            unfocusedBorderColor = fieldBorder,
                            disabledBorderColor = fieldBorder,
                            cursorColor = orange,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
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
                                text = "Enter your password (min 6 chars)",
                                color = Color(0xFF999999),
                                fontSize = 13.sp
                            )
                        },
                        enabled = !isLoading,
                        visualTransformation =
                            if (passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    passwordVisible = !passwordVisible
                                },
                                enabled = !isLoading
                            ) {
                                Icon(
                                    imageVector =
                                        if (passwordVisible) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility",
                                    tint = Color(0xFF777777),
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = fieldBackground,
                            unfocusedContainerColor = fieldBackground,
                            disabledContainerColor = fieldBackground,
                            focusedBorderColor = orange,
                            unfocusedBorderColor = fieldBorder,
                            disabledBorderColor = fieldBorder,
                            cursorColor = orange,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )


                    Spacer(
                        modifier = Modifier.height(18.dp)
                    )


                    // ============================================
                    // CONFIRM PASSWORD
                    // ============================================

                    Text(
                        text = "Confirm Password",
                        color = Color(0xFF292929),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            message = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        placeholder = {
                            Text(
                                text = "Confirm your password",
                                color = Color(0xFF999999),
                                fontSize = 13.sp
                            )
                        },
                        enabled = !isLoading,
                        visualTransformation =
                            if (confirmPasswordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    confirmPasswordVisible = !confirmPasswordVisible
                                },
                                enabled = !isLoading
                            ) {
                                Icon(
                                    imageVector =
                                        if (confirmPasswordVisible) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                    contentDescription = "Toggle confirm password visibility",
                                    tint = Color(0xFF777777),
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = fieldBackground,
                            unfocusedContainerColor = fieldBackground,
                            disabledContainerColor = fieldBackground,
                            focusedBorderColor = orange,
                            unfocusedBorderColor = fieldBorder,
                            disabledBorderColor = fieldBorder,
                            cursorColor = orange,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )


                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )


                    // ============================================
                    // TERMS & PRIVACY
                    // ============================================

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = acceptedTerms,
                            onCheckedChange = {
                                acceptedTerms = it
                                message = ""
                            },
                            enabled = !isLoading,
                            modifier = Modifier.size(20.dp),
                            colors = CheckboxDefaults.colors(
                                checkedColor = orange,
                                uncheckedColor = Color(0xFFB0B0B0),
                                checkmarkColor = Color.White
                            )
                        )

                        Spacer(
                            modifier = Modifier.width(8.dp)
                        )

                        Text(
                            text = "I accept the Terms of Service & Privacy Policy",
                            fontSize = 12.sp,
                            color = Color(0xFF333333)
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = "By registering, you acknowledge and consent to collection and verification of geolocation for audit trails.",
                        color = Color(0xFF888888),
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )


                    // ============================================
                    // STATUS / ERROR MESSAGE
                    // ============================================

                    if (message.isNotEmpty()) {
                        Spacer(
                            modifier = Modifier.height(14.dp)
                        )

                        val isSuccess = message.contains("success", ignoreCase = true) ||
                                message.contains("locally", ignoreCase = true)

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFF1F0),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 10.dp
                                ),
                                color = if (isSuccess) Color(0xFF198754) else Color(0xFFD32F2F),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }


                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )


                    // ============================================
                    // CREATE ACCOUNT BUTTON
                    // ============================================

                    Button(
                        onClick = {
                            when {
                                fullName.isBlank() -> {
                                    message = "Please enter your full name."
                                }

                                email.isBlank() -> {
                                    message = "Please enter your email."
                                }

                                !email.contains("@") -> {
                                    message = "Please enter a valid email address."
                                }

                                password.isBlank() -> {
                                    message = "Please enter your password."
                                }

                                password.length < 6 -> {
                                    message = "Password must contain at least 6 characters."
                                }

                                password != confirmPassword -> {
                                    message = "Passwords do not match."
                                }

                                !acceptedTerms -> {
                                    message = "Please accept the Terms of Service and Privacy Policy."
                                }

                                else -> {
                                    scope.launch {
                                        isLoading = true
                                        message = ""

                                        val cleanEmail = email.trim().lowercase()

                                        try {
                                            // ==================================
                                            // CHECK LOCAL DATABASE
                                            // ==================================
                                            val exists = userDao.userExists(cleanEmail)

                                            if (exists) {
                                                message = "An account with this email already exists."
                                                isLoading = false
                                                return@launch
                                            }

                                            // ==================================
                                            // HASH PASSWORD
                                            // ==================================
                                            val hashedPassword =
                                                PasswordUtils.hashPassword(password)

                                            // ==================================
                                            // STEP 1: CREATE LOCAL ACCOUNT
                                            // ==================================
                                            val localUser = UserEntity(
                                                fullName = fullName.trim(),
                                                email = cleanEmail,
                                                passwordHash = hashedPassword,
                                                role = "engineer",
                                                isSynced = false,
                                                serverUserId = null
                                            )

                                            userDao.insertUser(localUser)

                                            message = "Account created locally."

                                            // ==================================
                                            // STEP 2: TRY SERVER REGISTRATION
                                            // ==================================
                                            try {
                                                val response = RetrofitClient.api.signup(
                                                    SignupRequest(
                                                        name = fullName.trim(),
                                                        email = cleanEmail,
                                                        password = password,
                                                        role = "engineer"
                                                    )
                                                )

                                                if (response.isSuccessful) {
                                                    val serverUser = response.body()?.user
                                                    userDao.markUserSynced(
                                                        email = cleanEmail,
                                                        serverUserId = serverUser?.id
                                                    )
                                                    message = "Account created and synced successfully."
                                                } else {
                                                    message = "Account created locally. Server sync will happen later."
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                message = "Account created locally. You can sign in offline."
                                            }

                                            delay(700)
                                            onSignupClick()

                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            message = "Unable to create local account: ${e.message}"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = orange,
                            disabledContainerColor = orange.copy(alpha = 0.6f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Create Account",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }


                    Spacer(
                        modifier = Modifier.height(24.dp)
                    )


                    // ============================================
                    // DIVIDER
                    // ============================================

                    HorizontalDivider(
                        color = Color(0xFFF0EBE8)
                    )


                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )


                    // ============================================
                    // SIGN IN FOOTER LINK
                    // ============================================

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Already have an account? ",
                            color = Color(0xFF666666),
                            fontSize = 12.sp
                        )

                        Text(
                            text = "Sign in",
                            color = blue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                if (!isLoading) {
                                    onLoginClick()
                                }
                            }
                        )
                    }
                }
            }


            Spacer(
                modifier = Modifier.height(20.dp)
            )


            // ====================================================
            // SECURITY INFORMATION (MATCHING LOGIN SCREEN)
            // ====================================================

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF8F3F1),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = Color(0xFFF1E6E1)
                )
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 14.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                orange.copy(alpha = 0.12f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = orange,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(12.dp)
                    )

                    Column {
                        Text(
                            text = "Secure registration",
                            color = Color(0xFF343434),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(
                            modifier = Modifier.height(3.dp)
                        )

                        Text(
                            text = "Online synchronization is used when server is reachable. Offline accounts are safely kept on device.",
                            color = Color(0xFF858585),
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }


            Spacer(
                modifier = Modifier.height(28.dp)
            )
        }
    }
}
