package com.example.capstonesample

import android.Manifest
import com.example.capstonesample.ai.ModelDownloadManager
import com.example.capstonesample.ai.DownloadProgress
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.widget.Toast
import com.example.capstonesample.data.api.SubtaskItem
import com.example.capstonesample.data.api.UpdateSubtasksRequest
import com.example.capstonesample.data.model.CreateIssueRequest
import com.example.capstonesample.data.model.FieldAnnotation


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.core.content.FileProvider

import com.example.capstonesample.ai.AiPipeline
import com.example.capstonesample.ai.ModelStatusChecker
import com.example.capstonesample.pdf.PdfReportGenerator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.capstonesample.data.api.RetrofitClient
import com.example.capstonesample.data.model.UploadTaskReportRequest
import com.example.capstonesample.security.TokenManager

import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


// ============================================================
// COLORS
// ============================================================

private val TaskDetailBackground =
    Color(0xFFF6F8FA)

private val TaskDetailOrange =
    Color(0xFFF15A24)

private val TaskDetailGray =
    Color(0xFF64748B)

private val TaskDetailGreen =
    Color(0xFF10B981)

private val TaskDetailRed =
    Color(0xFFEF4444)


// ============================================================
// MANPOWER DATA MODEL
// ============================================================

data class ManpowerItem(
    val role: String,
    val count: String = "1",
    val isPreset: Boolean = false
)


