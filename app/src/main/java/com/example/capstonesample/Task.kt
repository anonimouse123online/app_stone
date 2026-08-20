package com.example.capstonesample

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

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


    // ============================================================
    // PREPARE BUNDLED GEMMA MODEL
    // ============================================================

    LaunchedEffect(Unit) {

        if (aiModelInstalled) {

            isPreparingAi =
                false

            return@LaunchedEffect
        }


        isPreparingAi =
            true

        aiError =
            null


        val ready =
            withContext(
                Dispatchers.IO
            ) {

                modelStatusChecker
                    .ensureGemmaInstalled()
            }


        aiModelInstalled =
            ready

        isPreparingAi =
            false


        if (!ready) {

            aiError =
                "Unable to prepare the bundled Gemma AI model."
        }
    }


    // ============================================================
    // CAMERA STATE
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
    // CAMERA RESULT
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


    // ============================================================
    // OPEN CAMERA FUNCTION
    // ============================================================

    fun openCamera() {

        try {

            // ----------------------------------------------------
            // CREATE LOCAL SITEPULSE PICTURES FOLDER
            // ----------------------------------------------------

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


            // ----------------------------------------------------
            // CREATE IMAGE FILE
            // ----------------------------------------------------

            val imageFile =
                File(
                    picturesDirectory,
                    "evidence_${System.currentTimeMillis()}.jpg"
                )


            // ----------------------------------------------------
            // CREATE SECURE CONTENT URI
            // ----------------------------------------------------

            val photoUri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    imageFile
                )


            pendingPhotoUri =
                photoUri


            // ----------------------------------------------------
            // OPEN PHONE CAMERA
            // ----------------------------------------------------

            cameraLauncher.launch(
                photoUri
            )


            // Optional callback from your existing code.
            onCameraClick()


        } catch (e: Exception) {

            e.printStackTrace()
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
                    task
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
            // PROGRESS
            // ====================================================

            TaskProgressCard(
                task =
                    task
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
                onReportClick =
                    onReportClick
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

                isGenerating =
                    isGeneratingAiReport,

                detectedObjects =
                    detectedObjects,

                report =
                    aiReport,

                error =
                    aiError,

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
                }
            )


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
                        task.status
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
// PROGRESS CARD
// ============================================================

@Composable
private fun TaskProgressCard(
    task: SiteTask
) {

    val safeProgress =
        task.progress
            .coerceIn(
                0,
                100
            )


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

    onCameraClick: () -> Unit

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
            // CAMERA BUTTON
            // ====================================================

            Button(

                onClick =
                    onCameraClick,

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


                Icon(

                    imageVector =
                        Icons.Outlined.CameraAlt,

                    contentDescription =
                        null
                )


                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )


                Text(

                    text =
                        if (capturedPhotoUri == null) {

                            "Open Camera"

                        } else {

                            "Take Another Photo"
                        },

                    fontWeight =
                        FontWeight.Bold
                )
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
// ============================================================

@Composable
private fun ReportTaskCard(

    onReportClick: () -> Unit

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


                Column {

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
                            "Report problems, delays, defects or safety concerns.",

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


            OutlinedButton(

                onClick =
                    onReportClick,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp),

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
                        Icons.Outlined.ReportProblem,

                    contentDescription =
                        null
                )


                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )


                Text(

                    text =
                        "Create Report",

                    fontWeight =
                        FontWeight.Bold
                )
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

    isGenerating: Boolean,

    detectedObjects: String,

    report: String?,

    error: String?,

    onGenerateReport: () -> Unit,

    onDownloadPdf: () -> Unit

) {

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

                        isPreparing ->
                            "Preparing bundled Gemma AI model..."

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
            // PREPARING GEMMA
            // ====================================================

            if (isPreparing) {

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