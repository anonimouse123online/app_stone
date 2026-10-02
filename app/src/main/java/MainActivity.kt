package com.example.capstonesample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log

import androidx.activity.ComponentActivity
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
import com.example.capstonesample.sync.SyncManager
import com.example.capstonesample.ui.theme.CapstoneSampleTheme


class MainActivity : ComponentActivity() {

    private val navigateToState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // ============================================================
        // CREATE SITEPULSE NOTIFICATION CHANNEL
        // ============================================================
        com.example.capstonesample.notifications.NotificationHelper.createNotificationChannel(this)

        // ============================================================
        // NOTIFICATION PERMISSION
        // Android 13+ requires runtime permission.
        // ============================================================
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

        // ============================================================
        // FIREBASE CLOUD MESSAGING
        // Subscribe this device to notifications sent to ALL USERS and ENGINEERS.
        // ============================================================
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

        // ============================================================
        // GET FCM TOKEN FOR DIRECT MESSAGING
        // ============================================================
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


        // ============================================================
        // RETROFIT
        // ============================================================

        RetrofitClient.init(this)


        // ============================================================
        // BACKGROUND SYNC
        // ============================================================

        // Periodic synchronization.
        //
        // WorkManager will wait for network connectivity
        // before running the SyncWorker.

        SyncManager.startPeriodicSync(
            applicationContext
        )


        // Request synchronization when the application starts.
        //
        // If there is no internet connection,
        // WorkManager will wait automatically.

        SyncManager.requestImmediateSync(
            applicationContext
        )


        // ============================================================
        // COMPOSE
        // ============================================================

        setContent {

            CapstoneSampleTheme {

                val context =
                    LocalContext.current


                val savedSession = remember {
                    if (TokenManager.isRememberMe(context)) {
                        TokenManager.getUserSession(context)
                    } else {
                        null
                    }
                }

                // ====================================================
                // NAVIGATION STATE
                // ====================================================

                val startScreen = when {
                    savedSession == null -> "login"
                    intent?.getStringExtra("navigate_to") == "chat" -> "chat"
                    else -> "dashboard"
                }

                var currentScreen by remember {
                    mutableStateOf(startScreen)
                }

                LaunchedEffect(navigateToState.value) {
                    navigateToState.value?.let { screen ->
                        if (savedSession != null) {
                            currentScreen = screen
                        }
                        navigateToState.value = null
                    }
                }


                // ====================================================
                // AUTHENTICATION STATE
                // ====================================================

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


                // ====================================================
                // SELECTED PROJECT
                // ====================================================

                var selectedProject by remember {
                    mutableStateOf<SiteProject?>(null)
                }


                // ====================================================
                // NAVIGATION
                // ====================================================

                when (currentScreen) {


                    // =================================================
                    // LOGIN
                    // =================================================

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

                                // =====================================
                                // SAVE USER SESSION
                                // =====================================

                                TokenManager.saveSession(
                                    context = context,
                                    token = token,
                                    fullName = fullName,
                                    email = email,
                                    role = role
                                )


                                // =====================================
                                // SAVE REAL JWT TOKEN
                                // =====================================

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


                                // =====================================
                                // GO TO DASHBOARD
                                // =====================================

                                currentScreen =
                                    "dashboard"
                            },


                            onSignupClick = {

                                currentScreen =
                                    "signup"
                            }
                        )
                    }


                    // =================================================
                    // SIGNUP
                    // =================================================

                    "signup" -> {

                        SignupScreen(

                            onSignupClick = {

                                // Registration completed.
                                //
                                // Queue another synchronization.
                                //
                                // WorkManager waits automatically
                                // if internet is unavailable.

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


                    // =================================================
                    // DASHBOARD
                    // =================================================

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


                    // =================================================
                    // PROJECTS
                    // =================================================

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


                    // =================================================
                    // PROJECT DETAILS
                    // =================================================

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

                            // Safety fallback.
                            //
                            // If the project information was somehow
                            // lost, return to the projects page.

                            LaunchedEffect(Unit) {

                                currentScreen =
                                    "projects"
                            }
                        }
                    }


                    // =================================================
                    // CHAT
                    // =================================================

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


                    // =================================================
                    // TASKS
                    // =================================================

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


                                // =====================================
                                // AI REMOVED FOR NOW
                                // =====================================
                                //
                                // Later we can open TaskDetailsScreen
                                // here and add the camera + AI analysis
                                // only when the user requests it.
                            },


                            token =
                                authToken
                        )
                    }


                    // =================================================
                    // PROFILE
                    // =================================================

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


                                // =====================================
                                // CLEAR LOCAL LOGIN STATE
                                // =====================================

                                authToken =
                                    ""

                                loggedInFullName =
                                    ""

                                loggedInEmail =
                                    ""

                                loggedInRole =
                                    ""


                                // =====================================
                                // DELETE STORED JWT AND ACTIVE SESSION
                                // =====================================

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