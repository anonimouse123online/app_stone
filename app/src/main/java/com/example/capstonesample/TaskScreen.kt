package com.example.capstonesample

import android.util.Base64

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstonesample.ui.theme.*

import com.example.capstonesample.data.api.RetrofitClient
import com.example.capstonesample.data.api.TaskResponse
import com.example.capstonesample.data.api.SubtaskItem

import kotlinx.coroutines.launch

import org.json.JSONObject


data class SiteTask(
    val id: String,
    val title: String,
    val projectId: String?,
    val projectCode: String?,
    val project: String,
    val status: String,
    val schedule: String,
    val assignee: String,
    val phase: String,
    val priority: String,
    val progress: Int,
    val assigneeInitials: String,
    val assigneeColor: Color,
    val indicatorColor: Color,
    val overdue: Boolean = false,
    val subtasks: List<SubtaskItem> = emptyList(),
    val projectLocation: String? = null,
    val materialsRequired: String? = null,
    val siteInstructions: String? = null,
    val allocatedMaterials: List<com.example.capstonesample.data.model.TaskAssignedResource> = emptyList(),
    val resources: List<com.example.capstonesample.data.model.TaskAssignedResource> = emptyList()
)


private val TaskBackground =
    Color(0xFFF6F8FA)

private val TaskOrange =
    Color(0xFFF15A24)

private val TaskGray =
    Color(0xFF64748B)

private val TaskGreen =
    Color(0xFF10B981)

private val TaskRed =
    Color(0xFFEF4444)


// GET USER UUID FROM JWT
private fun getUserIdFromJwt(
    token: String
): String? {

    if (
        token.isBlank() ||
        token.startsWith("LOCAL_")
    ) {

        return null
    }


    return try {

        // Remove "Bearer " if it was accidentally passed here.
        val cleanToken =
            token.removePrefix(
                "Bearer "
            )


        val parts =
            cleanToken.split(".")


        if (
            parts.size < 2
        ) {

            return null
        }


        val decodedBytes =
            Base64.decode(

                parts[1],

                Base64.URL_SAFE or
                        Base64.NO_WRAP or
                        Base64.NO_PADDING
            )


        val json =
            JSONObject(

                String(
                    decodedBytes,
                    Charsets.UTF_8
                )
            )


        json.optString(
            "id",
            null
        )


    } catch (
        e: Exception
    ) {

        e.printStackTrace()

        null
    }
}


