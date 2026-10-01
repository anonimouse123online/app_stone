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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextDecoration
import android.widget.Toast
import com.example.capstonesample.data.api.SubtaskItem
import com.example.capstonesample.data.api.UpdateSubtasksRequest
import com.example.capstonesample.data.model.CreateIssueRequest


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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

import java.io.File


// ============================================================
// COLORS
// ============================================================

private val TaskDetailBackground =
    Color(0xFFF0E1D8)

private val TaskDetailOrange =
    Color(0xFFF15A24)

private val TaskDetailGray =
    Color(0xFF777777)

private val TaskDetailGreen =
    Color(0xFF1B9A41)

private val TaskDetailRed =
    Color(0xFFD32F2F)


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

    // Download state for Gemma model
    var isDownloadingModel by remember { mutableStateOf(false) }
    var downloadProgressPct by remember { mutableStateOf(0) }
    var downloadProgressText by remember { mutableStateOf("") }
    var downloadError by remember { mutableStateOf<String?>(null) }

    fun toggleSubtask(subtaskId: String) {
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

                // New photo = clear old AI result.
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
                                18.sp
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

                onCameraClick = {
                    openCamera()
                },

                onGalleryClick = {
                    galleryLauncher.launch("image/*")
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
                                                imageUri
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

                                    aiError =
                                        response.body()
                                            ?.message
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

                    if (!isCompletingTask) {

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

            Text(

                text =
                    label,

                fontSize =
                    9.sp,

                color =
                    TaskDetailGray
            )


            Text(

                text =
                    value,

                fontSize =
                    12.sp,

                fontWeight =
                    FontWeight.Medium
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
    onToggleSubtask: (String) -> Unit
) {
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
                    tint = TaskDetailOrange
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Subtasks & Steps",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.weight(1f))

                if (totalCount > 0) {
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
                                    color = if (isDone) Color(0xFFF8FAFC) else Color(0xFFFAFAFA),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onToggleSubtask(item.id)
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isDone,
                                onCheckedChange = {
                                    onToggleSubtask(item.id)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = TaskDetailOrange,
                                    uncheckedColor = Color(0xFF94A3B8),
                                    checkmarkColor = Color.White
                                ),
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = item.title.ifBlank { "Untitled Step" },
                                fontSize = 13.sp,
                                fontWeight = if (isDone) FontWeight.Normal else FontWeight.Medium,
                                color = if (isDone) TaskDetailGray else Color(0xFF1E293B),
                                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                modifier = Modifier.weight(1f)
                            )
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
// FIELD EVIDENCE / CAMERA
// ============================================================

@Composable
private fun FieldEvidenceCard(

    capturedPhotoUri: Uri?,

    onCameraClick: () -> Unit,

    onGalleryClick: () -> Unit = {}

) {

    val context =
        LocalContext.current


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
                verticalAlignment =
                    Alignment.CenterVertically
            ) {


                Icon(

                    imageVector =
                        Icons.Outlined.CameraAlt,

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
                            "Field Evidence",

                        fontSize =
                            16.sp,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Text(

                        text =
                            "Capture photo evidence for this task.",

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
            // CAMERA & GALLERY BUTTONS
            // ====================================================

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick =
                        onCameraClick,
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(48.dp),
                    shape =
                        RoundedCornerShape(12.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                TaskDetailOrange
                        )
                ) {
                    Icon(
                        imageVector =
                            Icons.Outlined.CameraAlt,
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
                            if (capturedPhotoUri == null) {
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
                    onClick =
                        onGalleryClick,
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(48.dp),
                    shape =
                        RoundedCornerShape(12.dp),
                    border =
                        BorderStroke(
                            1.dp,
                            TaskDetailOrange
                        ),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor =
                                TaskDetailOrange
                        )
                ) {
                    Icon(
                        imageVector =
                            Icons.Outlined.PhotoLibrary,
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
                            "Gallery",
                        fontSize =
                            13.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }


            // ====================================================
            // SHOW CAPTURED PHOTO
            // ====================================================

            if (capturedPhotoUri != null) {


                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )


                Text(

                    text =
                        "Captured Photo",

                    fontSize =
                        13.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                // ------------------------------------------------
                // LOAD LOCAL PHOTO
                // ------------------------------------------------

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


                // ------------------------------------------------
                // DISPLAY PHOTO
                // ------------------------------------------------

                if (bitmap != null) {

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

                        Image(

                            bitmap =
                                bitmap.asImageBitmap(),

                            contentDescription =
                                "Captured field evidence",

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),

                            contentScale =
                                ContentScale.Crop
                        )
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
                        Modifier.height(10.dp)
                )


                // =================================================
                // SAVED LOCALLY INDICATOR
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
                            Modifier.size(17.dp)
                    )


                    Spacer(
                        modifier =
                            Modifier.width(6.dp)
                    )


                    Text(

                        text =
                            "Photo saved locally on this device.",

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
                    Modifier.height(10.dp)
            )


            Text(

                text =
                    "Photos stay on this phone and are not uploaded to the backend.",

                fontSize =
                    9.sp,

                color =
                    TaskDetailGray
            )
        }
    }
}


// ============================================================
// REPORT CARD
// Manual issue report — separate from AI-generated reports.
// ============================================================

@Composable
private fun ReportTaskCard(
    task: SiteTask,
    onReportClick: () -> Unit
) {
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
                        TaskDetailRed
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


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            // ========================================================
            // OPEN / CLOSE BUTTON
            // ========================================================

            OutlinedButton(
                onClick = {
                    expanded =
                        !expanded

                    validationError =
                        null

                    sentMessage =
                        null
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp),

                shape =
                    RoundedCornerShape(12.dp),

                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor =
                            TaskDetailRed
                    )
            ) {

                Icon(
                    imageVector =
                        if (expanded) {
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
                        if (expanded) {
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
                            !isSending,

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


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
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
                            report,

                        modifier =
                            Modifier.padding(14.dp),

                        fontSize =
                            11.sp,

                        lineHeight =
                            17.sp
                    )
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