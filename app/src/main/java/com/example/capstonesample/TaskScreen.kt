package com.example.capstonesample

import android.util.Base64

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

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

import com.example.capstonesample.data.api.RetrofitClient
import com.example.capstonesample.data.api.TaskResponse

import kotlinx.coroutines.launch

import org.json.JSONObject


// ============================================================
// TASK UI MODEL
// ============================================================

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
    val overdue: Boolean = false
)


// ============================================================
// COLORS
// ============================================================

private val TaskBackground =
    Color(0xFFF0E1D8)

private val TaskOrange =
    Color(0xFFF15A24)

private val TaskGray =
    Color(0xFF777777)

private val TaskGreen =
    Color(0xFF1B9A41)

private val TaskRed =
    Color(0xFFD32F2F)


// ============================================================
// GET USER UUID FROM JWT
// ============================================================

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


// ============================================================
// TASKS SCREEN
// ============================================================

@Composable
fun TasksScreen(

    onHomeClick: () -> Unit,

    onProjectsClick: () -> Unit,

    onMessagesClick: () -> Unit,

    onProfileClick: () -> Unit,

    /*
     * Kept for compatibility with your current navigation.
     *
     * The task details are now opened internally using selectedTask.
     * You can remove this parameter later if nothing else uses it.
     */
    onTaskClick: (SiteTask) -> Unit = {},

    // REAL JWT
    token: String = "",

    // Optional project UUID filter
    projectId: String? = null

) {

    val scope =
        rememberCoroutineScope()


    // ============================================================
    // SELECTED TASK
    //
    // When user clicks a task card, the selected task is stored
    // here and TaskDetailScreen.kt will be shown.
    // ============================================================

    var selectedTask by remember {

        mutableStateOf<SiteTask?>(
            null
        )
    }


    // ============================================================
    // CURRENT ENGINEER UUID
    // ============================================================

    val engineerId =
        remember(token) {

            getUserIdFromJwt(
                token
            )
        }


    // ============================================================
    // SERVER TASKS
    // ============================================================

    var serverTasks by remember {

        mutableStateOf<List<TaskResponse>>(
            emptyList()
        )
    }


    // ============================================================
    // FILTER
    // ============================================================

    var selectedFilter by remember {

        mutableStateOf(
            "All"
        )
    }


    // ============================================================
    // LOADING
    // ============================================================

    var isRefreshing by remember {

        mutableStateOf(
            false
        )
    }


    // ============================================================
    // ERROR
    // ============================================================

    var taskError by remember {

        mutableStateOf<String?>(
            null
        )
    }


    // ============================================================
    // REFRESH TASKS
    // ============================================================

    fun refreshTasks() {

        // ========================================================
        // TOKEN CHECK
        // ========================================================

        if (
            token.isBlank()
        ) {

            taskError =
                "No login token found. Please sign in again."

            return
        }


        // ========================================================
        // OFFLINE CHECK
        // ========================================================

        if (
            token.startsWith(
                "LOCAL_"
            )
        ) {

            taskError =
                "You are currently using offline login."

            return
        }


        // ========================================================
        // ENGINEER CHECK
        // ========================================================

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


        // ========================================================
        // API REQUEST
        // ========================================================

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


                // ====================================================
                // AUTHORIZATION TOKEN
                // ====================================================

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


                // ====================================================
                // GET /tasks
                // ====================================================

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


                // ====================================================
                // SUCCESS
                // ====================================================

                if (
                    response.isSuccessful
                ) {

                    val result =
                        response.body()


                    if (
                        result?.success == true
                    ) {

                        // =================================================
                        // STORE TASKS
                        // =================================================

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

                    // =================================================
                    // SERVER ERROR BODY
                    // =================================================

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


    // ============================================================
    // AUTOMATIC TASK LOAD
    // ============================================================

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


    // ============================================================
    // API TASK -> UI TASK
    // ============================================================

    val tasks =
        serverTasks.map { task ->


            // ========================================================
            // STATUS
            // ========================================================

            val taskStatus =
                task.status
                    ?: "Pending"


            // ========================================================
            // STATUS INDICATOR COLOR
            // ========================================================

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


            // ========================================================
            // ASSIGNEE
            // ========================================================

            val assigneeName =
                task.assigneeName
                    ?: "Me"


            // ========================================================
            // INITIALS
            // ========================================================

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


            // ========================================================
            // SITE TASK
            // ========================================================

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
                    )
            )
        }


    // ============================================================
    // FILTERED TASKS
    // ============================================================

    val filteredTasks =
        when (
            selectedFilter
        ) {


            // ========================================================
            // TO DO
            // ========================================================

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


            // ========================================================
            // IN PROGRESS
            // ========================================================

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


            // ========================================================
            // DONE
            // ========================================================

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


            // ========================================================
            // ALL
            // ========================================================

            else ->

                tasks
        }


    // ============================================================
    // SCREEN SWITCH
    //
    // If a task has been selected, show TaskDetailScreen.kt.
    //
    // Otherwise show the regular Tasks list.
    //
    // This is placed AFTER all remember/state calls.
    // ============================================================

    val openedTask =
        selectedTask


    if (
        openedTask != null
    ) {

        // ========================================================
        // TASK DETAILS
        // ========================================================

        TaskDetailScreen(

            task =
                openedTask,


            // ====================================================
            // BACK
            // ====================================================

            onBackClick = {

                selectedTask =
                    null

                // Fetch the latest task status/progress from backend
                refreshTasks()
            },


            // ====================================================
            // CAMERA
            // ====================================================

            onCameraClick = {

                /*
                 * ===================================================
                 * TODO: CAMERA IMPLEMENTATION
                 * ===================================================
                 *
                 * Recommended Android library:
                 *
                 * CAMERA X
                 *
                 * Future flow:
                 *
                 * Open Camera
                 *      ↓
                 * Capture construction/site photo
                 *      ↓
                 * Store image locally
                 *      ↓
                 * Add:
                 *
                 * - Timestamp
                 * - GPS/location
                 * - Engineer ID
                 * - Task ID
                 * - Project ID
                 *
                 *      ↓
                 *
                 * Upload photo to backend
                 *
                 *      ↓
                 *
                 * Save as Task Evidence
                 *
                 *
                 * openedTask.id
                 *
                 * gives you the TASK UUID.
                 *
                 *
                 * openedTask.projectId
                 *
                 * gives you the PROJECT UUID.
                 *
                 *
                 * ===================================================
                 * FUTURE AI
                 * ===================================================
                 *
                 * Photo
                 *    ↓
                 * AI Vision Model
                 *    ↓
                 * Analyze:
                 *
                 * - PPE detection
                 * - Helmet detection
                 * - Safety vest detection
                 * - Possible hazards
                 * - Visible cracks/damage
                 * - Construction progress
                 * - Missing materials/components
                 * - Worksite condition
                 *
                 *
                 * RECOMMENDED ARCHITECTURE:
                 *
                 * CameraX
                 *    ↓
                 * Captured image
                 *    ↓
                 * Upload to SitePulse backend
                 *    ↓
                 * Vision AI
                 *    ↓
                 * AI Result
                 *    ↓
                 * Engineer reviews result
                 *    ↓
                 * Engineer confirms/rejects
                 *
                 *
                 * IMPORTANT:
                 *
                 * AI should NEVER automatically approve
                 * construction work.
                 *
                 * AI should only provide:
                 *
                 * - observations
                 * - warnings
                 * - suggestions
                 * - confidence scores
                 *
                 * The engineer must make the final decision.
                 */
            },


            // ====================================================
            // REPORT
            // ====================================================

            onReportClick = {

                /*
                 * ===================================================
                 * TODO: REPORT ISSUE SCREEN
                 * ===================================================
                 *
                 * Later create:
                 *
                 * ReportIssueScreen.kt
                 *
                 *
                 * Suggested navigation:
                 *
                 * ReportIssueScreen(
                 *
                 *     taskId =
                 *         openedTask.id,
                 *
                 *     projectId =
                 *         openedTask.projectId
                 * )
                 *
                 *
                 * Suggested report fields:
                 *
                 * - Report title
                 *
                 * - Description
                 *
                 * - Category
                 *
                 * - Severity
                 *
                 * - Photo evidence
                 *
                 * - GPS/location
                 *
                 * - Date
                 *
                 * - Timestamp
                 *
                 * - Engineer
                 *
                 * - Task
                 *
                 * - Project
                 *
                 *
                 * FUTURE AI:
                 *
                 * Engineer captures image
                 *       ↓
                 *
                 * Engineer writes short note
                 *       ↓
                 *
                 * AI analyzes:
                 *
                 * Image + note + task
                 *
                 *       ↓
                 *
                 * AI can suggest:
                 *
                 * Report Title
                 *
                 * Report Description
                 *
                 * Possible Category
                 *
                 * Possible Severity
                 *
                 * Recommended Action
                 *
                 *
                 * Engineer reviews everything
                 * before submitting.
                 */
            }
        )


    } else {

        // ========================================================
        // TASK LIST SCREEN
        // ========================================================

        Scaffold(

            containerColor =
                TaskBackground,


            // ====================================================
            // TOP BAR
            // ====================================================

            topBar = {

                TasksTopBar(

                    isRefreshing =
                        isRefreshing,

                    onRefresh = {

                        refreshTasks()
                    }
                )
            },


            // ====================================================
            // BOTTOM NAVIGATION
            // ====================================================

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


                // ====================================================
                // TASK HEADER
                // ====================================================

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


                        // =================================================
                        // OFFLINE
                        // =================================================

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


                            // =================================================
                            // REFRESHING
                            // =================================================

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


                    // =================================================
                    // FILTER BUTTONS
                    // =================================================

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


                    // =================================================
                    // ERROR MESSAGE
                    // =================================================

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


                // ====================================================
                // INITIAL LOADING
                // ====================================================

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


                    // =================================================
                    // EMPTY STATE
                    // =================================================

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


                            // =================================================
                            // MANUAL REFRESH
                            // =================================================

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


                    // =================================================
                    // TASK LIST
                    // =================================================

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


                        // =================================================
                        // TASK ITEMS
                        // =================================================

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

                                    // =========================================
                                    // OPEN TASK DETAILS
                                    // =========================================

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


// ============================================================
// TOP BAR
// ============================================================

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
                    FontWeight.Bold
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


// ============================================================
// FILTER BUTTON
// ============================================================

@Composable
private fun TaskFilterButton(

    text: String,

    selected: Boolean,

    onClick: () -> Unit

) {

    Box(

        modifier =
            Modifier
                .background(

                    if (
                        selected
                    ) {

                        TaskOrange

                    } else {

                        Color(
                            0xFFE9DED8
                        )
                    },

                    RoundedCornerShape(
                        10.dp
                    )
                )
                .clickable {

                    onClick()
                }
                .padding(

                    horizontal =
                        14.dp,

                    vertical =
                        8.dp
                )

    ) {


        Text(

            text =
                text,

            fontSize =
                11.sp,

            fontWeight =
                if (
                    selected
                ) {

                    FontWeight.Bold

                } else {

                    FontWeight.Normal
                },

            color =
                if (
                    selected
                ) {

                    Color.White

                } else {

                    TaskGray
                }
        )
    }
}


// ============================================================
// TASK CARD
// ============================================================

@Composable
private fun TaskListCard(

    task: SiteTask,

    onClick: () -> Unit

) {

    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {

                    onClick()
                },

        shape =
            RoundedCornerShape(
                16.dp
            ),

        colors =
            CardDefaults
                .cardColors(

                    containerColor =
                        Color.White
                )

    ) {


        Column(

            modifier =
                Modifier.padding(
                    16.dp
                )

        ) {


            // ====================================================
            // TITLE + STATUS
            // ====================================================

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically

            ) {


                // =================================================
                // STATUS DOT
                // =================================================

                Box(

                    modifier =
                        Modifier
                            .size(
                                8.dp
                            )
                            .background(

                                task.indicatorColor,

                                CircleShape
                            )
                )


                Spacer(

                    modifier =
                        Modifier.width(
                            8.dp
                        )
                )


                // =================================================
                // TASK TITLE
                // =================================================

                Text(

                    text =
                        task.title,

                    modifier =
                        Modifier.weight(
                            1f
                        ),

                    fontSize =
                        16.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                // =================================================
                // STATUS
                // =================================================

                TaskStatusBadge(

                    status =
                        task.status
                )
            }


            Spacer(

                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            // ====================================================
            // PROJECT
            // ====================================================

            TaskDetailRow(

                label =
                    "Project",

                value =
                    task.project
            )


            // ====================================================
            // PHASE
            // ====================================================

            TaskDetailRow(

                label =
                    "Phase",

                value =
                    task.phase
            )


            // ====================================================
            // PRIORITY
            // ====================================================

            TaskDetailRow(

                label =
                    "Priority",

                value =
                    task.priority
            )


            // ====================================================
            // ASSIGNEE
            // ====================================================

            TaskDetailRow(

                label =
                    "Assigned to",

                value =
                    task.assignee
            )


            Spacer(

                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            // ====================================================
            // PROGRESS HEADER
            // ====================================================

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically

            ) {


                Text(

                    text =
                        "Progress",

                    fontSize =
                        10.sp,

                    color =
                        TaskGray
                )


                Spacer(

                    modifier =
                        Modifier.weight(
                            1f
                        )
                )


                Text(

                    text =
                        "${task.progress}%",

                    fontSize =
                        10.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        TaskOrange
                )
            }


            Spacer(

                modifier =
                    Modifier.height(
                        6.dp
                    )
            )


            // ====================================================
            // PROGRESS BAR
            // ====================================================

            LinearProgressIndicator(

                progress = {

                    task.progress
                        .coerceIn(
                            0,
                            100
                        ) / 100f
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            6.dp
                        ),

                color =
                    TaskOrange,

                trackColor =
                    Color(
                        0xFFEAE4E1
                    )
            )


            Spacer(

                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            HorizontalDivider()


            Spacer(

                modifier =
                    Modifier.height(
                        10.dp
                    )
            )


            // ====================================================
            // DUE DATE
            // ====================================================

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically

            ) {


                Icon(

                    imageVector =
                        if (
                            task.overdue
                        ) {

                            Icons.Outlined.Warning

                        } else {

                            Icons.Outlined.Schedule
                        },

                    contentDescription =
                        null,

                    modifier =
                        Modifier.size(
                            16.dp
                        ),

                    tint =
                        if (
                            task.overdue
                        ) {

                            TaskRed

                        } else {

                            TaskGray
                        }
                )


                Spacer(

                    modifier =
                        Modifier.width(
                            6.dp
                        )
                )


                Text(

                    text =
                        if (
                            task.overdue
                        ) {

                            "Overdue: ${task.schedule}"

                        } else {

                            "Due: ${task.schedule}"
                        },

                    modifier =
                        Modifier.weight(
                            1f
                        ),

                    fontSize =
                        10.sp,

                    color =
                        if (
                            task.overdue
                        ) {

                            TaskRed

                        } else {

                            TaskGray
                        }
                )


                // =================================================
                // OPEN ARROW
                // =================================================

                Icon(

                    imageVector =
                        Icons.Outlined.ChevronRight,

                    contentDescription =
                        "Open Task",

                    modifier =
                        Modifier.size(
                            18.dp
                        ),

                    tint =
                        TaskOrange
                )
            }
        }
    }
}


// ============================================================
// TASK DETAIL ROW
// ============================================================

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


// ============================================================
// STATUS BADGE
// ============================================================

@Composable
private fun TaskStatusBadge(

    status: String

) {

    // ========================================================
    // STATUS COLORS
    // ========================================================

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


// ============================================================
// BOTTOM NAVIGATION
// ============================================================

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


        // ========================================================
        // HOME
        // ========================================================

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


        // ========================================================
        // PROJECTS
        // ========================================================

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


        // ========================================================
        // MESSAGES
        // ========================================================

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


        // ========================================================
        // TASKS
        // ========================================================

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


        // ========================================================
        // PROFILE
        // ========================================================

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


// ============================================================
// NAVIGATION ITEM
// ============================================================

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