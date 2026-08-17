package com.example.capstonesample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider

import com.example.capstonesample.ai.*
import com.example.capstonesample.pdf.PdfReportGenerator
import com.example.capstonesample.sync.SyncManager
import com.example.capstonesample.ui.theme.CapstoneSampleTheme

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)


        // ============================================================
        // BACKGROUND SYNC
        // ============================================================

        // Runs periodically whenever Android allows it.
        //
        // SyncManager itself requires network connectivity,
        // so if there is no internet WorkManager waits.
        SyncManager.startPeriodicSync(
            applicationContext
        )


        // Queue a sync immediately.
        //
        // If there is internet:
        //     SyncWorker runs.
        //
        // If there is NO internet:
        //     WorkManager waits until internet becomes available.
        SyncManager.requestImmediateSync(
            applicationContext
        )


        // ============================================================
        // COMPOSE
        // ============================================================

        setContent {

            CapstoneSampleTheme {

                val context = LocalContext.current


                // ============================================================
                // AI MODEL STATUS
                // ============================================================

                val statusChecker =
                    remember {
                        ModelStatusChecker(context)
                    }

                var aiReady by remember {
                    mutableStateOf(
                        statusChecker.isReady()
                    )
                }


                // ============================================================
                // NAVIGATION STATE
                // ============================================================

                var currentScreen by remember {
                    mutableStateOf("login")
                }


                // ============================================================
                // AUTH TOKEN
                // ============================================================

                var authToken by remember {
                    mutableStateOf("")
                }


                // ============================================================
                // SELECTED PROJECT
                // ============================================================

                var selectedProject by remember {
                    mutableStateOf<SiteProject?>(null)
                }


                // ============================================================
                // TASK STATE
                // ============================================================

                var selectedTask by remember {
                    mutableStateOf<Task?>(null)
                }


                // ============================================================
                // CAMERA / AI STATE
                // ============================================================

                var capturedImageUri by remember {
                    mutableStateOf<Uri?>(null)
                }

                var aiReport by remember {
                    mutableStateOf<String?>(null)
                }

                var isAnalyzing by remember {
                    mutableStateOf(false)
                }

                var pendingCameraUri by remember {
                    mutableStateOf<Uri?>(null)
                }


                val activityScope =
                    rememberCoroutineScope()


                // ============================================================
                // AI PIPELINE
                // ============================================================

                var aiPipeline by remember {
                    mutableStateOf<AiPipeline?>(null)
                }

                var isLoadingPipeline by remember {
                    mutableStateOf(false)
                }


                LaunchedEffect(aiReady) {

                    if (
                        aiReady &&
                        aiPipeline == null
                    ) {

                        isLoadingPipeline = true

                        aiPipeline =
                            withContext(
                                Dispatchers.Default
                            ) {

                                try {

                                    AiPipeline(context)

                                } catch (e: Throwable) {

                                    aiReady = false
                                    null
                                }
                            }

                        isLoadingPipeline = false
                    }
                }


                // ============================================================
                // CAMERA
                // ============================================================

                val cameraLauncher =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.TakePicture()
                    ) { success ->

                        if (success) {

                            capturedImageUri =
                                pendingCameraUri

                            aiReport = null

                        } else {

                            pendingCameraUri = null
                        }
                    }


                val cameraPermissionLauncher =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { granted ->

                        if (granted) {

                            val uri =
                                createImageUri(context)

                            pendingCameraUri = uri

                            cameraLauncher.launch(uri)
                        }
                    }


                // ============================================================
                // GALLERY
                // ============================================================

                val galleryLauncher =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.PickVisualMedia()
                    ) { uri ->

                        if (uri != null) {

                            capturedImageUri = uri
                            aiReport = null
                        }
                    }


                val onTakePhoto: () -> Unit = {

                    val permission =
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        )

                    if (
                        permission ==
                        PackageManager.PERMISSION_GRANTED
                    ) {

                        val uri =
                            createImageUri(context)

                        pendingCameraUri = uri

                        cameraLauncher.launch(uri)

                    } else {

                        cameraPermissionLauncher.launch(
                            Manifest.permission.CAMERA
                        )
                    }
                }


                val onUploadPhoto: () -> Unit = {

                    galleryLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts
                                .PickVisualMedia
                                .ImageOnly
                        )
                    )
                }


                // ============================================================
                // AI ANALYSIS
                // ============================================================

                val onAnalyzeImage: (Uri) -> Unit = { uri ->

                    val task =
                        selectedTask

                    val pipeline =
                        aiPipeline


                    if (
                        !aiReady ||
                        pipeline == null
                    ) {

                        aiReport =
                            "Error: AI models not downloaded. Please download from settings."

                    } else if (task == null) {

                        aiReport =
                            "Error: No task selected."

                    } else {

                        isAnalyzing = true
                        aiReport = null


                        activityScope.launch {

                            val report =
                                withContext(
                                    Dispatchers.Default
                                ) {

                                    try {

                                        val prompt = """
                                            Generate a professional structural inspection report for the
                                            following task, based on the attached site photo:

                                            Task: ${task.title}
                                            Lead Engineer: ${task.engineer}
                                            Status: ${task.status}

                                            Format the report with:
                                            - Executive Summary
                                            - Observed Conditions
                                            - Recommendations
                                            - Safety Notes
                                        """.trimIndent()


                                        pipeline.generateReport(
                                            prompt
                                        )

                                    } catch (e: Exception) {

                                        "Report generation failed: ${
                                            e.message ?: "unknown error"
                                        }"
                                    }
                                }


                            aiReport = report

                            isAnalyzing = false
                        }
                    }
                }


                // ============================================================
                // PDF
                // ============================================================

                val onDownloadPdf: () -> Unit = {

                    val task =
                        selectedTask

                    val report =
                        aiReport


                    if (
                        task != null &&
                        report != null
                    ) {

                        try {

                            val pdfUri =
                                PdfReportGenerator.generate(

                                    context = context,

                                    taskTitle =
                                        task.title,

                                    engineer =
                                        task.engineer,

                                    status =
                                        task.status,

                                    reportText =
                                        report
                                )


                            val shareIntent =
                                Intent(
                                    Intent.ACTION_SEND
                                ).apply {

                                    type =
                                        "application/pdf"

                                    putExtra(
                                        Intent.EXTRA_STREAM,
                                        pdfUri
                                    )

                                    addFlags(
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                }


                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    "Share Report"
                                )
                            )

                        } catch (e: Exception) {

                            aiReport =
                                "Error: Failed to generate PDF (${
                                    e.message ?: "unknown error"
                                })."
                        }
                    }
                }


                // ============================================================
                // NAVIGATION
                // ============================================================

                when (currentScreen) {


                    "login" -> {

                        LoginScreen(

                            onLoginClick = { token ->

                                // Save either:
                                //
                                // real JWT when online
                                //
                                // OR
                                //
                                // LOCAL_x when offline

                                authToken = token


                                // If this is a real server JWT,
                                // request another sync immediately.
                                if (
                                    token.isNotBlank() &&
                                    !token.startsWith("LOCAL_")
                                ) {

                                    SyncManager
                                        .requestImmediateSync(
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

                                // Registration completed.
                                //
                                // Queue another sync.
                                //
                                // If there's no internet,
                                // WorkManager waits automatically.

                                SyncManager
                                    .requestImmediateSync(
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

                                selectedProject =
                                    project
                            },

                            token =
                                authToken
                        )
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

                            onChatClick = { chat ->

                            }
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

                            },

                            onAddTaskClick = {

                            }
                        )
                    }


                    "profile" -> {

                        ProfileScreen(

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

                            onLogoutClick = {

                                authToken = ""

                                selectedProject = null
                                selectedTask = null

                                capturedImageUri = null
                                aiReport = null

                                currentScreen =
                                    "login"
                            }
                        )
                    }
                }
            }
        }
    }


    // ============================================================
    // CREATE CAMERA IMAGE URI
    // ============================================================

    private fun createImageUri(
        context: android.content.Context
    ): Uri {

        val imageFile =
            java.io.File(
                context.cacheDir,
                "inspection_${
                    System.currentTimeMillis()
                }.jpg"
            )


        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            imageFile
        )
    }
}