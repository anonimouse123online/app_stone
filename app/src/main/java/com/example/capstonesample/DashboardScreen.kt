package com.example.capstonesample

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

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// ============================================================
// UI MODELS
// ============================================================

data class Task(
    val title: String,
    val engineer: String,
    val status: String,
    val subtasks: List<String> = emptyList()
)


data class DashboardProjectUi(
    val name: String,
    val type: String,
    val progress: Int,
    val status: String
)


data class DashboardFieldActivityUi(
    val title: String,
    val description: String,
    val type: DashboardActivityType
)


enum class DashboardActivityType {
    WARNING,
    INFO,
    SUCCESS
}


// ============================================================
// COLORS
// ============================================================

private val DashboardBackground = Color(0xFFF0E1D8)
private val SitePulseOrange = Color(0xFFF15A24)
private val OrangeLight = Color(0xFFFFF0DD)
private val TextGray = Color(0xFF777777)
private val Green = Color(0xFF1B9A41)
private val Blue = Color(0xFF2864E8)


// ============================================================
// DASHBOARD
// ============================================================

@Composable
fun DashboardScreen(

    token: String,

    selectedScreen: String = "dashboard",

    onHomeClick: () -> Unit = {},

    onProjectsClick: () -> Unit = {},

    onChatClick: () -> Unit,

    onTasksClick: () -> Unit = {},

    onProfileClick: () -> Unit

) {

    // ========================================================
    // API DATA
    // ========================================================

    var projects by remember {
        mutableStateOf<List<DashboardProjectUi>>(emptyList())
    }

    var tasks by remember {
        mutableStateOf<List<TaskResponse>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }


    // ========================================================
    // LOAD PROJECTS + TASKS
    // ========================================================

    LaunchedEffect(token) {

        isLoading = true
        errorMessage = null

        try {

            val authorizationToken =
                if (token.startsWith("Bearer ")) {
                    token
                } else {
                    "Bearer $token"
                }


            // =================================================
            // PROJECTS API
            // GET /projects
            // =================================================

            val projectsResponse =
                RetrofitClient.api.getProjects(
                    token = authorizationToken
                )

            if (projectsResponse.isSuccessful) {

                val body =
                    projectsResponse.body()

                if (body?.success == true) {

                    projects =
                        body.data.map { project ->

                            DashboardProjectUi(

                                // Project name may be nullable from API
                                name =
                                    project.name
                                        ?: "Unnamed Project",

                                // Your Project model does not have a "type" field yet
                                type =
                                    "PROJECT",

                                progress =
                                    (project.progress ?: 0)
                                        .coerceIn(0, 100),

                                status =
                                    project.status
                                        ?: "Active"
                            )
                        }

                } else {

                    errorMessage =
                        "Unable to load projects."
                }

            } else {

                errorMessage =
                    when (projectsResponse.code()) {

                        401 ->
                            "Session expired. Please login again."

                        403 ->
                            "You do not have permission to view projects."

                        404 ->
                            "Projects API was not found."

                        500 ->
                            "Server error while loading projects."

                        else ->
                            "Projects error: ${projectsResponse.code()}"
                    }
            }


            // =================================================
            // TASKS API
            // GET /tasks
            // =================================================

            val tasksResponse =
                RetrofitClient.api.getTasks(
                    token = authorizationToken
                )

            if (tasksResponse.isSuccessful) {

                val body =
                    tasksResponse.body()

                if (body?.success == true) {

                    tasks =
                        body.data

                } else {

                    if (errorMessage == null) {
                        errorMessage =
                            "Unable to load tasks."
                    }
                }

            } else {

                if (errorMessage == null) {

                    errorMessage =
                        when (tasksResponse.code()) {

                            401 ->
                                "Session expired. Please login again."

                            403 ->
                                "You do not have permission to view tasks."

                            404 ->
                                "Tasks API was not found."

                            500 ->
                                "Server error while loading tasks."

                            else ->
                                "Tasks error: ${tasksResponse.code()}"
                        }
                }
            }

        } catch (e: Exception) {

            errorMessage =
                e.message
                    ?: "Unable to connect to SitePulse server."

        } finally {

            isLoading = false
        }
    }


    // ========================================================
    // USER
    //
    // Temporary until we connect profile/current-user API.
    // ========================================================
    val userName = "SitePulse User"
    val userRole = "Field Engineer"

    val initials = "SP"


    val currentDate =
        remember {

            SimpleDateFormat(
                "EEEE, MMM dd",
                Locale.getDefault()
            ).format(Date())
        }


    // ========================================================
    // RECENT FIELD ACTIVITY
    //
    // Uses tasks from GET /tasks
    // ========================================================

    val activities =
        tasks
            .take(5)
            .map { task ->

                DashboardFieldActivityUi(

                    title =
                        task.title,

                    description =
                        buildString {

                            append(
                                task.status
                                    ?: "Pending"
                            )

                            task.projectName?.let {

                                append(" • ")
                                append(it)
                            }

                            task.assigneeName?.let {

                                append(" • ")
                                append(it)
                            }
                        },

                    type =
                        when (
                            task.status
                                ?.uppercase()
                        ) {

                            "DELAYED",
                            "OVERDUE",
                            "BLOCKED",
                            "REJECTED" ->

                                DashboardActivityType.WARNING


                            "COMPLETED",
                            "DONE",
                            "APPROVED" ->

                                DashboardActivityType.SUCCESS


                            else ->

                                DashboardActivityType.INFO
                        }
                )
            }


    // ========================================================
    // HEALTH CALCULATION
    // ========================================================

    val totalTasks =
        tasks.size


    val delayedTasks =
        tasks.count { task ->

            task.status.equals(
                "Delayed",
                ignoreCase = true
            ) ||

                    task.status.equals(
                        "Overdue",
                        ignoreCase = true
                    ) ||

                    task.status.equals(
                        "Blocked",
                        ignoreCase = true
                    )
        }


    val completedTasks =
        tasks.count { task ->

            task.status.equals(
                "Completed",
                ignoreCase = true
            ) ||

                    task.status.equals(
                        "Done",
                        ignoreCase = true
                    ) ||

                    task.status.equals(
                        "Approved",
                        ignoreCase = true
                    )
        }


    // ========================================================
    // PROJECT STATUS
    // ========================================================

    val onTrack =
        projects.count { project ->

            project.status.equals(
                "Active",
                ignoreCase = true
            ) ||

                    project.status.equals(
                        "On Track",
                        ignoreCase = true
                    )
        }


    val delayed =
        projects.count { project ->

            project.status.equals(
                "Delayed",
                ignoreCase = true
            )
        }


    // ========================================================
    // HEALTH SCORE
    //
    // TEMPORARY FORMULA:
    // Every delayed / overdue / blocked task reduces health.
    //
    // We can improve this later using:
    // - expected project progress
    // - issues
    // - delayed tasks
    // - overdue tasks
    // - field reports
    // ========================================================

    val health =
        if (totalTasks == 0) {

            100

        } else {

            val delayedPercentage =
                (
                        delayedTasks.toFloat() /
                                totalTasks.toFloat()
                        ) * 100f

            (100 - delayedPercentage.toInt())
                .coerceIn(0, 100)
        }


    // ========================================================
    // SCREEN
    // ========================================================

    Scaffold(

        containerColor =
            DashboardBackground,

        topBar = {

            DashboardTopBar(
                initials = initials
            )
        },

        bottomBar = {

            SitePulseBottomNavigation(

                selectedScreen =
                    selectedScreen,

                onHomeClick =
                    onHomeClick,

                onProjectsClick =
                    onProjectsClick,

                onMessagesClick =
                    onChatClick,

                onTasksClick =
                    onTasksClick,

                onProfileClick =
                    onProfileClick
            )
        }

    ) { padding ->


        LazyColumn(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(
                        DashboardBackground
                    ),

            contentPadding =
                PaddingValues(
                    horizontal = 18.dp,
                    vertical = 14.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(14.dp)

        ) {


            // =================================================
            // LOADING
            // =================================================

            if (isLoading) {

                item {

                    LinearProgressIndicator(

                        modifier =
                            Modifier.fillMaxWidth(),

                        color =
                            SitePulseOrange
                    )
                }
            }


            // =================================================
            // ERROR
            // =================================================

            errorMessage?.let { error ->

                item {

                    Card(

                        modifier =
                            Modifier.fillMaxWidth(),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Color(0xFFFFE7E7)
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
                                    Icons.Outlined.ErrorOutline,

                                contentDescription =
                                    null,

                                tint =
                                    Color.Red
                            )


                            Spacer(
                                modifier =
                                    Modifier.width(8.dp)
                            )


                            Text(

                                text =
                                    error,

                                color =
                                    Color.Red,

                                fontSize =
                                    11.sp
                            )
                        }
                    }
                }
            }


            // =================================================
            // USER
            // =================================================

            item {

                Column {

                    Text(
                        text = "FIELD COMMAND",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray
                    )


                    Spacer(
                        modifier =
                            Modifier.height(2.dp)
                    )


                    Text(

                        text =
                            if (isLoading) {
                                "Loading..."
                            } else {
                                userName
                            },

                        fontSize =
                            24.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            Color.Black
                    )


                    Text(

                        text =
                            "$userRole • $currentDate",

                        fontSize =
                            12.sp,

                        color =
                            TextGray
                    )
                }
            }


            // =================================================
            // ADVISORY
            // =================================================

            item {
                AdvisoryCard()
            }


            // =================================================
            // HEALTH
            // =================================================

            item {

                HealthStatusCard(

                    health =
                        health,

                    onTrack =
                        onTrack,

                    delayed =
                        delayed,

                    delayedTasks =
                        delayedTasks,

                    completedTasks =
                        completedTasks,

                    totalTasks =
                        totalTasks
                )
            }


            // =================================================
            // QUICK ACTIONS
            // =================================================

            item {
                QuickActions()
            }


            // =================================================
            // ACTIVE PROJECT TITLE
            // =================================================

            item {

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically

                ) {

                    Text(
                        text = "Active Projects",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )


                    Spacer(
                        modifier =
                            Modifier.weight(1f)
                    )


                    Text(

                        text =
                            "See details",

                        fontSize =
                            11.sp,

                        color =
                            SitePulseOrange,

                        modifier =
                            Modifier.clickable {
                                onProjectsClick()
                            }
                    )
                }
            }


            // =================================================
            // PROJECTS
            // =================================================

            if (
                projects.isEmpty() &&
                !isLoading
            ) {

                item {

                    EmptyDashboardCard(
                        text =
                            "No projects available."
                    )
                }

            } else {

                item {

                    Row(

                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.spacedBy(
                                10.dp
                            )

                    ) {

                        projects
                            .take(2)
                            .forEach { project ->

                                ProjectCard(

                                    project =
                                        project,

                                    modifier =
                                        Modifier.weight(1f)
                                )
                            }


                        if (projects.size == 1) {

                            Spacer(
                                modifier =
                                    Modifier.weight(1f)
                            )
                        }
                    }
                }
            }


            // =================================================
            // RECENT FIELD ACTIVITY
            // =================================================

            item {

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically

                ) {

                    Text(

                        text =
                            "Recent Field Activity",

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
                            "View tasks",

                        fontSize =
                            11.sp,

                        color =
                            SitePulseOrange,

                        modifier =
                            Modifier.clickable {
                                onTasksClick()
                            }
                    )
                }
            }


            // =================================================
            // TASK ACTIVITIES
            // =================================================

            if (
                activities.isEmpty() &&
                !isLoading
            ) {

                item {

                    EmptyDashboardCard(
                        text =
                            "No recent task activity."
                    )
                }

            } else {

                items(
                    items = activities,
                    key = {
                        "${it.title}-${it.description}"
                    }
                ) { activity ->

                    ActivityRow(
                        activity =
                            activity
                    )
                }
            }


            item {

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )
            }
        }
    }
}


