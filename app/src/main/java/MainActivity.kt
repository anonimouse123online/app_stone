package com.example.capstonesample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log

import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

import com.google.firebase.messaging.FirebaseMessaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

import com.example.capstonesample.data.api.RetrofitClient
import com.example.capstonesample.security.TokenManager
import com.example.capstonesample.security.BiometricSessionStore
import com.example.capstonesample.sync.SyncManager
import com.example.capstonesample.ui.theme.CapstoneSampleTheme


class MainActivity : FragmentActivity() {

    private val navigateToState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // Clear a locked session before background work or Retrofit can read its active JWT.
        val session = TokenManager.getUserSession(this)
        val biometricStore = BiometricSessionStore(this)
        val startupSession = if (session != null && biometricStore.isEnabled(session.email)) {
            biometricStore.markLastAccount(session.email)
            TokenManager.clearSession(this)
            null
        } else if (TokenManager.isRememberMe(this)) {
            session
        } else {
            TokenManager.clearSession(this)
            null
        }

        com.example.capstonesample.notifications.NotificationHelper.createNotificationChannel(this)

        // Android 13+ requires runtime permission.
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }

        // Subscribe this device to notifications sent to ALL USERS and ENGINEERS.
        FirebaseMessaging.getInstance()
            .subscribeToTopic("all_users")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM_TOPIC", "Subscribed to all_users")
                } else {
                    Log.e("FCM_TOPIC", "Failed to subscribe to all_users", task.exception)
                }
            }

        FirebaseMessaging.getInstance()
            .subscribeToTopic("engineers")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM_TOPIC", "Subscribed to engineers")
                } else {
                    Log.e("FCM_TOPIC", "Failed to subscribe to engineers", task.exception)
                }
            }

        FirebaseMessaging.getInstance()
            .token
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val token = task.result
                    Log.d("FCM_TOKEN", "Token: $token")
                    getSharedPreferences("sitepulse_fcm", MODE_PRIVATE)
                        .edit()
                        .putString("fcm_token", token)
                        .apply()
                } else {
                    Log.e("FCM_TOKEN", "Fetching FCM token failed", task.exception)
                }
            }


        RetrofitClient.init(this)


        SyncManager.startPeriodicSync(
            applicationContext
        )


        SyncManager.requestImmediateSync(
            applicationContext
        )


        setContent {

            CapstoneSampleTheme {

                val context =
                    LocalContext.current


                val savedSession = remember {
                    startupSession
                }

                val startScreen = when {
                    savedSession == null -> "login"
                    intent?.getStringExtra("navigate_to") == "chat" -> "chat"
                    else -> "dashboard"
                }

                var currentScreen by remember {
                    mutableStateOf(startScreen)
                }

                var authToken by remember {
                    mutableStateOf(savedSession?.token ?: "")
                }


                var loggedInFullName by remember {
                    mutableStateOf(savedSession?.fullName ?: "")
                }


                var loggedInEmail by remember {
                    mutableStateOf(savedSession?.email ?: "")
                }


                var loggedInRole by remember {
                    mutableStateOf(savedSession?.role ?: "")
                }

                LaunchedEffect(navigateToState.value) {
                    navigateToState.value?.let { screen ->
                        if (authToken.isNotBlank()) currentScreen = screen
                        navigateToState.value = null
                    }
                }

                LaunchedEffect(authToken) {
                    if (authToken.isNotBlank()) {
                        com.example.capstonesample.NavBadgeState.refresh(context, authToken)
                    }
                }


                var selectedProject by remember {
                    mutableStateOf<SiteProject?>(null)
                }


                when (currentScreen) {


                    "login" -> {

                        LoginScreen(

                            onLoginClick = {
                                    token: String,
                                    fullName: String,
                                    email: String,
                                    role: String ->


                                authToken =
                                    token


                                loggedInFullName =
                                    fullName


                                loggedInEmail =
                                    email


                                loggedInRole =
                                    role

                                TokenManager.saveSession(
                                    context = context,
                                    token = token,
                                    fullName = fullName,
                                    email = email,
                                    role = role
                                )


                                // SAVE REAL JWT TOKEN
                                if (
                                    token.isNotBlank() &&
                                    !token.startsWith("LOCAL_")
                                ) {

                                    TokenManager.saveToken(
                                        context = context,
                                        token = token
                                    )


                                    // Sync data after successful login.

                                    SyncManager.requestImmediateSync(
                                        context
                                    )
                                }


                                currentScreen =
                                    "dashboard"
                            },


                            onSignupClick = {

                                currentScreen =
                                    "signup"
                            }
                        )
                    }


                    "signup" -> {

                        SignupScreen(

                            onSignupClick = {

                                SyncManager.requestImmediateSync(
                                    context
                                )


                                currentScreen =
                                    "login"
                            },


                            onLoginClick = {

                                currentScreen =
                                    "login"
                            }
                        )
                    }


                    "dashboard" -> {

                        DashboardScreen(

                            token =
                                authToken,

                            userName =
                                loggedInFullName,

                            userRole =
                                loggedInRole,

                            selectedScreen =
                                "dashboard",


                            onHomeClick = {

                                currentScreen =
                                    "dashboard"
                            },


                            onProjectsClick = {

                                currentScreen =
                                    "projects"
                            },


                            onChatClick = {

                                currentScreen =
                                    "chat"
                            },


                            onTasksClick = {

                                currentScreen =
                                    "tasks"
                            },


                            onProfileClick = {

                                currentScreen =
                                    "profile"
                            }
                        )
                    }


                    "projects" -> {

                        ProjectsScreen(

                            onHomeClick = {

                                currentScreen =
                                    "dashboard"
                            },


                            onMessagesClick = {

                                currentScreen =
                                    "chat"
                            },


                            onTasksClick = {

                                currentScreen =
                                    "tasks"
                            },


                            onProfileClick = {

                                currentScreen =
                                    "profile"
                            },


                            onProjectClick = { project ->


                                println(
                                    "===================================="
                                )

                                println(
                                    "📂 OPEN PROJECT DETAILS"
                                )

                                println(
                                    "PROJECT = ${project.name}"
                                )

                                println(
                                    "CODE = ${project.code}"
                                )

                                println(
                                    "===================================="
                                )


                                selectedProject =
                                    project


                                currentScreen =
                                    "project_detail"
                            },


                            token =
                                authToken,

                            userName =
                                loggedInFullName
                        )
                    }


                    "project_detail" -> {

                        val project =
                            selectedProject


                        if (project != null) {

                            ProjectDetailsScreen(

                                project =
                                    project,


                                token =
                                    authToken,


                                onBackClick = {

                                    currentScreen =
                                        "projects"
                                }
                            )

                        } else {

                            // Return to the project list if project state is missing.

                            LaunchedEffect(Unit) {

                                currentScreen =
                                    "projects"
                            }
                        }
                    }


                    "chat" -> {

                        ChatScreen(

                            onHomeClick = {

                                currentScreen =
                                    "dashboard"
                            },


                            onProjectsClick = {

                                currentScreen =
                                    "projects"
                            },


                            onTasksClick = {

                                currentScreen =
                                    "tasks"
                            },


                            onProfileClick = {

                                currentScreen =
                                    "profile"
                            },

                            userName =
                                loggedInFullName
                        )
                    }


                    "tasks" -> {

                        TasksScreen(

                            onHomeClick = {

                                currentScreen =
                                    "dashboard"
                            },


                            onProjectsClick = {

                                currentScreen =
                                    "projects"
                            },


                            onMessagesClick = {

                                currentScreen =
                                    "chat"
                            },


                            onProfileClick = {

                                currentScreen =
                                    "profile"
                            },


                            onTaskClick = { siteTask ->

                                println(
                                    "===================================="
                                )

                                println(
                                    "📋 OPEN TASK"
                                )

                                println(
                                    "TASK ID = ${siteTask.id}"
                                )

                                println(
                                    "===================================="
                                )


                                // Keep camera and AI analysis out of this flow until requested.
                            },


                            token =
                                authToken
                        )
                    }


                    "profile" -> {

                        ProfileScreen(

                            token =
                                authToken,


                            profile =
                                ProfileUiModel(

                                    fullName =
                                        loggedInFullName,


                                    email =
                                        loggedInEmail,


                                    role =
                                        loggedInRole,


                                    completedCount =
                                        0,


                                    loggedHours =
                                        "0h"
                                ),


                            onHomeClick = {

                                currentScreen =
                                    "dashboard"
                            },


                            onProjectsClick = {

                                currentScreen =
                                    "projects"
                            },


                            onMessagesClick = {

                                currentScreen =
                                    "chat"
                            },


                            onTasksClick = {

                                currentScreen =
                                    "tasks"
                            },


                            onProjectClick = { project ->

                                selectedProject =
                                    project


                                currentScreen =
                                    "project_detail"
                            },


                            onLogoutClick = {


                                // CLEAR LOCAL LOGIN STATE
                                authToken =
                                    ""

                                loggedInFullName =
                                    ""

                                loggedInEmail =
                                    ""

                                loggedInRole =
                                    ""


                                // DELETE STORED JWT AND ACTIVE SESSION
                                TokenManager.clearSession(
                                    context
                                )



                                currentScreen =
                                    "login"
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // When app is closed or put in the background, trigger an immediate notification check
        com.example.capstonesample.sync.SyncManager.checkNotificationsImmediately(applicationContext)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("navigate_to")?.let { target ->
            navigateToState.value = target
        }
    }
}