@Composable
fun TasksScreen(

    onHomeClick: () -> Unit,

    onProjectsClick: () -> Unit,

    onMessagesClick: () -> Unit,

    onProfileClick: () -> Unit,

    // Retain the callback for MainActivity navigation.
    onTaskClick: (SiteTask) -> Unit = {},

    // REAL JWT
    token: String = "",

    // Optional project UUID filter
    projectId: String? = null

) {

    val scope =
        rememberCoroutineScope()


    //
    // When user clicks a task card, the selected task is stored
    // here and TaskDetailScreen.kt will be shown.
    var selectedTask by remember {

        mutableStateOf<SiteTask?>(
            null
        )
    }


    // CURRENT ENGINEER UUID
    val engineerId =
        remember(token) {

            getUserIdFromJwt(
                token
            )
        }


    var serverTasks by remember {

        mutableStateOf<List<TaskResponse>>(
            emptyList()
        )
    }


    var selectedFilter by remember {

        mutableStateOf(
            "All"
        )
    }


    var isRefreshing by remember {

        mutableStateOf(
            false
        )
    }


    var taskError by remember {

        mutableStateOf<String?>(
            null
        )
    }


    fun refreshTasks() {

        if (
            token.isBlank()
        ) {

            taskError =
                "No login token found. Please sign in again."

            return
        }


        if (
            token.startsWith(
                "LOCAL_"
            )
        ) {

            taskError =
                "You are currently using offline login."

            return
        }


        if (
            engineerId.isNullOrBlank()
        ) {

            taskError =
                "Unable to identify the logged-in engineer."


            println(
                "❌ ENGINEER ID FROM JWT IS NULL"
            )


            return
        }


        scope.launch {

            isRefreshing =
                true

            taskError =
                null


            try {

                println(
                    "======================================"
                )

                println(
                    "🌐 TASK SCREEN REQUEST"
                )

                println(
                    "ENGINEER ID = $engineerId"
                )

                println(
                    "PROJECT ID = $projectId"
                )

                println(
                    "======================================"
                )


                // AUTHORIZATION TOKEN
                val authorizationToken =
                    if (
                        token.startsWith(
                            "Bearer "
                        )
                    ) {

                        token

                    } else {

                        "Bearer $token"
                    }


                // GET /tasks
                val response =
                    RetrofitClient.api
                        .getTasks(

                            token =
                                authorizationToken,

                            assigneeId =
                                engineerId,

                            projectId =
                                projectId
                        )


                println(
                    "TASK HTTP = ${response.code()}"
                )


                if (
                    response.isSuccessful
                ) {

                    val result =
                        response.body()


                    if (
                        result?.success == true
                    ) {

                        serverTasks =
                            result.data


                        println(
                            "✅ ANDROID RECEIVED ${result.data.size} TASK(S)"
                        )


                        result.data.forEach { task ->

                            println(
                                "--------------------------------"
                            )

                            println(
                                "TASK ID = ${task.id}"
                            )

                            println(
                                "TASK TITLE = ${task.title}"
                            )

                            println(
                                "TASK STATUS = ${task.status}"
                            )

                            println(
                                "TASK PROJECT ID = ${task.projectId}"
                            )

                            println(
                                "TASK PROJECT CODE = ${task.projectCode}"
                            )

                            println(
                                "TASK PROJECT = ${task.projectName}"
                            )

                            println(
                                "TASK ASSIGNEE ID = ${task.assigneeId}"
                            )

                            println(
                                "TASK ASSIGNEE = ${task.assigneeName}"
                            )

                            println(
                                "TASK DUE DATE = ${task.dueDate}"
                            )

                            println(
                                "TASK PHASE = ${task.phase}"
                            )

                            println(
                                "TASK PRIORITY = ${task.priority}"
                            )

                            println(
                                "TASK PROGRESS = ${task.progress}"
                            )
                        }


                    } else {

                        serverTasks =
                            emptyList()


                        taskError =
                            "Unable to load task data."
                    }


                } else {

                    val error =
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
                        "❌ TASK REQUEST FAILED"
                    )

                    println(
                        "HTTP = ${response.code()}"
                    )

                    println(
                        "ERROR = $error"
                    )


                    taskError =
                        when (
                            response.code()
                        ) {

                            401 ->

                                "Session expired. Please sign in again."


                            403 ->

                                "You do not have permission to view tasks."


                            404 ->

                                "Tasks API was not found."


                            500 ->

                                "Server error while loading tasks."


                            else ->

                                "Unable to load tasks. Server returned ${response.code()}."
                        }
                }


            } catch (
                e: Exception
            ) {

                e.printStackTrace()


                println(
                    "❌ TASK EXCEPTION = ${e.message}"
                )


                taskError =
                    e.message
                        ?: "Unable to connect to the SitePulse server."


            } finally {

                isRefreshing =
                    false
            }
        }
    }


    LaunchedEffect(
        token,
        engineerId,
        projectId
    ) {

        if (
            token.isNotBlank() &&
            !token.startsWith("LOCAL_") &&
            !engineerId.isNullOrBlank()
        ) {

            refreshTasks()
        }
    }


    // API TASK -> UI TASK
    val tasks =
        serverTasks.map { task ->


            val taskStatus =
                task.status
                    ?: "Pending"


            val indicatorColor =
                when (
                    taskStatus.lowercase()
                ) {

                    "completed",
                    "done",
                    "approved" ->

                        TaskGreen


                    "in progress",
                    "ongoing" ->

                        TaskOrange


                    "urgent",
                    "overdue",
                    "blocked",
                    "rejected" ->

                        TaskRed


                    else ->

                        TaskGray
                }


            val assigneeName =
                task.assigneeName
                    ?: "Me"


            val initials =
                assigneeName
                    .split(" ")
                    .filter {

                        it.isNotBlank()
                    }
                    .take(2)
                    .joinToString("") {

                        it.first()
                            .uppercase()
                    }
                    .ifBlank {

                        "ME"
                    }


            SiteTask(

                id =
                    task.id,

                title =
                    task.title,

                projectId =
                    task.projectId,

                projectCode =
                    task.projectCode,

                project =
                    task.projectName
                        ?: "Assigned Project",

                status =
                    taskStatus,

                schedule =
                    task.dueDate
                        ?: "No due date",

                assignee =
                    assigneeName,

                phase =
                    task.phase
                        ?: "No phase",

                priority =
                    task.priority
                        ?: "Normal",

                progress =
                    if (
                        taskStatus.equals(
                            "Completed",
                            ignoreCase = true
                        ) ||
                        taskStatus.equals(
                            "Done",
                            ignoreCase = true
                        ) ||
                        taskStatus.equals(
                            "Approved",
                            ignoreCase = true
                        )
                    ) {
                        100
                    } else {
                        (task.progress ?: 0)
                            .coerceIn(
                                0,
                                100
                            )
                    },

                assigneeInitials =
                    initials,

                assigneeColor =
                    Color(
                        0xFF263238
                    ),

                indicatorColor =
                    indicatorColor,

                overdue =
                    taskStatus.equals(
                        "overdue",
                        ignoreCase = true
                    ),

                subtasks =
                    task.subtasks ?: emptyList(),

                projectLocation =
                    task.projectLocation,
                materialsRequired = task.materialsRequired,
                siteInstructions = task.siteInstructions,
                allocatedMaterials = task.allocatedMaterials.orEmpty(),
                resources = task.resources.orEmpty()
            )
        }


    val filteredTasks =
        when (
            selectedFilter
        ) {


            "To Do" ->

                tasks.filter {

                    it.status.equals(
                        "Pending",
                        ignoreCase = true
                    ) ||

                            it.status.equals(
                                "Ready",
                                ignoreCase = true
                            ) ||

                            it.status.equals(
                                "To Do",
                                ignoreCase = true
                            )
                }


            "In Progress" ->

                tasks.filter {

                    it.status.equals(
                        "In Progress",
                        ignoreCase = true
                    ) ||

                            it.status.equals(
                                "Ongoing",
                                ignoreCase = true
                            )
                }


            "Done" ->

                tasks.filter {

                    it.status.equals(
                        "Completed",
                        ignoreCase = true
                    ) ||

                            it.status.equals(
                                "Done",
                                ignoreCase = true
                            ) ||

                            it.status.equals(
                                "Approved",
                                ignoreCase = true
                            )
                }


            else ->

                tasks
        }


    //
    // If a task has been selected, show TaskDetailScreen.kt.
    //
    // Otherwise show the regular Tasks list.
    //
    // This is placed AFTER all remember/state calls.
    val openedTask =
        selectedTask


    if (
        openedTask != null
    ) {

        TaskDetailScreen(

            task =
                openedTask,

            onTaskUpdated = { saved ->
                serverTasks = serverTasks.map { existing ->
                    if (existing.id == saved.id) existing.copy(
                        status = saved.status ?: existing.status,
                        progress = saved.progress ?: existing.progress,
                        subtasks = saved.subtasks ?: existing.subtasks
                    ) else existing
                }
                selectedTask = selectedTask?.let { existing ->
                    if (existing.id == saved.id) existing.copy(
                        status = saved.status ?: existing.status,
                        progress = saved.progress ?: existing.progress,
                        subtasks = saved.subtasks ?: existing.subtasks
                    ) else existing
                }
            },


            onBackClick = {

                selectedTask =
                    null

                // Fetch the latest task status/progress from backend
                refreshTasks()
            },


            onCameraClick = {

                /*
                 * TODO: Implement camera capture and attach the photo to its
                 * task and project. Include timestamp, location, and engineer
                 * context with the evidence.
                 *
                 * Keep AI advisory. The engineer reviews and decides before
                 * construction work is approved or evidence is submitted.
                 */
            },


            onReportClick = {

                /*
                 * TODO: Implement issue reporting with title, description,
                 * category, severity, photo and location evidence, timestamp,
                 * engineer, task, and project details.
                 *
                 * AI may suggest report fields, but the engineer reviews and
                 * submits the issue.
                 */
            }
        )


    } else {

        Scaffold(

            containerColor =
                TaskBackground,


            topBar = {

                TasksTopBar(

                    isRefreshing =
                        isRefreshing,

                    onRefresh = {

                        refreshTasks()
                    }
                )
            },


            bottomBar = {

                TasksBottomNavigationBar(

                    selectedScreen =
                        "tasks",

                    onHomeClick =
                        onHomeClick,

                    onProjectsClick =
                        onProjectsClick,

                    onMessagesClick =
                        onMessagesClick,

                    onTasksClick = {

                        // Already on tasks
                    },

                    onProfileClick =
                        onProfileClick
                )
            }


        ) { padding ->


            Column(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            padding
                        )

            ) {


                Column(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                Color.White
                            )
                            .padding(
                                20.dp
                            )

                ) {


                    Row(

                        modifier =
                            Modifier.fillMaxWidth(),

                        verticalAlignment =
                            Alignment.CenterVertically

                    ) {


                        Text(

                            text =
                                "Tasks",

                            fontSize =
                                24.sp,

                            fontWeight =
                                FontWeight.Bold
                        )


                        Spacer(

                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        )


                        if (
                            token.startsWith(
                                "LOCAL_"
                            )
                        ) {

                            Text(

                                text =
                                    "Offline",

                                color =
                                    TaskGray,

                                fontSize =
                                    10.sp
                            )


                        } else if (
                            isRefreshing
                        ) {


                            CircularProgressIndicator(

                                modifier =
                                    Modifier.size(
                                        18.dp
                                    ),

                                strokeWidth =
                                    2.dp,

                                color =
                                    TaskOrange
                            )
                        }
                    }


                    Spacer(

                        modifier =
                            Modifier.height(
                                12.dp
                            )
                    )


                    Row(

                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )

                    ) {


                        listOf(

                            "All",

                            "To Do",

                            "In Progress",

                            "Done"

                        ).forEach { filter ->


                            TaskFilterButton(

                                text =
                                    filter,

                                selected =
                                    selectedFilter ==
                                            filter,

                                onClick = {

                                    selectedFilter =
                                        filter
                                }
                            )
                        }
                    }


                    if (
                        taskError != null
                    ) {

                        Spacer(

                            modifier =
                                Modifier.height(
                                    10.dp
                                )
                        )


                        Text(

                            text =
                                taskError!!,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error,

                            fontSize =
                                11.sp
                        )
                    }
                }


                if (
                    isRefreshing &&
                    serverTasks.isEmpty()
                ) {


                    Box(

                        modifier =
                            Modifier.fillMaxSize(),

                        contentAlignment =
                            Alignment.Center

                    ) {


                        Column(

                            horizontalAlignment =
                                Alignment.CenterHorizontally

                        ) {


                            CircularProgressIndicator(

                                color =
                                    TaskOrange
                            )


                            Spacer(

                                modifier =
                                    Modifier.height(
                                        10.dp
                                    )
                            )


                            Text(

                                text =
                                    "Loading assigned tasks...",

                                fontSize =
                                    11.sp,

                                color =
                                    TaskGray
                            )
                        }
                    }


                } else if (
                    filteredTasks.isEmpty()
                ) {


                    Box(

                        modifier =
                            Modifier.fillMaxSize(),

                        contentAlignment =
                            Alignment.Center

                    ) {


                        Column(

                            horizontalAlignment =
                                Alignment.CenterHorizontally

                        ) {


                            Icon(

                                imageVector =
                                    Icons.Outlined.TaskAlt,

                                contentDescription =
                                    null,

                                modifier =
                                    Modifier.size(
                                        50.dp
                                    ),

                                tint =
                                    TaskGray
                            )


                            Spacer(

                                modifier =
                                    Modifier.height(
                                        10.dp
                                    )
                            )


                            Text(

                                text =
                                    if (
                                        selectedFilter ==
                                        "All"
                                    ) {

                                        "No assigned tasks"

                                    } else {

                                        "No ${selectedFilter.lowercase()} tasks"
                                    },

                                fontWeight =
                                    FontWeight.Bold
                            )


                            Spacer(

                                modifier =
                                    Modifier.height(
                                        5.dp
                                    )
                            )


                            Text(

                                text =
                                    if (
                                        token.startsWith(
                                            "LOCAL_"
                                        )
                                    ) {

                                        "Offline mode."

                                    } else {

                                        "Tasks assigned by the administrator will appear here."
                                    },

                                color =
                                    TaskGray,

                                fontSize =
                                    11.sp
                            )


                            Spacer(

                                modifier =
                                    Modifier.height(
                                        15.dp
                                    )
                            )


                            if (
                                !token.startsWith(
                                    "LOCAL_"
                                )
                            ) {

                                Button(

                                    onClick = {

                                        refreshTasks()
                                    },

                                    colors =
                                        ButtonDefaults
                                            .buttonColors(

                                                containerColor =
                                                    TaskOrange
                                            ),

                                    shape =
                                        RoundedCornerShape(
                                            12.dp
                                        )

                                ) {


                                    Icon(

                                        imageVector =
                                            Icons.Outlined.Refresh,

                                        contentDescription =
                                            null,

                                        modifier =
                                            Modifier.size(
                                                18.dp
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
                                            "Refresh Tasks"
                                    )
                                }
                            }
                        }
                    }


                } else {


                    LazyColumn(

                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(
                                    TaskBackground
                                ),

                        contentPadding =
                            PaddingValues(
                                14.dp
                            ),

                        verticalArrangement =
                            Arrangement.spacedBy(
                                12.dp
                            )

                    ) {


                        items(

                            items =
                                filteredTasks,

                            key = {

                                it.id
                            }

                        ) { task ->


                            TaskListCard(

                                task =
                                    task,

                                onClick = {

                                    selectedTask =
                                        task


                                    /*
                                     * Keep this callback for compatibility
                                     * with your existing MainActivity /
                                     * navigation.
                                     *
                                     * If you do not need it later,
                                     * you can remove onTaskClick completely.
                                     */
                                    onTaskClick(
                                        task
                                    )
                                }
                            )
                        }


                        item {

                            Spacer(

                                modifier =
                                    Modifier.height(
                                        70.dp
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}


@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
private fun TasksTopBar(

    isRefreshing: Boolean,

    onRefresh: () -> Unit

) {

    TopAppBar(

        title = {

            Text(

                text =
                    "SitePulse",

                fontWeight =
                    FontWeight.Bold,

                color =
                    Color.Black
            )
        },


        actions = {


            IconButton(

                enabled =
                    !isRefreshing,

                onClick =
                    onRefresh

            ) {


                Icon(

                    imageVector =
                        Icons.Outlined.Refresh,

                    contentDescription =
                        "Refresh Tasks"
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


@Composable
private fun TaskFilterButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (selected) TaskOrange else Color.White,
                RoundedCornerShape(20.dp)
            )
            .then(
                if (!selected) Modifier.border(1.dp, CardBorderStroke, RoundedCornerShape(20.dp))
                else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else TextSlate700
        )
    }
}


@Composable
private fun TaskListCard(
    task: SiteTask,
    onClick: () -> Unit
) {
    val priorityUpper = task.priority.trim().uppercase()
    val (priorityBg, priorityTextColor) = when {
        priorityUpper.contains("HIGH") || priorityUpper.contains("URGENT") || priorityUpper.contains("CRITICAL") ->
            Pair(RoseRedBg, RoseRedText)
        priorityUpper.contains("MED") ->
            Pair(AmberWarningBg, AmberWarningText)
        else ->
            Pair(EmeraldGreenBg, EmeraldGreenText)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorderStroke),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Top Row: Project Code/Name Pill & Priority Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val projectTag = task.projectCode?.ifBlank { null } ?: task.project.ifBlank { "Project" }
                Box(
                    modifier = Modifier
                        .background(IndigoBlueBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = projectTag.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = IndigoBlueText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (task.phase.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = task.phase,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSlate700,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .background(priorityBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = task.priority.ifBlank { "NORMAL" }.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = priorityTextColor
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Task Title + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(task.indicatorColor, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = task.title,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSlate900,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                TaskStatusBadge(status = task.status)
            }

            Spacer(Modifier.height(12.dp))

            // Assignee & Subtasks Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Assignee Avatar
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(task.assigneeColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = task.assigneeInitials,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = task.assignee.ifBlank { "Unassigned" },
                    fontSize = 12.sp,
                    color = TextSlate700,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (task.subtasks.isNotEmpty()) {
                    Spacer(Modifier.width(10.dp))
                    val doneCount = task.subtasks.count { it.completed }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                            .border(1.dp, CardBorderStroke, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = if (doneCount == task.subtasks.size) EmeraldGreen else TextSlate400,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "$doneCount/${task.subtasks.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSlate700
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progress",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSlate500
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${task.progress}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TaskOrange
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { task.progress.coerceIn(0, 100) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = TaskOrange,
                trackColor = Color(0xFFE2E8F0)
            )

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(Modifier.height(10.dp))

            // Due Date & Chevron Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (task.overdue) Icons.Outlined.Warning else Icons.Outlined.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = if (task.overdue) TaskRed else TextSlate400
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (task.overdue) "Overdue: ${task.schedule}" else "Due: ${task.schedule}",
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    fontWeight = if (task.overdue) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (task.overdue) TaskRed else TextSlate500
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Open Task",
                    modifier = Modifier.size(18.dp),
                    tint = TaskOrange
                )
            }
        }
    }
}


@Composable
private fun TaskDetailRow(

    label: String,

    value: String

) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical =
                        3.dp
                )

    ) {


        Text(

            text =
                "$label: ",

            fontSize =
                11.sp,

            color =
                TaskGray
        )


        Text(

            text =
                value,

            fontSize =
                11.sp,

            fontWeight =
                FontWeight.Medium
        )
    }
}


@Composable
private fun TaskStatusBadge(

    status: String

) {

    val backgroundColor =
        when (
            status.lowercase()
        ) {

            "completed",
            "done",
            "approved" ->

                Color(
                    0xFFE1F5E7
                )


            "overdue",
            "blocked",
            "rejected",
            "urgent" ->

                Color(
                    0xFFFFE1E1
                )


            "in progress",
            "ongoing" ->

                Color(
                    0xFFFFE5D8
                )


            else ->

                Color(
                    0xFFECECEC
                )
        }


    val textColor =
        when (
            status.lowercase()
        ) {

            "completed",
            "done",
            "approved" ->

                TaskGreen


            "overdue",
            "blocked",
            "rejected",
            "urgent" ->

                TaskRed


            "in progress",
            "ongoing" ->

                TaskOrange


            else ->

                TaskGray
        }


    Box(

        modifier =
            Modifier
                .background(

                    backgroundColor,

                    RoundedCornerShape(
                        8.dp
                    )
                )
                .padding(

                    horizontal =
                        8.dp,

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


@Composable
private fun TasksBottomNavigationBar(

    selectedScreen: String,

    onHomeClick: () -> Unit,

    onProjectsClick: () -> Unit,

    onMessagesClick: () -> Unit,

    onTasksClick: () -> Unit,

    onProfileClick: () -> Unit

) {

    NavigationBar(

        containerColor =
            Color.White

    ) {


        TasksNavigationItem(

            title =
                "Home",

            icon =
                Icons.Outlined.Home,

            selected =
                selectedScreen ==
                        "dashboard",

            onClick =
                onHomeClick
        )


        TasksNavigationItem(

            title =
                "Projects",

            icon =
                Icons.Outlined.BusinessCenter,

            selected =
                selectedScreen ==
                        "projects",

            onClick =
                onProjectsClick
        )


        TasksNavigationItem(

            title =
                "Messages",

            icon =
                Icons.Outlined.ChatBubbleOutline,

            selected =
                selectedScreen ==
                        "chat",

            onClick =
                onMessagesClick
        )


        TasksNavigationItem(

            title =
                "Tasks",

            icon =
                Icons.Outlined.TaskAlt,

            selected =
                selectedScreen ==
                        "tasks",

            onClick =
                onTasksClick
        )


        TasksNavigationItem(

            title =
                "Profile",

            icon =
                Icons.Outlined.Person,

            selected =
                selectedScreen ==
                        "profile",

            onClick =
                onProfileClick
        )
    }
}


@Composable
private fun RowScope.TasksNavigationItem(

    title: String,

    icon: ImageVector,

    selected: Boolean,

    onClick: () -> Unit

) {

    NavigationBarItem(

        selected =
            selected,

        onClick =
            onClick,

        icon = {


            Icon(

                imageVector =
                    icon,

                contentDescription =
                    title
            )
        },

        label = {


            Text(

                text =
                    title,

                fontSize =
                    9.sp
            )
        },

        colors =
            NavigationBarItemDefaults
                .colors(

                    selectedIconColor =
                        TaskOrange,

                    selectedTextColor =
                        TaskOrange,

                    indicatorColor =
                        Color(
                            0xFFFFE7DD
                        ),

                    unselectedIconColor =
                        TaskGray,

                    unselectedTextColor =
                        TaskGray
                )
    )
}