// ============================================================
// INITIALS
// ============================================================

private fun generateInitials(
    name: String
): String {

    val words =
        name
            .trim()
            .split(" ")
            .filter {
                it.isNotBlank()
            }

    if (words.isEmpty()) {
        return "SP"
    }

    if (words.size == 1) {

        return words[0]
            .take(2)
            .uppercase()
    }

    return (
            words.first()
                .first()
                .toString() +

                    words.last()
                        .first()
                        .toString()
            )
        .uppercase()
}


// ============================================================
// TOP BAR
// ============================================================

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
private fun DashboardTopBar(
    initials: String
) {

    TopAppBar(

        title = {

            Text(
                text = "SitePulse",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        },

        actions = {

            IconButton(
                onClick = {
                    // TODO: Connect notifications API later
                }
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Notifications,

                    contentDescription =
                        "Notifications"
                )
            }


            Box(

                modifier =
                    Modifier
                        .padding(end = 12.dp)
                        .size(36.dp)
                        .background(
                            Color(0xFF263238),
                            CircleShape
                        ),

                contentAlignment =
                    Alignment.Center

            ) {

                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
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
// ADVISORY
// ============================================================

@Composable
private fun AdvisoryCard() {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(14.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    OrangeLight
            )

    ) {

        Row(

            modifier =
                Modifier.padding(12.dp),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            Icon(
                imageVector =
                    Icons.Outlined.Warning,
                contentDescription =
                    null,
                tint =
                    SitePulseOrange
            )


            Spacer(
                modifier =
                    Modifier.width(10.dp)
            )


            Column {

                Text(
                    text =
                        "Field Advisory",
                    fontSize =
                        13.sp,
                    fontWeight =
                        FontWeight.Bold
                )


                Text(
                    text =
                        "Monitor assigned tasks, progress and field conditions.",
                    fontSize =
                        10.sp,
                    lineHeight =
                        13.sp,
                    color =
                        TextGray
                )
            }
        }
    }
}


// ============================================================
// HEALTH
// ============================================================

@Composable
private fun HealthStatusCard(

    health: Int,

    onTrack: Int,

    delayed: Int,

    delayedTasks: Int,

    completedTasks: Int,

    totalTasks: Int

) {

    val healthMessage =
        when {

            totalTasks == 0 ->
                "No Task Data Yet"

            health >= 90 ->
                "All Sites Operational"

            health >= 75 ->
                "Minor Attention Required"

            health >= 50 ->
                "Project Attention Required"

            else ->
                "Critical Attention Required"
        }


    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .height(118.dp),

        shape =
            RoundedCornerShape(20.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )

    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            Box(

                modifier =
                    Modifier
                        .size(68.dp)
                        .background(
                            Color(0xFFFFEEE7),
                            CircleShape
                        ),

                contentAlignment =
                    Alignment.Center

            ) {

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "$health%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "HEALTH",
                        fontSize = 8.sp
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.width(16.dp)
            )


            Column {

                Text(
                    text =
                        "AGGREGATED STATUS",
                    fontSize =
                        9.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        TextGray
                )


                Text(

                    text =
                        healthMessage,

                    fontSize =
                        14.sp,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(7.dp)
                )


                Row {

                    StatusBadge(
                        text =
                            "● $onTrack On Track",
                        background =
                            Color(0xFFE1F5E7),
                        textColor =
                            Green
                    )


                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )


                    StatusBadge(
                        text =
                            "● $delayed Delayed",
                        background =
                            Color(0xFFFFEED7),
                        textColor =
                            Color(0xFFFF8A00)
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(5.dp)
                )


                Text(

                    text =
                        "$completedTasks completed • " +
                                "$delayedTasks delayed • " +
                                "$totalTasks total tasks",

                    fontSize =
                        9.sp,

                    color =
                        TextGray
                )
            }
        }
    }
}


// ============================================================
// STATUS BADGE
// ============================================================

@Composable
private fun StatusBadge(

    text: String,

    background: Color,

    textColor: Color

) {

    Box(

        modifier =
            Modifier
                .background(
                    background,
                    RoundedCornerShape(10.dp)
                )
                .padding(
                    horizontal = 7.dp,
                    vertical = 4.dp
                )

    ) {

        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}


// ============================================================
// QUICK ACTIONS
// ============================================================

@Composable
private fun QuickActions() {

    Row(

        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.spacedBy(10.dp)

    ) {

        QuickActionButton(
            title = "Log Time",
            icon = Icons.Outlined.Schedule,
            modifier = Modifier.weight(1f)
        )

        QuickActionButton(
            title = "Capture",
            icon = Icons.Outlined.CameraAlt,
            modifier = Modifier.weight(1f)
        )

        QuickActionButton(
            title = "Report",
            icon = Icons.Outlined.PriorityHigh,
            modifier = Modifier.weight(1f)
        )
    }
}


// ============================================================
// QUICK ACTION BUTTON
// ============================================================

@Composable
private fun QuickActionButton(

    title: String,

    icon: ImageVector,

    modifier: Modifier = Modifier

) {

    Card(

        modifier =
            modifier
                .height(48.dp)
                .clickable {
                    // TODO: Connect action/navigation later
                },

        shape =
            RoundedCornerShape(10.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )

    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 10.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.Center

        ) {

            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(18.dp),
                tint = SitePulseOrange
            )


            Spacer(
                modifier =
                    Modifier.width(7.dp)
            )


            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// ============================================================
// PROJECT CARD
// ============================================================

@Composable
private fun ProjectCard(

    project: DashboardProjectUi,

    modifier: Modifier = Modifier

) {

    val isActive =
        project.status.equals(
            "Active",
            ignoreCase = true
        ) ||

                project.status.equals(
                    "On Track",
                    ignoreCase = true
                )


    Card(

        modifier =
            modifier,

        shape =
            RoundedCornerShape(15.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )

    ) {

        Column(
            modifier =
                Modifier.padding(12.dp)
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = project.type,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGray
                )


                Spacer(
                    modifier =
                        Modifier.weight(1f)
                )


                StatusBadge(
                    text = project.status,

                    background =
                        if (isActive) {
                            Color(0xFFE1F5E7)
                        } else {
                            Color(0xFFFFEED7)
                        },

                    textColor =
                        if (isActive) {
                            Green
                        } else {
                            Color(0xFFFF8A00)
                        }
                )
            }


            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )


            Text(
                text = project.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )


            Text(
                text =
                    "${project.progress}% Complete",
                fontSize = 10.sp,
                color = TextGray
            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            LinearProgressIndicator(

                progress = {
                    project.progress / 100f
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(5.dp),

                color =
                    if (isActive) {
                        SitePulseOrange
                    } else {
                        Color(0xFFFF9800)
                    },

                trackColor =
                    Color(0xFFE9E4E1)
            )
        }
    }
}


// ============================================================
// ACTIVITY ROW
// ============================================================

@Composable
private fun ActivityRow(

    activity:
    DashboardFieldActivityUi

) {

    Row(

        modifier =
            Modifier.fillMaxWidth(),

        verticalAlignment =
            Alignment.Top

    ) {

        Box(

            modifier =
                Modifier
                    .padding(top = 5.dp)
                    .size(9.dp)
                    .background(

                        when (
                            activity.type
                        ) {

                            DashboardActivityType.WARNING ->
                                SitePulseOrange

                            DashboardActivityType.INFO ->
                                Blue

                            DashboardActivityType.SUCCESS ->
                                Green
                        },

                        CircleShape
                    )
        )


        Spacer(
            modifier =
                Modifier.width(12.dp)
        )


        Column {

            Text(
                text = activity.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )


            Text(
                text = activity.description,
                fontSize = 10.sp,
                color = TextGray
            )
        }
    }
}


// ============================================================
// EMPTY CARD
// ============================================================

@Composable
private fun EmptyDashboardCard(
    text: String
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(12.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )

    ) {

        Text(
            text = text,
            modifier =
                Modifier.padding(14.dp),
            color = TextGray,
            fontSize = 11.sp
        )
    }
}


// ============================================================
// BOTTOM NAVIGATION
// ============================================================

@Composable
fun SitePulseBottomNavigation(

    selectedScreen: String,

    onHomeClick: () -> Unit,

    onProjectsClick: () -> Unit,

    onMessagesClick: () -> Unit,

    onTasksClick: () -> Unit,

    onProfileClick: () -> Unit

) {

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 2.dp
    ) {

        SitePulseNavigationItem(
            title = "Home",
            icon = Icons.Outlined.Home,
            selected =
                selectedScreen == "dashboard",
            onClick = onHomeClick
        )


        SitePulseNavigationItem(
            title = "Projects",
            icon = Icons.Outlined.BusinessCenter,
            selected =
                selectedScreen == "projects",
            onClick = onProjectsClick
        )


        SitePulseNavigationItem(
            title = "Messages",
            icon =
                Icons.Outlined.ChatBubbleOutline,
            selected =
                selectedScreen == "chat",
            onClick = onMessagesClick
        )


        SitePulseNavigationItem(
            title = "Tasks",
            icon = Icons.Outlined.TaskAlt,
            selected =
                selectedScreen == "tasks",
            onClick = onTasksClick
        )


        SitePulseNavigationItem(
            title = "Profile",
            icon = Icons.Outlined.Person,
            selected =
                selectedScreen == "profile",
            onClick = onProfileClick
        )
    }
}


// ============================================================
// NAVIGATION ITEM
// ============================================================

@Composable
private fun RowScope.SitePulseNavigationItem(

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
                    title,
                modifier =
                    Modifier.size(21.dp)
            )
        },

        label = {

            Text(
                text = title,
                fontSize = 9.sp
            )
        },

        colors =
            NavigationBarItemDefaults.colors(

                selectedIconColor =
                    SitePulseOrange,

                selectedTextColor =
                    SitePulseOrange,

                indicatorColor =
                    Color(0xFFFFE6DC),

                unselectedIconColor =
                    Color(0xFF777777),

                unselectedTextColor =
                    Color(0xFF777777)
            )
    )
}