// ============================================================
// TASK DETAIL SCREEN
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(

    task: SiteTask,

    onBackClick: () -> Unit,

    // Keep this so your existing navigation code will not break.
    // Camera is now handled directly inside this screen.
    onCameraClick: () -> Unit = {},

    onReportClick: () -> Unit

) {

    val context =
        LocalContext.current


    // ============================================================
    // AI / LLM STATE
    // ============================================================

    val aiScope =
        rememberCoroutineScope()

    val modelStatusChecker =
        remember {
            ModelStatusChecker(
                context.applicationContext
            )
        }

    fun isGemmaInstalled(): Boolean {

        return try {

            modelStatusChecker.isGemmaReady()

        } catch (_: Exception) {

            false
        }
    }

    var aiModelInstalled by remember {
        mutableStateOf(
            isGemmaInstalled()
        )
    }

    var isPreparingAi by remember {
        mutableStateOf(
            !aiModelInstalled
        )
    }

    var isGeneratingAiReport by remember {
        mutableStateOf(false)
    }

    var detectedObjects by remember {
        mutableStateOf("")
    }

    var aiReport by remember {
        mutableStateOf<String?>(null)
    }

    var aiError by remember {
        mutableStateOf<String?>(null)
    }
    var isUploadingReport by remember {
        mutableStateOf(false)
    }

    var reportUploaded by remember {
        mutableStateOf(false)
    }

    var isCompletingTask by remember {
        mutableStateOf(false)
    }

    var taskCompleted by remember {
        mutableStateOf(
            task.status.equals(
                "Completed",
                ignoreCase = true
            ) ||
                    task.status.equals(
                        "Done",
                        ignoreCase = true
                    )
        )
    }

    var actionMessage by remember {
        mutableStateOf<String?>(null)
    }

    // ============================================================
    // ENGINEER IMAGE ANNOTATIONS
    // ============================================================

    var fieldAnnotations by remember {
        mutableStateOf<List<FieldAnnotation>>(emptyList())
    }

    var currentSubtasks by remember(task.id) {
        mutableStateOf(task.subtasks)
    }

    var currentProgress by remember(task.id) {
        mutableStateOf(task.progress)
    }

    var currentStatus by remember(task.id) {
        mutableStateOf(task.status)
    }

    var isUpdatingSubtasks by remember {
        mutableStateOf(false)
    }

    var currentProjectStatus by remember(task.id) {
        mutableStateOf(task.projectStatus)
    }

    fun isProjectActivated(status: String?): Boolean {
        if (status.isNullOrBlank()) return true
        val s = status.trim().lowercase()
        return s != "planning" && s != "draft" && s != "pending" && s != "not started" && s != "inactive"
    }

    val isProjectActive = remember(currentProjectStatus) {
        isProjectActivated(currentProjectStatus)
    }

    // Refresh or fetch project status if not yet present
    LaunchedEffect(task.id, task.projectCode) {
        if (currentProjectStatus.isNullOrBlank() && !task.projectCode.isNullOrBlank()) {
            try {
                val token = TokenManager.getToken(context)
                val authHeader = if (token.isNullOrBlank()) "" else if (token.startsWith("Bearer ")) token else "Bearer $token"
                val res = RetrofitClient.api.getProjectByCode(authHeader, task.projectCode)
                if (res.isSuccessful) {
                    res.body()?.data?.status?.let { s ->
                        currentProjectStatus = s
                    }
                }
            } catch (e: Exception) {
                Log.w("TaskDetail", "Could not fetch project status for ${task.projectCode}", e)
            }
        }
    }

    // Download state for Gemma model
    var isDownloadingModel by remember { mutableStateOf(false) }
    var downloadProgressPct by remember { mutableStateOf(0) }
    var downloadProgressText by remember { mutableStateOf("") }
    var downloadError by remember { mutableStateOf<String?>(null) }

    // Manpower deployment inputs (starts empty so nothing is displayed until added)
    var manpowerList by remember { mutableStateOf<List<ManpowerItem>>(emptyList()) }

    fun toggleSubtask(subtaskId: String) {
        if (!isProjectActive) {
            Toast.makeText(
                context,
                "Project is in planning and not yet activated. Subtasks are locked.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val updated = currentSubtasks.map { item ->
            if (item.id == subtaskId) item.copy(completed = !item.completed) else item
        }
        val doneCount = updated.count { it.completed }
        val newPct = if (updated.isNotEmpty()) Math.round((doneCount.toFloat() / updated.size.toFloat()) * 100) else 0
        val newStatus = when {
            newPct >= 100 -> "Completed"
            newPct > 0 -> "In Progress"
            else -> "Pending"
        }

        currentSubtasks = updated
        currentProgress = newPct
        currentStatus = newStatus
        if (newPct >= 100) {
            taskCompleted = true
        }

        aiScope.launch {
            try {
                isUpdatingSubtasks = true
                val response = RetrofitClient.api.updateSubtasks(
                    taskId = task.id,
                    request = UpdateSubtasksRequest(subtasks = updated)
                )
                if (!response.isSuccessful) {
                    Toast.makeText(context, "Failed to sync subtask: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("SUBTASKS", "Failed to update subtasks", e)
                Toast.makeText(context, "Network error updating subtask", Toast.LENGTH_SHORT).show()
            } finally {
                isUpdatingSubtasks = false
            }
        }
    }


    // ============================================================
    // PREPARE / AUTO-DOWNLOAD GEMMA MODEL
    // ============================================================

    LaunchedEffect(Unit) {

        // Already on disk → done
        if (aiModelInstalled) {
            isPreparingAi = false
            return@LaunchedEffect
        }

        isPreparingAi = true
        aiError = null

        // 1. Quick check: maybe it was downloaded earlier
        val alreadyReady = withContext(Dispatchers.IO) {
            modelStatusChecker.ensureGemmaInstalled()
        }

        if (alreadyReady) {
            aiModelInstalled = true
            isPreparingAi = false
            return@LaunchedEffect
        }

        // 2. Not on disk and not bundled → auto-download
        isDownloadingModel = true
        downloadError = null
        downloadProgressPct = 0
        downloadProgressText = "Starting download..."

        val downloadManager = ModelDownloadManager(context)

        try {
            downloadManager.downloadGemma().collect { progress ->
                when (progress) {
                    is DownloadProgress.Progress -> {
                        val pct = if (progress.total > 0) {
                            (progress.downloaded * 100 / progress.total).toInt().coerceIn(0, 100)
                        } else 0
                        downloadProgressPct = pct
                        downloadProgressText = "${progress.downloadedMB}MB / ${progress.totalMB}MB"
                    }
                    is DownloadProgress.FileDone -> {
                        downloadProgressPct = 100
                        downloadProgressText = "Download complete!"
                    }
                    is DownloadProgress.Error -> {
                        downloadError = progress.message
                    }
                }
            }
        } catch (e: Exception) {
            downloadError = e.message ?: "Download failed."
        }

        isDownloadingModel = false

        // 3. Re-check after download
        val nowReady = withContext(Dispatchers.IO) {
            modelStatusChecker.isGemmaReady()
        }

        aiModelInstalled = nowReady
        isPreparingAi = false

        if (!nowReady && downloadError == null) {
            aiError = "Download finished but model verification failed."
        } else if (downloadError != null) {
            aiError = "Model download failed: $downloadError"
        }
    }


    // ============================================================
    // CAMERA & GALLERY STATE
    // ============================================================

    // URI of the photo that has successfully been captured.
    var capturedPhotoUri by remember {
        mutableStateOf<Uri?>(null)
    }

    // URI where the next photo will be saved.
    var pendingPhotoUri by remember {
        mutableStateOf<Uri?>(null)
    }

    // ============================================================
    // CAMERA & GALLERY LAUNCHERS
    // ============================================================

    val cameraLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.TakePicture()
        ) { success ->

            if (success) {

                // The camera successfully saved the image.
                capturedPhotoUri =
                    pendingPhotoUri

                // New photo = clear old AI result and old annotations.
                fieldAnnotations = emptyList()
                detectedObjects = ""
                aiReport = null
                aiError = null

            } else {

                // User cancelled the camera.
                pendingPhotoUri =
                    null
            }
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                capturedPhotoUri = uri
                detectedObjects = ""
                aiReport = null
                aiError = null
            }
        }

    fun launchCameraIntent() {
        try {
            val picturesDirectory =
                File(
                    context.getExternalFilesDir(
                        Environment.DIRECTORY_PICTURES
                    ),
                    "SitePulse"
                )

            if (!picturesDirectory.exists()) {
                picturesDirectory.mkdirs()
            }

            val imageFile =
                File(
                    picturesDirectory,
                    "evidence_${System.currentTimeMillis()}.jpg"
                )

            val photoUri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    imageFile
                )

            pendingPhotoUri =
                photoUri

            cameraLauncher.launch(
                photoUri
            )

            onCameraClick()

        } catch (e: Exception) {
            Log.e("CAMERA", "Failed to launch camera", e)
            Toast.makeText(
                context,
                "Unable to open camera: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                launchCameraIntent()
            } else {
                Toast.makeText(
                    context,
                    "Camera permission is required to capture field evidence.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    fun openCamera() {
        if (!isProjectActive) {
            Toast.makeText(
                context,
                "Project is in planning and not yet activated. Field evidence capture is locked.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val hasPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            launchCameraIntent()
        } else {
            cameraPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }


    Scaffold(

        containerColor =
            TaskDetailBackground,

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(

                            text =
                                "Task Details",

                            fontWeight =
                                FontWeight.Bold,

                            fontSize =
                                18.sp,

                            color =
                                Color.Black

                        )


                        Text(
                            text =
                                task.project,

                            fontSize =
                                10.sp,

                            color =
                                TaskDetailGray
                        )
                    }
                },


                navigationIcon = {

                    IconButton(
                        onClick =
                            onBackClick
                    ) {

                        Icon(

                            imageVector =
                                Icons.AutoMirrored
                                    .Outlined
                                    .ArrowBack,

                            contentDescription =
                                "Back"
                        )
                    }
                },


                colors =
                    TopAppBarDefaults
                        .topAppBarColors(
                            containerColor =
                                Color.White
                        )
            )
        }

    ) { padding ->


        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(16.dp)

        ) {


            // ====================================================
            // TASK HEADER
            // ====================================================

            TaskHeaderCard(
                task =
                    task,
                status =
                    currentStatus
            )


            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )


            // ====================================================
            // TASK INFORMATION
            // ====================================================

            TaskInformationCard(
                task =
                    task
            )


            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )


            // ====================================================
            // SUBTASKS & EXECUTION STEPS
            // ====================================================

            TaskSubtasksCard(
                subtasks =
                    currentSubtasks,
                isUpdating =
                    isUpdatingSubtasks,
                isProjectActive =
                    isProjectActive,
                projectStatus =
                    currentProjectStatus,
                onToggleSubtask = { subtaskId ->
                    toggleSubtask(subtaskId)
                }
            )


            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )


            // ====================================================
            // PROGRESS
            // ====================================================

            TaskProgressCard(
                task =
                    task,
                progress =
                    currentProgress,
                status =
                    currentStatus
            )


            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )


            // ====================================================
            // FIELD EVIDENCE
            // ====================================================

            FieldEvidenceCard(

                capturedPhotoUri =
                    capturedPhotoUri,

                annotations =
                    fieldAnnotations,

                isProjectActive =
                    isProjectActive,

                onAnnotationsChange = { updatedAnnotations ->
                    fieldAnnotations =
                        updatedAnnotations
                },

                onCameraClick = {
                    openCamera()
                },

                onGalleryClick = {
                    if (!isProjectActive) {
                        Toast.makeText(
                            context,
                            "Project is in planning and not yet activated. Field evidence capture is locked.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        galleryLauncher.launch("image/*")
                    }
                }
            )


            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )


            // ====================================================
            // REPORT
            // ====================================================

            ReportTaskCard(
                task = task,
                isProjectActive = isProjectActive,
                onReportClick = onReportClick
            )


            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )


            // ====================================================
            // FUTURE AI
            // ====================================================

            AiFieldAnalysisCard(

                capturedPhotoUri =
                    capturedPhotoUri,

                aiModelInstalled =
                    aiModelInstalled,

                isPreparing =
                    isPreparingAi,

                isDownloading =
                    isDownloadingModel,

                downloadPct =
                    downloadProgressPct,

                downloadText =
                    downloadProgressText,

                isGenerating =
                    isGeneratingAiReport,

                detectedObjects =
                    detectedObjects,

                report =
                    aiReport,

                error =
                    aiError,

                isUploading =
                    isUploadingReport,

                isCompleting =
                    isCompletingTask,

                reportUploaded =
                    reportUploaded,

                taskCompleted =
                    taskCompleted,

                manpowerList =
                    manpowerList,

                onManpowerListChange = {
                    manpowerList = it
                },

                onReportChange = {
                    aiReport = it
                },

                onGenerateReport = {

                    val imageUri =
                        capturedPhotoUri

                    when {

                        imageUri == null -> {

                            aiError =
                                "Capture a field photo first."
                        }

                        isPreparingAi -> {

                            aiError =
                                "Gemma AI is still being prepared."
                        }

                        !aiModelInstalled -> {

                            aiError =
                                "The bundled Gemma AI model is not ready."
                        }

                        !isGeneratingAiReport -> {

                            isGeneratingAiReport =
                                true

                            detectedObjects =
                                ""

                            aiReport =
                                null

                            aiError =
                                null

                            reportUploaded =
                                false

                            actionMessage =
                                null

                            val totalManpower = manpowerList.sumOf { it.count.toIntOrNull() ?: 0 }

                            val inputtedManpower = if (manpowerList.isNotEmpty() && totalManpower > 0) {
                                val lines = mutableListOf<String>()
                                lines.add("Total: $totalManpower")
                                manpowerList.forEach { item ->
                                    val c = item.count.toIntOrNull() ?: 0
                                    val cleanRole = if (item.role.startsWith("PIC", ignoreCase = true)) "PIC" else item.role.trim()
                                    if (cleanRole.isNotBlank() && c > 0) {
                                        lines.add("- $cleanRole: $c")
                                    }
                                }
                                lines.joinToString("\n")
                            } else {
                                ""
                            }

                            aiScope.launch {

                                try {

                                    val pipeline =
                                        AiPipeline(
                                            context.applicationContext
                                        )

                                    try {

                                        pipeline.initialize()

                                        val result =
                                            pipeline.analyzeImage(
                                                imageUri =
                                                    imageUri,

                                                annotations =
                                                     fieldAnnotations,

                                                taskTitle =
                                                    task.title,

                                                projectName =
                                                    task.project,

                                                location =
                                                    task.projectLocation?.ifBlank { null } ?: "Project Site",

                                                subtasks =
                                                    currentSubtasks,

                                                assignee =
                                                    task.assignee,

                                                currentProgress =
                                                    currentProgress,

                                                manpower =
                                                    inputtedManpower
                                            )

                                        detectedObjects =
                                            result.detectionSummary

                                        aiReport =
                                            result.report

                                    } finally {

                                        pipeline.release()
                                    }

                                } catch (e: Throwable) {

                                    Log.e(
                                        "AI_FIELD",
                                        "AI report generation failed",
                                        e
                                    )

                                    aiError =
                                        e.message
                                            ?: "AI report generation failed."

                                } finally {

                                    isGeneratingAiReport =
                                        false
                                }
                            }
                        }
                    }
                },

                onDownloadPdf = {

                    val reportText =
                        aiReport

                    if (reportText.isNullOrBlank()) {

                        aiError =
                            "Generate an AI report first."

                    } else {

                        try {

                            val pdfUri =
                                PdfReportGenerator.generate(

                                    context =
                                        context,

                                    taskTitle =
                                        task.title,

                                    engineer =
                                        task.assignee,

                                    status =
                                        task.status,

                                    reportText =
                                        reportText
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
                                    "Save or Share AI Report"
                                )
                            )

                        } catch (e: Exception) {

                            aiError =
                                "Unable to create PDF: ${
                                    e.message ?: "Unknown error"
                                }"
                        }
                    }
                },

                onUploadReport = {

                    val reportText =
                        aiReport

                    if (reportText.isNullOrBlank()) {

                        aiError =
                            "Generate an AI report first."

                    } else if (!isUploadingReport) {

                        isUploadingReport =
                            true

                        aiError =
                            null

                        actionMessage =
                            null

                        aiScope.launch {

                            try {

                                val response =
                                    RetrofitClient.api
                                        .uploadTaskReport(

                                            taskId =
                                                task.id,

                                            request =
                                                UploadTaskReportRequest(

                                                    taskId =
                                                        task.id,

                                                    projectCode =
                                                        task.projectCode
                                                            ?: throw IllegalStateException(
                                                                "Project code is missing for this task."
                                                            ),

                                                    title =
                                                        "AI Field Report - ${task.title}",

                                                    reportText =
                                                        reportText
                                                )
                                        )

                                if (
                                    response.isSuccessful &&
                                    response.body()?.success == true
                                ) {

                                    reportUploaded =
                                        true

                                    actionMessage =
                                        "Report uploaded successfully."

                                } else {

                                    val errServerMsg = try {
                                        response.errorBody()?.string()?.let { errJson ->
                                            val obj = org.json.JSONObject(errJson)
                                            obj.optString("message").ifBlank { obj.optString("error") }
                                        }
                                    } catch (_: Exception) {
                                        null
                                    }

                                    aiError =
                                        response.body()?.message
                                            ?: errServerMsg?.takeIf { it.isNotBlank() }
                                            ?: "Unable to upload report."
                                }

                            } catch (e: Exception) {

                                Log.e(
                                    "TASK_REPORT",
                                    "Report upload failed",
                                    e
                                )

                                aiError =
                                    e.message
                                        ?: "Unable to upload report."

                            } finally {

                                isUploadingReport =
                                    false
                            }
                        }
                    }
                },

                onMarkDone = {

                    if (!isProjectActive) {
                        Toast.makeText(
                            context,
                            "Cannot complete task: Project is currently in planning and not yet activated.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (!isCompletingTask) {

                        isCompletingTask =
                            true

                        aiError =
                            null

                        actionMessage =
                            null

                        aiScope.launch {

                            try {

                                val response =
                                    RetrofitClient.api
                                        .completeTask(
                                            taskId =
                                                task.id
                                        )

                                if (
                                    response.isSuccessful &&
                                    response.body()?.success == true
                                ) {

                                    taskCompleted =
                                        true

                                    currentProgress = 100
                                    currentStatus = "Completed"
                                    currentSubtasks = currentSubtasks.map { it.copy(completed = true) }

                                    actionMessage =
                                        "Task marked as completed."

                                } else {

                                    aiError =
                                        response.body()
                                            ?.message
                                            ?: "Unable to complete task."
                                }

                            } catch (e: Exception) {

                                Log.e(
                                    "TASK_COMPLETE",
                                    "Task completion failed",
                                    e
                                )

                                aiError =
                                    e.message
                                        ?: "Unable to complete task."

                            } finally {

                                isCompletingTask =
                                    false
                            }
                        }
                    }
                }
            )


            actionMessage?.let { message ->

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color(0xFFE1F5E7)
                        ),

                    shape =
                        RoundedCornerShape(12.dp)
                ) {

                    Row(
                        modifier =
                            Modifier.padding(12.dp),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.CheckCircle,

                            contentDescription =
                                null,

                            tint =
                                TaskDetailGreen
                        )

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        Text(
                            text =
                                message,

                            color =
                                TaskDetailGreen,

                            fontSize =
                                11.sp,

                            fontWeight =
                                FontWeight.Medium
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(30.dp)
            )
        }
    }
}


// ============================================================
// TASK HEADER
// ============================================================

@Composable
private fun TaskHeaderCard(
    task: SiteTask,
    status: String = task.status
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )

    ) {

        Column(

            modifier =
                Modifier.padding(18.dp)

        ) {


            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically

            ) {


                Box(

                    modifier =
                        Modifier
                            .size(10.dp)
                            .background(
                                task.indicatorColor,
                                CircleShape
                            )
                )


                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )


                Text(

                    text =
                        task.title,

                    modifier =
                        Modifier.weight(1f),

                    fontSize =
                        20.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                TaskDetailStatusBadge(
                    status =
                        status
                )
            }


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(

                    imageVector =
                        Icons.Outlined.BusinessCenter,

                    contentDescription =
                        null,

                    modifier =
                        Modifier.size(17.dp),

                    tint =
                        TaskDetailOrange
                )


                Spacer(
                    modifier =
                        Modifier.width(7.dp)
                )


                Text(

                    text =
                        task.project,

                    fontSize =
                        13.sp,

                    fontWeight =
                        FontWeight.Medium
                )
            }
        }
    }
}


// ============================================================
// TASK INFORMATION
// ============================================================

@Composable
private fun TaskInformationCard(
    task: SiteTask
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )

    ) {

        Column(

            modifier =
                Modifier.padding(18.dp)

        ) {


            Text(

                text =
                    "Task Information",

                fontSize =
                    16.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )


            InformationRow(

                icon =
                    Icons.Outlined.Person,

                label =
                    "Assigned To",

                value =
                    task.assignee
            )


            InformationRow(

                icon =
                    Icons.Outlined.Layers,

                label =
                    "Phase",

                value =
                    task.phase
            )


            InformationRow(

                icon =
                    Icons.Outlined.Flag,

                label =
                    "Priority",

                value =
                    task.priority
            )


            InformationRow(

                icon =
                    Icons.Outlined.Schedule,

                label =
                    "Due Date",

                value =
                    task.schedule
            )


            InformationRow(

                icon =
                    Icons.Outlined.Tag,

                label =
                    "Task ID",

                value =
                    task.id
            )
        }
    }
}


// ============================================================
// INFORMATION ROW
// ============================================================

@Composable
private fun InformationRow(

    icon: androidx.compose.ui.graphics.vector.ImageVector,

    label: String,

    value: String

) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical =
                        7.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically

    ) {


        Icon(

            imageVector =
                icon,

            contentDescription =
                null,

            modifier =
                Modifier.size(19.dp),

            tint =
                TaskDetailGray
        )


        Spacer(
            modifier =
                Modifier.width(10.dp)
        )


        Column {
            val displayValue = if (label.equals("Task ID", ignoreCase = true) && value.length > 8) {
                "#${value.take(8).uppercase()}"
            } else {
                value
            }

            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = TaskDetailGray
            )

            Text(
                text = displayValue,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
        }
    }
}


// ============================================================
// TASK SUBTASKS CARD
// ============================================================

@Composable
private fun TaskSubtasksCard(
    subtasks: List<SubtaskItem>,
    isUpdating: Boolean,
    isProjectActive: Boolean = true,
    projectStatus: String? = null,
    onToggleSubtask: (String) -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            val completedCount = subtasks.count { it.completed }
            val totalCount = subtasks.size

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Checklist,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isProjectActive) TaskDetailOrange else Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Subtasks & Steps",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.weight(1f))

                if (!isProjectActive) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFFD97706)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Planning • Locked",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                } else if (totalCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (completedCount == totalCount) Color(0xFFE1F5E7) else Color(0xFFFFE5D8),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$completedCount of $totalCount done",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (completedCount == totalCount) TaskDetailGreen else TaskDetailOrange
                        )
                    }
                }
            }

            // Locked banner when project is in planning
            if (!isProjectActive) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFFBEB), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Project is in Planning phase. Subtasks are locked until construction is activated.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (subtasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No subtasks recorded for this task.",
                        fontSize = 12.sp,
                        color = TaskDetailGray
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subtasks.forEach { item ->
                        val isDone = item.completed
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = when {
                                        !isProjectActive -> Color(0xFFF1F5F9).copy(alpha = 0.5f)
                                        isDone -> Color(0xFFF8FAFC)
                                        else -> Color(0xFFFAFAFA)
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    if (!isProjectActive) {
                                        Toast.makeText(
                                            context,
                                            "Project is in planning and not activated yet. Subtasks cannot be modified.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        onToggleSubtask(item.id)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isDone,
                                enabled = isProjectActive,
                                onCheckedChange = {
                                    if (!isProjectActive) {
                                        Toast.makeText(
                                            context,
                                            "Project is in planning and not activated yet. Subtasks cannot be modified.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        onToggleSubtask(item.id)
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = TaskDetailOrange,
                                    uncheckedColor = if (!isProjectActive) Color(0xFFCBD5E1) else Color(0xFF94A3B8),
                                    checkmarkColor = Color.White,
                                    disabledCheckedColor = TaskDetailOrange.copy(alpha = 0.4f),
                                    disabledUncheckedColor = Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = item.title.ifBlank { "Untitled Step" },
                                fontSize = 13.sp,
                                fontWeight = if (isDone) FontWeight.Normal else FontWeight.Medium,
                                color = when {
                                    !isProjectActive -> Color(0xFF94A3B8)
                                    isDone -> TaskDetailGray
                                    else -> Color(0xFF1E293B)
                                },
                                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                modifier = Modifier.weight(1f)
                            )

                            if (!isProjectActive) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = "Locked",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ============================================================
// PROGRESS CARD
// ============================================================

@Composable
private fun TaskProgressCard(
    task: SiteTask,
    progress: Int = task.progress,
    status: String = task.status
) {

    val safeProgress =
        if (
            status.equals(
                "Completed",
                ignoreCase = true
            ) ||
            status.equals(
                "Done",
                ignoreCase = true
            ) ||
            status.equals(
                "Approved",
                ignoreCase = true
            )
        ) {
            100
        } else {
            progress
                .coerceIn(
                    0,
                    100
                )
        }


    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )

    ) {

        Column(

            modifier =
                Modifier.padding(18.dp)

        ) {


            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically

            ) {


                Text(

                    text =
                        "Task Progress",

                    fontSize =
                        16.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.weight(1f)
                )


                Text(

                    text =
                        "$safeProgress%",

                    fontSize =
                        17.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        TaskDetailOrange
                )
            }


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            LinearProgressIndicator(

                progress = {
                    safeProgress / 100f
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp),

                color =
                    TaskDetailOrange,

                trackColor =
                    Color(0xFFEAE4E1)
            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            Text(

                text =
                    when {

                        safeProgress >= 100 ->
                            "Task completed"

                        safeProgress >= 75 ->
                            "Task is nearing completion"

                        safeProgress >= 50 ->
                            "Task is in progress"

                        safeProgress > 0 ->
                            "Task has started"

                        else ->
                            "Task has not started"
                    },

                fontSize =
                    10.sp,

                color =
                    TaskDetailGray
            )
        }
    }
}


// ============================================================
// FIELD EVIDENCE / CAMERA + ENGINEER ANNOTATION
// ============================================================

@Composable
private fun FieldEvidenceCard(

    capturedPhotoUri: Uri?,

    annotations: List<FieldAnnotation>,

    isProjectActive: Boolean = true,

    onAnnotationsChange:
        (List<FieldAnnotation>) -> Unit,

    onCameraClick: () -> Unit,

    onGalleryClick: () -> Unit = {}

) {

    val context =
        LocalContext.current

    val latestAnnotations =
        rememberUpdatedState(annotations)


    // ============================================================
    // ANNOTATION STATE
    // ============================================================

    var annotationMode by remember {
        mutableStateOf(false)
    }

    var imageAreaSize by remember {
        mutableStateOf(
            androidx.compose.ui.unit.IntSize.Zero
        )
    }

    var dragStart by remember {
        mutableStateOf<Offset?>(null)
    }

    var dragEnd by remember {
        mutableStateOf<Offset?>(null)
    }

    var pendingBox by remember {
        mutableStateOf<PendingAnnotationBox?>(null)
    }

    var showAnnotationDialog by remember {
        mutableStateOf(false)
    }

    var selectedAnnotationId by remember {
        mutableStateOf<Long?>(null)
    }

    var movingAnnotationId by remember {
        mutableStateOf<Long?>(null)
    }


    // ============================================================
    // CARD
    // ============================================================

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )

    ) {

        Column(

            modifier =
                Modifier.padding(18.dp)

        ) {


            // ====================================================
            // FIELD EVIDENCE HEADER
            // ====================================================

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                Icon(

                    imageVector =
                        Icons.Outlined.CameraAlt,

                    contentDescription =
                        null,

                    tint =
                        if (isProjectActive) TaskDetailOrange else Color(0xFF94A3B8)
                )


                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )


                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(

                        text =
                            "Field Evidence",

                        fontSize =
                            16.sp,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Text(

                        text =
                            "Capture a photo, then manually draw and label work areas.",

                        fontSize =
                            10.sp,

                        color =
                            TaskDetailGray
                    )
                }

                if (!isProjectActive) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFFD97706)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Planning • Locked",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                }
            }

            // Locked banner when project is in planning
            if (!isProjectActive) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFFBEB), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Project is in Planning phase. Field evidence capture is locked until construction is activated.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )


            // ====================================================
            // CAMERA & GALLERY BUTTONS
            // ====================================================

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (!isProjectActive) {
                            Toast.makeText(
                                context,
                                "Project is currently in planning. Field evidence cannot be captured until activated.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            onCameraClick()
                        }
                    },
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(48.dp),
                    shape =
                        RoundedCornerShape(12.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                if (isProjectActive) TaskDetailOrange else Color(0xFFE2E8F0),
                            contentColor =
                                if (isProjectActive) Color.White else Color(0xFF94A3B8)
                        )
                ) {
                    Icon(
                        imageVector =
                            if (!isProjectActive) Icons.Outlined.Lock else Icons.Outlined.CameraAlt,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(18.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.width(6.dp)
                    )

                    Text(
                        text =
                            if (!isProjectActive) {
                                "Camera (Locked)"
                            } else if (capturedPhotoUri == null) {
                                "Camera"
                            } else {
                                "Retake"
                            },
                        fontSize =
                            13.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        if (!isProjectActive) {
                            Toast.makeText(
                                context,
                                "Project is currently in planning. Field evidence cannot be captured until activated.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            onGalleryClick()
                        }
                    },
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(48.dp),
                    shape =
                        RoundedCornerShape(12.dp),
                    border =
                        BorderStroke(
                            1.dp,
                            if (isProjectActive) TaskDetailOrange else Color(0xFFE2E8F0)
                        ),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor =
                                if (isProjectActive) TaskDetailOrange else Color(0xFF94A3B8)
                        )
                ) {
                    Icon(
                        imageVector =
                            if (!isProjectActive) Icons.Outlined.Lock else Icons.Outlined.PhotoLibrary,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(18.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.width(6.dp)
                    )

                    Text(
                        text =
                            if (!isProjectActive) "Gallery (Locked)" else "Gallery",
                        fontSize =
                            13.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }


            // ====================================================
            // CAPTURED PHOTO
            // ====================================================

            if (capturedPhotoUri != null) {


                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )


                // =================================================
                // PHOTO HEADER
                // =================================================

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically

                ) {

                    Column(

                        modifier =
                            Modifier.weight(1f)

                    ) {

                        Text(

                            text =
                                "Captured Photo",

                            fontSize =
                                13.sp,

                            fontWeight =
                                FontWeight.Bold
                        )


                        Text(

                            text =
                                if (annotationMode) {

                                    "Touch and drag over the photo to create a work area."

                                } else {

                                    "Draw a new work area or drag an existing box to reposition it."
                                },

                            fontSize =
                                9.sp,

                            color =
                                TaskDetailGray
                        )
                    }


                    // =============================================
                    // DRAW WORK AREA BUTTON
                    // =============================================

                    TextButton(

                        onClick = {

                            if (!isProjectActive) {
                                Toast.makeText(
                                    context,
                                    "Project is currently in planning. Annotations are locked.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                annotationMode =
                                    !annotationMode

                                dragStart =
                                    null

                                dragEnd =
                                    null

                                movingAnnotationId =
                                    null

                                selectedAnnotationId =
                                    null
                            }
                        }

                    ) {

                        Icon(

                            imageVector =
                                if (annotationMode) {

                                    Icons.Outlined.Close

                                } else {

                                    Icons.Outlined.AddBox
                                },

                            contentDescription =
                                null,

                            modifier =
                                Modifier.size(17.dp)
                        )


                        Spacer(
                            modifier =
                                Modifier.width(4.dp)
                        )


                        Text(

                            text =
                                if (annotationMode) {

                                    "Cancel Drawing"

                                } else {

                                    "Draw Work Area"
                                }
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                // =================================================
                // LOAD BITMAP
                // =================================================

                val bitmap =
                    remember(capturedPhotoUri) {

                        try {

                            context
                                .contentResolver
                                .openInputStream(
                                    capturedPhotoUri
                                )
                                ?.use { inputStream ->

                                    BitmapFactory
                                        .decodeStream(
                                            inputStream
                                        )
                                }

                        } catch (e: Exception) {

                            e.printStackTrace()

                            null
                        }
                    }


                // =================================================
                // DISPLAY + ANNOTATE IMAGE
                // =================================================

                if (bitmap != null) {

                    val imageAspectRatio =
                        if (bitmap.height > 0) {

                            bitmap.width.toFloat() /
                                    bitmap.height.toFloat()

                        } else {

                            1f
                        }


                    Card(

                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(14.dp),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Color(0xFFF4F4F4)
                            )

                    ) {

                        Box(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(
                                        imageAspectRatio
                                    )
                                    .onSizeChanged {

                                        imageAreaSize =
                                            it
                                    }

                        ) {


                            // ============================================
                            // ORIGINAL IMAGE
                            // ============================================

                            Image(

                                bitmap =
                                    bitmap.asImageBitmap(),

                                contentDescription =
                                    "Captured field evidence",

                                modifier =
                                    Modifier.fillMaxSize(),

                                contentScale =
                                    ContentScale.Fit
                            )


                            // ============================================
                            // SAVED ANNOTATIONS
                            // ============================================

                            Canvas(

                                modifier =
                                    Modifier.fillMaxSize()

                            ) {


                                annotations
                                    .forEach { annotation ->


                                        val left =
                                            annotation.left *
                                                    size.width


                                        val top =
                                            annotation.top *
                                                    size.height


                                        val right =
                                            annotation.right *
                                                    size.width


                                        val bottom =
                                            annotation.bottom *
                                                    size.height


                                        val boxWidth =
                                            (
                                                    right -
                                                            left
                                                    )
                                                .coerceAtLeast(
                                                    1f
                                                )


                                        val boxHeight =
                                            (
                                                    bottom -
                                                            top
                                                    )
                                                .coerceAtLeast(
                                                    1f
                                                )


                                        val isSelected =
                                            annotation.id ==
                                                    selectedAnnotationId


                                        val currentBoxColor =
                                            if (isSelected) {

                                                TaskDetailGreen

                                            } else {

                                                TaskDetailOrange
                                            }


                                        // =================================
                                        // DRAW RECTANGLE
                                        // =================================

                                        drawRect(

                                            color =
                                                currentBoxColor,

                                            topLeft =
                                                Offset(
                                                    left,
                                                    top
                                                ),

                                            size =
                                                Size(
                                                    boxWidth,
                                                    boxHeight
                                                ),

                                            style =
                                                Stroke(

                                                    width =
                                                        if (isSelected) {

                                                            8f

                                                        } else {

                                                            5f
                                                        }
                                                )
                                        )


                                        // =================================
                                        // LABEL TEXT
                                        // =================================

                                        val labelText =
                                            buildString {

                                                append(
                                                    annotation.label
                                                )

                                                annotation.progress
                                                    ?.let {

                                                        append(
                                                            " • $it%"
                                                        )
                                                    }
                                            }


                                        val textPaint =
                                            android.graphics.Paint()
                                                .apply {

                                                    color =
                                                        android.graphics.Color.WHITE

                                                    textSize =
                                                        30f

                                                    isAntiAlias =
                                                        true

                                                    typeface =
                                                        android.graphics.Typeface
                                                            .DEFAULT_BOLD
                                                }


                                        val backgroundPaint =
                                            android.graphics.Paint()
                                                .apply {

                                                    color =
                                                        currentBoxColor
                                                            .toArgb()

                                                    style =
                                                        android.graphics.Paint.Style.FILL
                                                }


                                        val textWidth =
                                            textPaint
                                                .measureText(
                                                    labelText
                                                )


                                        val labelLeft =
                                            left


                                        val labelTop =
                                            (
                                                    top -
                                                            38f
                                                    )
                                                .coerceAtLeast(
                                                    0f
                                                )


                                        drawContext
                                            .canvas
                                            .nativeCanvas
                                            .drawRect(

                                                labelLeft,

                                                labelTop,

                                                (
                                                        labelLeft +
                                                                textWidth +
                                                                20f
                                                        )
                                                    .coerceAtMost(
                                                        size.width
                                                    ),

                                                labelTop +
                                                        38f,

                                                backgroundPaint
                                            )


                                        drawContext
                                            .canvas
                                            .nativeCanvas
                                            .drawText(

                                                labelText,

                                                labelLeft +
                                                        10f,

                                                labelTop +
                                                        29f,

                                                textPaint
                                            )


                                        // =================================
                                        // SELECTED BOX HANDLES
                                        // =================================

                                        if (isSelected) {


                                            drawCircle(

                                                color =
                                                    TaskDetailGreen,

                                                radius =
                                                    10f,

                                                center =
                                                    Offset(
                                                        left,
                                                        top
                                                    )
                                            )


                                            drawCircle(

                                                color =
                                                    TaskDetailGreen,

                                                radius =
                                                    10f,

                                                center =
                                                    Offset(
                                                        right,
                                                        top
                                                    )
                                            )


                                            drawCircle(

                                                color =
                                                    TaskDetailGreen,

                                                radius =
                                                    10f,

                                                center =
                                                    Offset(
                                                        left,
                                                        bottom
                                                    )
                                            )


                                            drawCircle(

                                                color =
                                                    TaskDetailGreen,

                                                radius =
                                                    10f,

                                                center =
                                                    Offset(
                                                        right,
                                                        bottom
                                                    )
                                            )
                                        }
                                    }


                                // ========================================
                                // CURRENT RECTANGLE BEING DRAWN
                                // ========================================

                                val startPoint =
                                    dragStart


                                val endPoint =
                                    dragEnd


                                if (
                                    annotationMode &&
                                    startPoint != null &&
                                    endPoint != null
                                ) {


                                    val left =
                                        min(
                                            startPoint.x,
                                            endPoint.x
                                        )


                                    val top =
                                        min(
                                            startPoint.y,
                                            endPoint.y
                                        )


                                    val right =
                                        max(
                                            startPoint.x,
                                            endPoint.x
                                        )


                                    val bottom =
                                        max(
                                            startPoint.y,
                                            endPoint.y
                                        )


                                    drawRect(

                                        color =
                                            TaskDetailGreen,

                                        topLeft =
                                            Offset(
                                                left,
                                                top
                                            ),

                                        size =
                                            Size(

                                                (
                                                        right -
                                                                left
                                                        )
                                                    .coerceAtLeast(
                                                        1f
                                                    ),

                                                (
                                                        bottom -
                                                                top
                                                        )
                                                    .coerceAtLeast(
                                                        1f
                                                    )
                                            ),

                                        style =
                                            Stroke(
                                                width =
                                                    6f
                                            )
                                    )
                                }
                            }


                            // ============================================
                            // TOUCH LAYER
                            // ============================================

                            Box(

                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .pointerInput(
                                            annotationMode,
                                            imageAreaSize
                                        ) {


                                            detectDragGestures(


                                                // =================================
                                                // DRAG START
                                                // =================================

                                                onDragStart = { offset ->


                                                    // =============================
                                                    // CREATE NEW BOX
                                                    // =============================

                                                    if (annotationMode) {


                                                        selectedAnnotationId =
                                                            null


                                                        movingAnnotationId =
                                                            null


                                                        dragStart =
                                                            offset


                                                        dragEnd =
                                                            offset


                                                    } else {


                                                        // =========================
                                                        // FIND EXISTING BOX
                                                        // =========================

                                                        val currentAnnotations =
                                                            latestAnnotations.value


                                                        val touchedAnnotation =
                                                            currentAnnotations
                                                                .lastOrNull {
                                                                        annotation ->


                                                                    val left =
                                                                        annotation.left *
                                                                                imageAreaSize.width
                                                                                    .toFloat()


                                                                    val top =
                                                                        annotation.top *
                                                                                imageAreaSize.height
                                                                                    .toFloat()


                                                                    val right =
                                                                        annotation.right *
                                                                                imageAreaSize.width
                                                                                    .toFloat()


                                                                    val bottom =
                                                                        annotation.bottom *
                                                                                imageAreaSize.height
                                                                                    .toFloat()


                                                                    offset.x >=
                                                                            left &&
                                                                            offset.x <=
                                                                            right &&
                                                                            offset.y >=
                                                                            top &&
                                                                            offset.y <=
                                                                            bottom
                                                                }


                                                        if (
                                                            touchedAnnotation !=
                                                            null
                                                        ) {


                                                            selectedAnnotationId =
                                                                touchedAnnotation.id


                                                            movingAnnotationId =
                                                                touchedAnnotation.id


                                                        } else {


                                                            selectedAnnotationId =
                                                                null


                                                            movingAnnotationId =
                                                                null
                                                        }
                                                    }
                                                },


                                                // =================================
                                                // DRAGGING
                                                // =================================

                                                onDrag = {
                                                        change,
                                                        dragAmount ->


                                                    change.consume()


                                                    // =============================
                                                    // DRAW NEW BOX
                                                    // =============================

                                                    if (annotationMode) {


                                                        val previous =
                                                            dragEnd
                                                                ?: dragStart
                                                                ?: Offset.Zero


                                                        val next =
                                                            previous +
                                                                    dragAmount


                                                        dragEnd =
                                                            Offset(

                                                                x =
                                                                    next.x
                                                                        .coerceIn(

                                                                            0f,

                                                                            imageAreaSize.width
                                                                                .toFloat()
                                                                        ),

                                                                y =
                                                                    next.y
                                                                        .coerceIn(

                                                                            0f,

                                                                            imageAreaSize.height
                                                                                .toFloat()
                                                                        )
                                                            )


                                                    } else {


                                                        // =========================
                                                        // MOVE EXISTING BOX
                                                        // =========================

                                                        val movingId =
                                                            movingAnnotationId


                                                        if (
                                                            movingId != null &&
                                                            imageAreaSize.width > 0 &&
                                                            imageAreaSize.height > 0
                                                        ) {


                                                            val currentAnnotations =
                                                                latestAnnotations.value


                                                            val normalizedDx =
                                                                dragAmount.x /
                                                                        imageAreaSize.width
                                                                            .toFloat()


                                                            val normalizedDy =
                                                                dragAmount.y /
                                                                        imageAreaSize.height
                                                                            .toFloat()


                                                            val updatedAnnotations =
                                                                currentAnnotations
                                                                    .map {
                                                                            annotation ->


                                                                        if (
                                                                            annotation.id !=
                                                                            movingId
                                                                        ) {


                                                                            annotation


                                                                        } else {


                                                                            val boxWidth =
                                                                                (
                                                                                        annotation.right -
                                                                                                annotation.left
                                                                                        )
                                                                                    .coerceAtLeast(
                                                                                        0f
                                                                                    )


                                                                            val boxHeight =
                                                                                (
                                                                                        annotation.bottom -
                                                                                                annotation.top
                                                                                        )
                                                                                    .coerceAtLeast(
                                                                                        0f
                                                                                    )


                                                                            val maximumLeft =
                                                                                (
                                                                                        1f -
                                                                                                boxWidth
                                                                                        )
                                                                                    .coerceAtLeast(
                                                                                        0f
                                                                                    )


                                                                            val maximumTop =
                                                                                (
                                                                                        1f -
                                                                                                boxHeight
                                                                                        )
                                                                                    .coerceAtLeast(
                                                                                        0f
                                                                                    )


                                                                            val newLeft =
                                                                                (
                                                                                        annotation.left +
                                                                                                normalizedDx
                                                                                        )
                                                                                    .coerceIn(
                                                                                        0f,
                                                                                        maximumLeft
                                                                                    )


                                                                            val newTop =
                                                                                (
                                                                                        annotation.top +
                                                                                                normalizedDy
                                                                                        )
                                                                                    .coerceIn(
                                                                                        0f,
                                                                                        maximumTop
                                                                                    )


                                                                            annotation.copy(

                                                                                left =
                                                                                    newLeft,

                                                                                top =
                                                                                    newTop,

                                                                                right =
                                                                                    newLeft +
                                                                                            boxWidth,

                                                                                bottom =
                                                                                    newTop +
                                                                                            boxHeight
                                                                            )
                                                                        }
                                                                    }


                                                            onAnnotationsChange(
                                                                updatedAnnotations
                                                            )
                                                        }
                                                    }
                                                },


                                                // =================================
                                                // CANCEL DRAG
                                                // =================================

                                                onDragCancel = {


                                                    dragStart =
                                                        null


                                                    dragEnd =
                                                        null


                                                    movingAnnotationId =
                                                        null
                                                },


                                                // =================================
                                                // DRAG END
                                                // =================================

                                                onDragEnd = {


                                                    // =============================
                                                    // NEW RECTANGLE FINISHED
                                                    // =============================

                                                    if (annotationMode) {


                                                        val start =
                                                            dragStart


                                                        val end =
                                                            dragEnd


                                                        if (
                                                            start != null &&
                                                            end != null &&
                                                            imageAreaSize.width > 0 &&
                                                            imageAreaSize.height > 0
                                                        ) {


                                                            val left =
                                                                min(
                                                                    start.x,
                                                                    end.x
                                                                )


                                                            val top =
                                                                min(
                                                                    start.y,
                                                                    end.y
                                                                )


                                                            val right =
                                                                max(
                                                                    start.x,
                                                                    end.x
                                                                )


                                                            val bottom =
                                                                max(
                                                                    start.y,
                                                                    end.y
                                                                )


                                                            val boxWidth =
                                                                abs(
                                                                    right -
                                                                            left
                                                                )


                                                            val boxHeight =
                                                                abs(
                                                                    bottom -
                                                                            top
                                                                )


                                                            // Ignore tiny accidental taps.
                                                            if (
                                                                boxWidth >=
                                                                30f &&
                                                                boxHeight >=
                                                                30f
                                                            ) {


                                                                pendingBox =
                                                                    PendingAnnotationBox(

                                                                        left =
                                                                            (
                                                                                    left /
                                                                                            imageAreaSize.width
                                                                                                .toFloat()
                                                                                    )
                                                                                .coerceIn(
                                                                                    0f,
                                                                                    1f
                                                                                ),

                                                                        top =
                                                                            (
                                                                                    top /
                                                                                            imageAreaSize.height
                                                                                                .toFloat()
                                                                                    )
                                                                                .coerceIn(
                                                                                    0f,
                                                                                    1f
                                                                                ),

                                                                        right =
                                                                            (
                                                                                    right /
                                                                                            imageAreaSize.width
                                                                                                .toFloat()
                                                                                    )
                                                                                .coerceIn(
                                                                                    0f,
                                                                                    1f
                                                                                ),

                                                                        bottom =
                                                                            (
                                                                                    bottom /
                                                                                            imageAreaSize.height
                                                                                                .toFloat()
                                                                                    )
                                                                                .coerceIn(
                                                                                    0f,
                                                                                    1f
                                                                                )
                                                                    )


                                                                showAnnotationDialog =
                                                                    true
                                                            }
                                                        }


                                                        dragStart =
                                                            null


                                                        dragEnd =
                                                            null


                                                    } else {


                                                        // =========================
                                                        // MOVING BOX FINISHED
                                                        // =========================

                                                        movingAnnotationId =
                                                            null
                                                    }
                                                }
                                            )
                                        }
                            )
                        }
                    }


                    // =================================================
                    // DRAW MODE MESSAGE
                    // =================================================

                    if (annotationMode) {


                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )


                        Row(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(

                                        Color(
                                            0xFFFFF1EB
                                        ),

                                        RoundedCornerShape(
                                            10.dp
                                        )
                                    )
                                    .padding(
                                        10.dp
                                    ),

                            verticalAlignment =
                                Alignment.CenterVertically

                        ) {


                            Icon(

                                imageVector =
                                    Icons.Outlined.TouchApp,

                                contentDescription =
                                    null,

                                tint =
                                    TaskDetailOrange,

                                modifier =
                                    Modifier.size(
                                        18.dp
                                    )
                            )


                            Spacer(
                                modifier =
                                    Modifier.width(
                                        7.dp
                                    )
                            )


                            Text(

                                text =
                                    "Touch one corner of the work area and drag to the opposite corner. Release your finger to add the label.",

                                modifier =
                                    Modifier.weight(
                                        1f
                                    ),

                                fontSize =
                                    10.sp,

                                color =
                                    TaskDetailGray
                            )
                        }
                    }


                    // =================================================
                    // SELECTED BOX MESSAGE
                    // =================================================

                    if (
                        selectedAnnotationId != null &&
                        !annotationMode
                    ) {


                        Spacer(
                            modifier =
                                Modifier.height(
                                    8.dp
                                )
                        )


                        Row(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(

                                        Color(
                                            0xFFE8F5E9
                                        ),

                                        RoundedCornerShape(
                                            10.dp
                                        )
                                    )
                                    .padding(
                                        10.dp
                                    ),

                            verticalAlignment =
                                Alignment.CenterVertically

                        ) {


                            Icon(

                                imageVector =
                                    Icons.Outlined.OpenWith,

                                contentDescription =
                                    null,

                                tint =
                                    TaskDetailGreen,

                                modifier =
                                    Modifier.size(
                                        18.dp
                                    )
                            )


                            Spacer(
                                modifier =
                                    Modifier.width(
                                        7.dp
                                    )
                            )


                            Text(

                                text =
                                    "Work area selected. Drag inside the green box to move it.",

                                modifier =
                                    Modifier.weight(
                                        1f
                                    ),

                                fontSize =
                                    10.sp,

                                color =
                                    TaskDetailGray
                            )
                        }
                    }


                    // =================================================
                    // ENGINEER LABEL LIST
                    // =================================================

                    if (
                        annotations.isNotEmpty()
                    ) {


                        Spacer(
                            modifier =
                                Modifier.height(
                                    14.dp
                                )
                        )


                        Text(

                            text =
                                "Engineer Labels",

                            fontSize =
                                13.sp,

                            fontWeight =
                                FontWeight.Bold
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    8.dp
                                )
                        )


                        annotations
                            .forEachIndexed {
                                    index,
                                    annotation ->


                                val isSelected =
                                    annotation.id ==
                                            selectedAnnotationId


                                Card(

                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(

                                                bottom =
                                                    8.dp
                                            ),

                                    shape =
                                        RoundedCornerShape(
                                            12.dp
                                        ),

                                    colors =
                                        CardDefaults
                                            .cardColors(

                                                containerColor =
                                                    if (
                                                        isSelected
                                                    ) {

                                                        Color(
                                                            0xFFE8F5E9
                                                        )

                                                    } else {

                                                        Color(
                                                            0xFFF8F8F8
                                                        )
                                                    }
                                            )

                                ) {


                                    Row(

                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    12.dp
                                                ),

                                        verticalAlignment =
                                            Alignment.CenterVertically

                                    ) {


                                        Column(

                                            modifier =
                                                Modifier.weight(
                                                    1f
                                                )

                                        ) {


                                            Text(

                                                text =
                                                    "${index + 1}. ${annotation.label}",

                                                fontSize =
                                                    11.sp,

                                                fontWeight =
                                                    FontWeight.Bold
                                            )


                                            Spacer(
                                                modifier =
                                                    Modifier.height(
                                                        3.dp
                                                    )
                                            )


                                            Text(

                                                text =
                                                    buildString {


                                                        append(
                                                            annotation.stage
                                                        )


                                                        annotation.progress
                                                            ?.let {


                                                                append(
                                                                    " • $it%"
                                                                )
                                                            }
                                                    },

                                                fontSize =
                                                    10.sp,

                                                color =
                                                    TaskDetailGray
                                            )
                                        }


                                        // =================================
                                        // SELECT BUTTON
                                        // =================================

                                        IconButton(

                                            onClick = {


                                                selectedAnnotationId =
                                                    if (
                                                        selectedAnnotationId ==
                                                        annotation.id
                                                    ) {

                                                        null

                                                    } else {

                                                        annotation.id
                                                    }
                                            }

                                        ) {


                                            Icon(

                                                imageVector =
                                                    Icons.Outlined.OpenWith,

                                                contentDescription =
                                                    "Select work area",

                                                tint =
                                                    if (
                                                        isSelected
                                                    ) {

                                                        TaskDetailGreen

                                                    } else {

                                                        TaskDetailGray
                                                    }
                                            )
                                        }


                                        // =================================
                                        // DELETE
                                        // =================================

                                        IconButton(

                                            onClick = {


                                                onAnnotationsChange(

                                                    annotations
                                                        .filterNot {


                                                            it.id ==
                                                                    annotation.id
                                                        }
                                                )


                                                if (
                                                    selectedAnnotationId ==
                                                    annotation.id
                                                ) {


                                                    selectedAnnotationId =
                                                        null
                                                }


                                                if (
                                                    movingAnnotationId ==
                                                    annotation.id
                                                ) {


                                                    movingAnnotationId =
                                                        null
                                                }
                                            }

                                        ) {


                                            Icon(

                                                imageVector =
                                                    Icons.Outlined.DeleteOutline,

                                                contentDescription =
                                                    "Delete annotation",

                                                tint =
                                                    TaskDetailRed
                                            )
                                        }
                                    }
                                }
                            }
                    }


                } else {


                    Text(

                        text =
                            "Unable to display captured photo.",

                        fontSize =
                            10.sp,

                        color =
                            TaskDetailRed
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )


                // =================================================
                // LOCAL SAVE MESSAGE
                // =================================================

                Row(

                    verticalAlignment =
                        Alignment.CenterVertically

                ) {


                    Icon(

                        imageVector =
                            Icons.Outlined.CheckCircle,

                        contentDescription =
                            null,

                        tint =
                            TaskDetailGreen,

                        modifier =
                            Modifier.size(
                                17.dp
                            )
                    )


                    Spacer(
                        modifier =
                            Modifier.width(
                                6.dp
                            )
                    )


                    Text(

                        text =
                            "Photo and engineer labels are kept locally on this device.",

                        fontSize =
                            10.sp,

                        fontWeight =
                            FontWeight.Medium,

                        color =
                            TaskDetailGreen
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )


            Text(
                text = "AI analysis automatically scans the high-resolution photo. Engineer labels provide verified manual evidence.",
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = TaskDetailGray
            )
        }
    }


    // ============================================================
    // ADD LABEL DIALOG
    // ============================================================

    if (
        showAnnotationDialog &&
        pendingBox != null
    ) {


        AddFieldAnnotationDialog(


            // ====================================================
            // CANCEL
            // ====================================================

            onDismiss = {


                showAnnotationDialog =
                    false


                pendingBox =
                    null


                dragStart =
                    null


                dragEnd =
                    null
            },


            // ====================================================
            // SAVE
            // ====================================================

            onSave = {
                    label,
                    stage,
                    progress ->


                val box =
                    pendingBox


                if (
                    box != null
                ) {


                    val newAnnotation =
                        FieldAnnotation(

                            label =
                                label,

                            stage =
                                stage,

                            progress =
                                progress,

                            left =
                                box.left,

                            top =
                                box.top,

                            right =
                                box.right,

                            bottom =
                                box.bottom
                        )


                    onAnnotationsChange(

                        annotations +
                                newAnnotation
                    )


                    selectedAnnotationId =
                        newAnnotation.id
                }


                showAnnotationDialog =
                    false


                pendingBox =
                    null


                dragStart =
                    null


                dragEnd =
                    null


                // User can move the newly created box now.
                annotationMode =
                    false
            }
        )
    }
}

// ============================================================
// TEMPORARY BOX BEFORE ENGINEER SAVES THE LABEL
// ============================================================

private data class PendingAnnotationBox(

    val left: Float,

    val top: Float,

    val right: Float,

    val bottom: Float
)


// ============================================================
// ADD FIELD ANNOTATION DIALOG
// ============================================================

@Composable
private fun AddFieldAnnotationDialog(

    onDismiss: () -> Unit,

    onSave:
        (
        label: String,
        stage: String,
        progress: Int?
    ) -> Unit

) {

    val workLabels =
        listOf(
            "Rebar Installation",
            "Formwork Installation",
            "Concrete Placement",
            "Masonry Work",
            "Wall Construction",
            "Plastering",
            "Painting",
            "Excavation",
            "Backfilling",
            "Scaffolding",
            "Electrical Installation",
            "Plumbing Installation",
            "Other"
        )


    val workStages =
        listOf(
            "Not Started",
            "Started",
            "In Progress",
            "Nearly Complete",
            "Completed"
        )


    var selectedLabel by remember {
        mutableStateOf(
            workLabels.first()
        )
    }

    var selectedStage by remember {
        mutableStateOf(
            "In Progress"
        )
    }

    var progressValue by remember {
        mutableStateOf(50f)
    }

    var includeProgress by remember {
        mutableStateOf(true)
    }

    var labelMenuExpanded by remember {
        mutableStateOf(false)
    }

    var stageMenuExpanded by remember {
        mutableStateOf(false)
    }


    AlertDialog(

        onDismissRequest =
            onDismiss,

        title = {

            Text(
                text =
                    "Add Work Label"
            )
        },

        text = {

            Column {

                Text(

                    text =
                        "Work Type",

                    fontSize =
                        11.sp,

                    fontWeight =
                        FontWeight.SemiBold
                )


                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )


                Box {

                    OutlinedButton(

                        onClick = {
                            labelMenuExpanded =
                                true
                        },

                        modifier =
                            Modifier.fillMaxWidth()

                    ) {

                        Text(
                            text =
                                selectedLabel,

                            modifier =
                                Modifier.weight(1f)
                        )


                        Icon(

                            imageVector =
                                Icons.Outlined.ArrowDropDown,

                            contentDescription =
                                null
                        )
                    }


                    DropdownMenu(

                        expanded =
                            labelMenuExpanded,

                        onDismissRequest = {
                            labelMenuExpanded =
                                false
                        }

                    ) {

                        workLabels
                            .forEach { label ->

                                DropdownMenuItem(

                                    text = {
                                        Text(
                                            text =
                                                label
                                        )
                                    },

                                    onClick = {

                                        selectedLabel =
                                            label

                                        labelMenuExpanded =
                                            false
                                    }
                                )
                            }
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )


                Text(

                    text =
                        "Stage",

                    fontSize =
                        11.sp,

                    fontWeight =
                        FontWeight.SemiBold
                )


                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )


                Box {

                    OutlinedButton(

                        onClick = {
                            stageMenuExpanded =
                                true
                        },

                        modifier =
                            Modifier.fillMaxWidth()

                    ) {

                        Text(

                            text =
                                selectedStage,

                            modifier =
                                Modifier.weight(1f)
                        )


                        Icon(

                            imageVector =
                                Icons.Outlined.ArrowDropDown,

                            contentDescription =
                                null
                        )
                    }


                    DropdownMenu(

                        expanded =
                            stageMenuExpanded,

                        onDismissRequest = {
                            stageMenuExpanded =
                                false
                        }

                    ) {

                        workStages
                            .forEach { stage ->

                                DropdownMenuItem(

                                    text = {
                                        Text(
                                            text =
                                                stage
                                        )
                                    },

                                    onClick = {

                                        selectedStage =
                                            stage

                                        stageMenuExpanded =
                                            false
                                    }
                                )
                            }
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )


                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically

                ) {

                    Checkbox(

                        checked =
                            includeProgress,

                        onCheckedChange = {
                            includeProgress =
                                it
                        }
                    )


                    Text(

                        text =
                            "Add engineer progress",

                        fontSize =
                            11.sp,

                        fontWeight =
                            FontWeight.Medium
                    )
                }


                if (includeProgress) {

                    Text(

                        text =
                            "Progress: ${progressValue.toInt()}%",

                        fontSize =
                            11.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            TaskDetailOrange
                    )


                    Slider(

                        value =
                            progressValue,

                        onValueChange = {

                            progressValue =
                                it
                        },

                        valueRange =
                            0f..100f,

                        steps =
                            19
                    )
                }
            }
        },

        confirmButton = {

            Button(

                onClick = {

                    onSave(

                        selectedLabel,

                        selectedStage,

                        if (includeProgress) {

                            progressValue
                                .toInt()
                                .coerceIn(
                                    0,
                                    100
                                )

                        } else {

                            null
                        }
                    )
                },

                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                TaskDetailOrange
                        )

            ) {

                Text(
                    text =
                        "Save Label"
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text(
                    text =
                        "Cancel"
                )
            }
        }
    )
}


// ============================================================
// REPORT CARD
// Manual issue report — separate from AI-generated reports.
// ============================================================

@Composable
private fun ReportTaskCard(
    task: SiteTask,
    isProjectActive: Boolean = true,
    onReportClick: () -> Unit
) {
    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()

    var isSending by remember {
        mutableStateOf(false)
    }

    var expanded by remember {
        mutableStateOf(false)
    }

    var reportTitle by remember {
        mutableStateOf("")
    }

    var reportMessage by remember {
        mutableStateOf("")
    }

    var attachmentUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var validationError by remember {
        mutableStateOf<String?>(null)
    }

    var sentMessage by remember {
        mutableStateOf<String?>(null)
    }

    val attachmentPicker =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {
                attachmentUri = uri
            }
        }


    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .animateContentSize(),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )
    ) {

        Column(
            modifier =
                Modifier.padding(18.dp)
        ) {

            // ========================================================
            // HEADER
            // ========================================================

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.ReportProblem,

                    contentDescription =
                        null,

                    tint =
                        if (isProjectActive) TaskDetailRed else Color(0xFF94A3B8)
                )

                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Report an Issue",

                        fontSize =
                            16.sp,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            "Manually report delays, defects, safety concerns or site problems.",

                        fontSize =
                            10.sp,

                        color =
                            TaskDetailGray
                    )
                }

                if (!isProjectActive) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFFD97706)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Planning • Locked",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = {
                            expanded =
                                !expanded

                            validationError =
                                null

                            sentMessage =
                                null
                        }
                    ) {

                        Icon(
                            imageVector =
                                if (expanded) {
                                    Icons.Outlined.KeyboardArrowUp
                                } else {
                                    Icons.Outlined.KeyboardArrowDown
                                },

                            contentDescription =
                                if (expanded) {
                                    "Collapse issue report"
                                } else {
                                    "Expand issue report"
                                },

                            tint =
                                TaskDetailRed
                        )
                    }
                }
            }

            // Locked banner when project is in planning
            if (!isProjectActive) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFFBEB), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Project is in Planning phase. Issue reporting is locked until construction is activated.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            // ========================================================
            // OPEN / CLOSE BUTTON
            // ========================================================

            OutlinedButton(
                onClick = {
                    if (!isProjectActive) {
                        Toast.makeText(
                            context,
                            "Project is currently in planning. Issue reports cannot be created until activated.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        expanded =
                            !expanded

                        validationError =
                            null

                        sentMessage =
                            null
                    }
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp),

                shape =
                    RoundedCornerShape(12.dp),

                border =
                    BorderStroke(
                        1.dp,
                        if (isProjectActive) TaskDetailRed else Color(0xFFCBD5E1)
                    ),

                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor =
                            if (isProjectActive) TaskDetailRed else Color(0xFF94A3B8)
                    )
            ) {

                Icon(
                    imageVector =
                        if (!isProjectActive) {
                            Icons.Outlined.Lock
                        } else if (expanded) {
                            Icons.Outlined.KeyboardArrowUp
                        } else {
                            Icons.Outlined.AddCircleOutline
                        },

                    contentDescription =
                        null
                )

                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )

                Text(
                    text =
                        if (!isProjectActive) {
                            "Issue Reporting Locked (Planning)"
                        } else if (expanded) {
                            "Hide Issue Form"
                        } else {
                            "Create Issue Report"
                        },

                    fontWeight =
                        FontWeight.Bold
                )
            }


            // ========================================================
            // SLIDE-DOWN ISSUE FORM
            // ========================================================

            AnimatedVisibility(
                visible =
                    expanded,

                enter =
                    fadeIn(
                        animationSpec =
                            tween(180)
                    ) +
                            expandVertically(
                                animationSpec =
                                    tween(220)
                            ),

                exit =
                    fadeOut(
                        animationSpec =
                            tween(120)
                    ) +
                            shrinkVertically(
                                animationSpec =
                                    tween(180)
                            )
            ) {

                Column {

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )


                    Text(
                        text =
                            "Issue Title",

                        fontSize =
                            11.sp,

                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    OutlinedTextField(
                        value =
                            reportTitle,

                        onValueChange = {
                            reportTitle =
                                it

                            validationError =
                                null

                            sentMessage =
                                null
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        placeholder = {
                            Text(
                                text =
                                    "e.g. Cracked concrete near Column C4",

                                fontSize =
                                    11.sp
                            )
                        },

                        singleLine =
                            true,

                        shape =
                            RoundedCornerShape(12.dp)
                    )


                    Spacer(
                        modifier =
                            Modifier.height(14.dp)
                    )


                    Text(
                        text =
                            "Message / Description",

                        fontSize =
                            11.sp,

                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    OutlinedTextField(
                        value =
                            reportMessage,

                        onValueChange = {
                            reportMessage =
                                it

                            validationError =
                                null

                            sentMessage =
                                null
                        },

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    min =
                                        120.dp
                                ),

                        placeholder = {
                            Text(
                                text =
                                    "Describe what happened, where it happened, and why the admin should review it.",

                                fontSize =
                                    11.sp
                            )
                        },

                        minLines =
                            5,

                        maxLines =
                            8,

                        shape =
                            RoundedCornerShape(12.dp)
                    )


                    Spacer(
                        modifier =
                            Modifier.height(14.dp)
                    )


                    Text(
                        text =
                            "Attachment",

                        fontSize =
                            11.sp,

                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )


                    OutlinedButton(
                        onClick = {
                            attachmentPicker.launch(
                                "image/*"
                            )
                        },

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp),

                        shape =
                            RoundedCornerShape(12.dp)
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.AttachFile,

                            contentDescription =
                                null
                        )

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        Text(
                            text =
                                if (attachmentUri == null) {
                                    "Attach Image"
                                } else {
                                    "Change Attachment"
                                },

                            fontWeight =
                                FontWeight.Medium
                        )
                    }


                    if (attachmentUri != null) {

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Card(
                            modifier =
                                Modifier.fillMaxWidth(),

                            shape =
                                RoundedCornerShape(10.dp),

                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        Color(0xFFF7F7F7)
                                )
                        ) {

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal =
                                                10.dp,
                                            vertical =
                                                8.dp
                                        ),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Outlined.Image,

                                    contentDescription =
                                        null,

                                    modifier =
                                        Modifier.size(18.dp),

                                    tint =
                                        TaskDetailOrange
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(8.dp)
                                )

                                Text(
                                    text =
                                        "Image attached",

                                    modifier =
                                        Modifier.weight(1f),

                                    fontSize =
                                        10.sp,

                                    color =
                                        TaskDetailGray
                                )

                                IconButton(
                                    onClick = {
                                        attachmentUri =
                                            null
                                    }
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Outlined.Close,

                                        contentDescription =
                                            "Remove attachment",

                                        tint =
                                            TaskDetailRed
                                    )
                                }
                            }
                        }
                    }


                    if (!validationError.isNullOrBlank()) {

                        Spacer(
                            modifier =
                                Modifier.height(10.dp)
                        )

                        Text(
                            text =
                                validationError!!,

                            fontSize =
                                10.sp,

                            color =
                                TaskDetailRed
                        )
                    }


                    if (!sentMessage.isNullOrBlank()) {

                        Spacer(
                            modifier =
                                Modifier.height(10.dp)
                        )

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Outlined.CheckCircle,

                                contentDescription =
                                    null,

                                modifier =
                                    Modifier.size(17.dp),

                                tint =
                                    TaskDetailGreen
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(6.dp)
                            )

                            Text(
                                text =
                                    sentMessage!!,

                                fontSize =
                                    10.sp,

                                fontWeight =
                                    FontWeight.Medium,

                                color =
                                    TaskDetailGreen
                            )
                        }
                    }


                    Spacer(
                        modifier =
                            Modifier.height(14.dp)
                    )


                    Button(
                        onClick = {

                            when {

                                reportTitle
                                    .trim()
                                    .isEmpty() -> {

                                    validationError =
                                        "Please enter a report title."
                                }

                                reportMessage
                                    .trim()
                                    .isEmpty() -> {

                                    validationError =
                                        "Please enter a report message."
                                }

                                else -> {

                                    if (!isProjectActive) {
                                        Toast.makeText(
                                            context,
                                            "Cannot send issue report: Project is in planning and not yet activated.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@Button
                                    }

                                    validationError = null
                                    sentMessage = null

                                    if (!isSending) {

                                        isSending = true

                                        scope.launch {

                                            try {

                                                Log.d(
                                                    "ISSUE_REPORT",
                                                    "taskId=${task.id}, projectId=${task.projectId}, projectCode=${task.projectCode}, project=${task.project}"
                                                )

                                                val response =
                                                    RetrofitClient.api
                                                        .createProjectIssue(
                                                            projectCode = task.projectCode
                                                                ?: throw IllegalStateException(
                                                                    "Project code is missing for this task."
                                                                ),
                                                            request =
                                                                CreateIssueRequest(
                                                                    title =
                                                                        reportTitle.trim(),

                                                                    category =
                                                                        "Safety Hazard",

                                                                    priority =
                                                                        task.priority.ifBlank {
                                                                            "Medium"
                                                                        },

                                                                    location =
                                                                        null,

                                                                    description =
                                                                        reportMessage.trim(),

                                                                    assigned_to =
                                                                        null
                                                                )
                                                        )


                                                if (
                                                    response.isSuccessful &&
                                                    response.body()?.success == true
                                                ) {

                                                    sentMessage =
                                                        response.body()?.message
                                                            ?: "Issue report sent successfully."

                                                    reportTitle = ""
                                                    reportMessage = ""
                                                    attachmentUri = null

                                                    onReportClick()

                                                } else {

                                                    validationError =
                                                        response.body()?.message
                                                            ?: "Unable to send issue report."
                                                }

                                            } catch (e: Exception) {

                                                Log.e(
                                                    "ISSUE_REPORT",
                                                    "Issue report failed",
                                                    e
                                                )

                                                validationError =
                                                    e.message
                                                        ?: "Unable to send issue report."

                                            } finally {

                                                isSending =
                                                    false
                                            }
                                        }
                                    }
                                }
                            }
                        },

                        enabled =
                            isProjectActive && !isSending,

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(50.dp),

                        shape =
                            RoundedCornerShape(12.dp),

                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    TaskDetailRed
                            )
                    ) {

                        if (isSending) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(19.dp),

                                strokeWidth =
                                    2.dp,

                                color =
                                    Color.White
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(8.dp)
                            )

                            Text(
                                text =
                                    "Sending...",

                                fontWeight =
                                    FontWeight.Bold
                            )

                        } else {

                            Icon(
                                imageVector =
                                    Icons.Outlined.Send,

                                contentDescription =
                                    null
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(8.dp)
                            )

                            Text(
                                text =
                                    "Send Issue Report",

                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}


// ============================================================
// FUTURE AI CARD
// ============================================================

@Composable
private fun AiFieldAnalysisCard(

    capturedPhotoUri: Uri?,

    aiModelInstalled: Boolean,

    isPreparing: Boolean,

    isDownloading: Boolean = false,

    downloadPct: Int = 0,

    downloadText: String = "",

    isGenerating: Boolean,

    detectedObjects: String,

    report: String?,

    error: String?,

    isUploading: Boolean,

    isCompleting: Boolean,

    reportUploaded: Boolean,

    taskCompleted: Boolean,

    manpowerList: List<ManpowerItem> = emptyList(),

    onManpowerListChange: (List<ManpowerItem>) -> Unit = {},

    onReportChange: (String) -> Unit = {},

    onGenerateReport: () -> Unit,

    onDownloadPdf: () -> Unit,

    onUploadReport: () -> Unit,

    onMarkDone: () -> Unit

){

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color(0xFFFFF1EB)
            )

    ) {

        Column(

            modifier =
                Modifier.padding(18.dp)

        ) {


            // ====================================================
            // HEADER
            // ====================================================

            Row(

                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                Icon(

                    imageVector =
                        Icons.Outlined.AutoAwesome,

                    contentDescription =
                        null,

                    tint =
                        TaskDetailOrange
                )


                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )


                Column {

                    Text(

                        text =
                            "AI Field Analysis",

                        fontSize =
                            16.sp,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Text(

                        text =
                            "YOLO object detection + bundled Gemma field reporting.",

                        fontSize =
                            10.sp,

                        color =
                            TaskDetailGray
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )


            // ====================================================
            // PHOTO STATUS
            // ====================================================

            AiStatusRow(

                good =
                    capturedPhotoUri != null,

                text =
                    if (capturedPhotoUri != null) {

                        "Field photo ready"

                    } else {

                        "Capture a field photo first"
                    }
            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            // ====================================================
            // GEMMA STATUS
            // ====================================================

            AiStatusRow(

                good =
                    aiModelInstalled,

                text =
                    when {

                        isDownloading ->
                            "Downloading Gemma AI model..."

                        isPreparing ->
                            "Preparing Gemma AI model..."

                        aiModelInstalled ->
                            "Gemma AI model ready"

                        else ->
                            "Gemma AI model is not ready"
                    }
            )


            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )


            // ====================================================
            // DOWNLOADING GEMMA — INLINE PROGRESS
            // ====================================================

            if (isDownloading) {

                Column {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = TaskDetailOrange
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "Downloading AI model ($downloadText)",
                            fontSize = 11.sp,
                            color = TaskDetailGray
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { downloadPct / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = TaskDetailOrange,
                        trackColor = Color(0xFFE0C8BB)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$downloadPct%",
                        fontSize = 10.sp,
                        color = TaskDetailGray,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))


            // ====================================================
            // PREPARING GEMMA (after download)
            // ====================================================

            } else if (isPreparing) {

                Row(

                    verticalAlignment =
                        Alignment.CenterVertically

                ) {

                    CircularProgressIndicator(

                        modifier =
                            Modifier.size(20.dp),

                        strokeWidth =
                            2.dp,

                        color =
                            TaskDetailOrange
                    )


                    Spacer(
                        modifier =
                            Modifier.width(10.dp)
                    )


                    Text(

                        text =
                            "Preparing AI for first use...",

                        fontSize =
                            11.sp,

                        color =
                            TaskDetailGray
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )
            }


            // ====================================================
            // MANPOWER INPUT SECTION
            // ====================================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFFFD8CC))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val totalManpower = manpowerList.sumOf { it.count.toIntOrNull() ?: 0 }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = TaskDetailOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Manpower Deployment",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFECE5)
                        ) {
                            Text(
                                text = "Total: $totalManpower",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TaskDetailOrange,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (manpowerList.isEmpty()) {
                        Text(
                            text = "No manpower added yet. Tap below to add:",
                            fontSize = 11.sp,
                            color = TaskDetailGray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    } else {
                        // Display ONLY the manpower items that were added
                        manpowerList.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.isPreset) {
                                    Text(
                                        text = item.role,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155),
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.CenterStart,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        ) {
                                            if (item.role.isEmpty()) {
                                                Text(
                                                    text = "Role name...",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF94A3B8)
                                                )
                                            }
                                            BasicTextField(
                                                value = item.role,
                                                onValueChange = { newRole ->
                                                    val updated = manpowerList.toMutableList()
                                                    updated[index] = item.copy(role = newRole)
                                                    onManpowerListChange(updated)
                                                },
                                                textStyle = TextStyle(
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF0F172A)
                                                ),
                                                singleLine = true
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable {
                                                val cur = item.count.toIntOrNull() ?: 0
                                                if (cur > 0) {
                                                    val updated = manpowerList.toMutableList()
                                                    updated[index] = item.copy(count = (cur - 1).toString())
                                                    onManpowerListChange(updated)
                                                }
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF1F5F9)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "−",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF475569)
                                            )
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .width(46.dp)
                                            .height(32.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            BasicTextField(
                                                value = item.count,
                                                onValueChange = { newText ->
                                                    if (newText.isEmpty() || (newText.all { it.isDigit() } && newText.length <= 3)) {
                                                        val updated = manpowerList.toMutableList()
                                                        updated[index] = item.copy(count = newText)
                                                        onManpowerListChange(updated)
                                                    }
                                                },
                                                textStyle = TextStyle(
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    textAlign = TextAlign.Center,
                                                    color = Color(0xFF0F172A)
                                                ),
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Number
                                                ),
                                                singleLine = true
                                            )
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable {
                                                val cur = item.count.toIntOrNull() ?: 0
                                                val updated = manpowerList.toMutableList()
                                                updated[index] = item.copy(count = (cur + 1).toString())
                                                onManpowerListChange(updated)
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFFECE5)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "+",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TaskDetailOrange
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            val updated = manpowerList.toMutableList()
                                            updated.removeAt(index)
                                            onManpowerListChange(updated)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Remove role",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Available role buttons to add
                    val hasPic = manpowerList.any { it.role.startsWith("PIC", ignoreCase = true) }
                    val hasHeadTech = manpowerList.any { it.role.startsWith("Head Tech", ignoreCase = true) }
                    val hasTech = manpowerList.any { it.role.equals("Technicians", ignoreCase = true) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!hasPic) {
                            Surface(
                                modifier = Modifier.clickable {
                                    onManpowerListChange(
                                        manpowerList + ManpowerItem(
                                            role = "PIC (Person In Charge)",
                                            count = "1",
                                            isPreset = true
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFF1EB),
                                border = BorderStroke(1.dp, Color(0xFFFFD8CC))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "+",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TaskDetailOrange
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "PIC",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TaskDetailOrange
                                    )
                                }
                            }
                        }

                        if (!hasHeadTech) {
                            Surface(
                                modifier = Modifier.clickable {
                                    onManpowerListChange(
                                        manpowerList + ManpowerItem(
                                            role = "Head Technician",
                                            count = "1",
                                            isPreset = true
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFF1EB),
                                border = BorderStroke(1.dp, Color(0xFFFFD8CC))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "+",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TaskDetailOrange
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Head Tech",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TaskDetailOrange
                                    )
                                }
                            }
                        }

                        if (!hasTech) {
                            Surface(
                                modifier = Modifier.clickable {
                                    onManpowerListChange(
                                        manpowerList + ManpowerItem(
                                            role = "Technicians",
                                            count = "1",
                                            isPreset = true
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFF1EB),
                                border = BorderStroke(1.dp, Color(0xFFFFD8CC))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "+",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TaskDetailOrange
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Technicians",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TaskDetailOrange
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.clickable {
                                onManpowerListChange(
                                    manpowerList + ManpowerItem(
                                        role = "",
                                        count = "1",
                                        isPreset = false
                                    )
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "+",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TaskDetailGray
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Other Role",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TaskDetailGray
                                )
                            }
                        }
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )


            // ====================================================
            // GENERATE REPORT
            // ====================================================

            Button(

                onClick =
                    onGenerateReport,

                enabled =
                    capturedPhotoUri != null &&
                            aiModelInstalled &&
                            !isPreparing &&
                            !isGenerating,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp),

                shape =
                    RoundedCornerShape(12.dp),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            TaskDetailOrange
                    )

            ) {

                if (isGenerating) {

                    CircularProgressIndicator(

                        modifier =
                            Modifier.size(20.dp),

                        strokeWidth =
                            2.dp,

                        color =
                            Color.White
                    )


                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )


                    Text(
                        text =
                            "Generating AI Report..."
                    )

                } else {

                    Icon(

                        imageVector =
                            Icons.Outlined.AutoAwesome,

                        contentDescription =
                            null
                    )


                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )


                    Text(

                        text =
                            "Generate AI Report",

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }


            // ====================================================
            // ERROR
            // ====================================================

            if (!error.isNullOrBlank()) {

                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )


                Card(

                    modifier =
                        Modifier.fillMaxWidth(),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color(0xFFFFE5E5)
                        )

                ) {

                    Text(

                        text =
                            error,

                        modifier =
                            Modifier.padding(12.dp),

                        color =
                            TaskDetailRed,

                        fontSize =
                            11.sp
                    )
                }
            }


            // ====================================================
            // DETECTED OBJECTS
            // ====================================================

            if (detectedObjects.isNotBlank()) {

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )


                Text(

                    text =
                        "Detected Objects",

                    fontSize =
                        13.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )


                Card(

                    modifier =
                        Modifier.fillMaxWidth(),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color.White
                        ),

                    shape =
                        RoundedCornerShape(12.dp)

                ) {

                    Text(

                        text =
                            detectedObjects,

                        modifier =
                            Modifier.padding(12.dp),

                        fontSize =
                            11.sp,

                        lineHeight =
                            16.sp
                    )
                }
            }


            // ====================================================
            // GENERATED REPORT
            // ====================================================

            if (!report.isNullOrBlank()) {

                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )


                var isEditingReport by remember { mutableStateOf(false) }

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Description,

                            contentDescription =
                                null,

                            tint =
                                TaskDetailGreen
                        )


                        Spacer(
                            modifier =
                                Modifier.width(7.dp)
                        )


                        Text(
                            text =
                                "Generated AI Report",

                            fontSize =
                                14.sp,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = { isEditingReport = !isEditingReport },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isEditingReport) "Done" else "Edit",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TaskDetailOrange
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                if (isEditingReport) {
                    OutlinedTextField(
                        value = report,
                        onValueChange = onReportChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            fontSize = 11.sp,
                            lineHeight = 17.sp,
                            color = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Card(

                        modifier =
                            Modifier.fillMaxWidth(),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Color.White
                            ),

                        shape =
                            RoundedCornerShape(12.dp)

                    ) {

                        Text(

                            text =
                                report,

                            modifier =
                                Modifier.padding(14.dp),

                            fontSize =
                                11.sp,

                            lineHeight =
                                17.sp
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )


                OutlinedButton(

                    onClick =
                        onDownloadPdf,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)

                ) {

                    Icon(

                        imageVector =
                            Icons.Outlined.PictureAsPdf,

                        contentDescription =
                            null
                    )


                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )


                    Text(

                        text =
                            "Download AI Report PDF",

                        fontWeight =
                            FontWeight.Bold
                    )
                }


                // ====================================================
                // UPLOAD REPORT TO ADMIN
                // ====================================================

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Button(
                    onClick =
                        onUploadReport,

                    enabled =
                        !isUploading &&
                                !reportUploaded,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp),

                    shape =
                        RoundedCornerShape(12.dp),

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                TaskDetailOrange,

                            disabledContainerColor =
                                if (reportUploaded) {
                                    Color(0xFFE1F5E7)
                                } else {
                                    Color(0xFFD8D8D8)
                                },

                            disabledContentColor =
                                if (reportUploaded) {
                                    TaskDetailGreen
                                } else {
                                    Color.Gray
                                }
                        )
                ) {

                    if (isUploading) {

                        CircularProgressIndicator(
                            modifier =
                                Modifier.size(19.dp),

                            strokeWidth =
                                2.dp,

                            color =
                                Color.White
                        )

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        Text(
                            text =
                                "Uploading Report...",

                            fontWeight =
                                FontWeight.Bold
                        )

                    } else {

                        Icon(
                            imageVector =
                                if (reportUploaded) {
                                    Icons.Outlined.CheckCircle
                                } else {
                                    Icons.Outlined.CloudUpload
                                },

                            contentDescription =
                                null
                        )

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        Text(
                            text =
                                if (reportUploaded) {
                                    "Report Uploaded to Admin"
                                } else {
                                    "Upload Report to Admin"
                                },

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }


                // ====================================================
                // MARK TASK AS DONE
                // Available only after report upload succeeds
                // ====================================================

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Button(
                    onClick =
                        onMarkDone,

                    enabled =
                        reportUploaded &&
                                !isCompleting &&
                                !taskCompleted,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp),

                    shape =
                        RoundedCornerShape(12.dp),

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                TaskDetailGreen,

                            disabledContainerColor =
                                if (taskCompleted) {
                                    Color(0xFFE1F5E7)
                                } else {
                                    Color(0xFFE0E0E0)
                                },

                            disabledContentColor =
                                if (taskCompleted) {
                                    TaskDetailGreen
                                } else {
                                    Color.Gray
                                }
                        )
                ) {

                    if (isCompleting) {

                        CircularProgressIndicator(
                            modifier =
                                Modifier.size(19.dp),

                            strokeWidth =
                                2.dp,

                            color =
                                Color.White
                        )

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        Text(
                            text =
                                "Completing Task...",

                            fontWeight =
                                FontWeight.Bold
                        )

                    } else {

                        Icon(
                            imageVector =
                                Icons.Outlined.CheckCircle,

                            contentDescription =
                                null
                        )

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        Text(
                            text =
                                when {
                                    taskCompleted ->
                                        "Task Completed"

                                    !reportUploaded ->
                                        "Upload Report Before Completing"

                                    else ->
                                        "Mark Task as Done"
                                },

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun AiStatusRow(
    good: Boolean,
    text: String
) {

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = if (good) {
                Icons.Outlined.CheckCircle
            } else {
                Icons.Outlined.Info
            },
            contentDescription = null,
            tint = if (good) {
                TaskDetailGreen
            } else {
                TaskDetailOrange
            },
            modifier = Modifier.size(17.dp)
        )

        Spacer(Modifier.width(7.dp))

        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}





// ============================================================
// STATUS BADGE
// ============================================================

@Composable
private fun TaskDetailStatusBadge(
    status: String
) {

    val backgroundColor =
        when (
            status.lowercase()
        ) {

            "completed",
            "done" ->
                Color(0xFFE1F5E7)

            "overdue",
            "urgent",
            "blocked" ->
                Color(0xFFFFE1E1)

            else ->
                Color(0xFFFFE5D8)
        }


    val textColor =
        when (
            status.lowercase()
        ) {

            "completed",
            "done" ->
                TaskDetailGreen

            "overdue",
            "urgent",
            "blocked" ->
                TaskDetailRed

            else ->
                TaskDetailOrange
        }


    Box(

        modifier =
            Modifier
                .background(
                    backgroundColor,
                    RoundedCornerShape(9.dp)
                )
                .padding(
                    horizontal =
                        9.dp,
                    vertical =
                        5.dp
                )

    ) {

        Text(

            text =
                status,

            color =
                textColor,

            fontSize =
                9.sp,

            fontWeight =
                FontWeight.Bold
        )
    }
}