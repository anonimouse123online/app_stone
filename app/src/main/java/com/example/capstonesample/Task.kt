package com.example.capstonesample

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

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


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

    onCameraClick: () -> Unit,

    onReportClick: () -> Unit

) {

    Scaffold(

        containerColor =
            TaskDetailBackground,

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text = "Task Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Text(
                            text = task.project,
                            fontSize = 10.sp,
                            color = TaskDetailGray
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
                task = task
            )


            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )


            // ====================================================
            // TASK INFORMATION
            // ====================================================

            TaskInformationCard(
                task = task
            )


            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )


            // ====================================================
            // PROGRESS
            // ====================================================

            TaskProgressCard(
                task = task
            )


            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )


            // ====================================================
            // FIELD EVIDENCE
            // ====================================================

            FieldEvidenceCard(
                onCameraClick =
                    onCameraClick
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

            FutureAiCard()


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
                    vertical = 7.dp
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

    onCameraClick: () -> Unit

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
                        "Open Camera",

                    fontWeight =
                        FontWeight.Bold
                )
            }


            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )


            Text(

                text =
                    "Camera upload will be connected with CameraX and the evidence API later.",

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
private fun FutureAiCard() {

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

        Row(

            modifier =
                Modifier.padding(18.dp),

            verticalAlignment =
                Alignment.Top

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
                    Modifier.width(12.dp)
            )


            Column {

                Text(

                    text =
                        "AI Field Analysis",

                    fontSize =
                        15.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )


                Text(

                    text =
                        "AI analysis will be available here later. " +
                                "Captured photos can be analyzed for possible " +
                                "site issues, visible defects, safety concerns " +
                                "or progress observations.",

                    fontSize =
                        10.sp,

                    lineHeight =
                        14.sp,

                    color =
                        TaskDetailGray
                )


                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )


                AssistChip(

                    onClick = {
                        // TODO: Connect AI analysis later
                    },

                    enabled =
                        false,

                    label = {
                        Text(
                            "AI Analysis Coming Soon"
                        )
                    },

                    leadingIcon = {

                        Icon(

                            imageVector =
                                Icons.Outlined.AutoAwesome,

                            contentDescription =
                                null,

                            modifier =
                                Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
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
                    horizontal = 9.dp,
                    vertical = 5.dp
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