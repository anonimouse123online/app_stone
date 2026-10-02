package com.example.capstonesample

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Refresh
import java.util.Locale
import android.util.Base64
import org.json.JSONObject
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstonesample.ui.theme.*

import com.example.capstonesample.data.api.RetrofitClient

import kotlinx.coroutines.launch


// ============================================================
// COLORS
// ============================================================

private val ProfileBackground =
    Color(0xFFF6F8FA)

private val ProfileOrange =
    Color(0xFFF15A24)

private val ProfileGray =
    Color(0xFF64748B)

private val ProfileDividerColor =
    Color(0xFFE2E8F0)


// ============================================================
// PROFILE MODEL
// ============================================================

data class ProfileUiModel(

    val fullName: String = "",

    val email: String = "",

    val role: String = "",

    val completedCount: Int = 0,

    val loggedHours: String = "0h"
)


// ============================================================
// PROFILE SCREEN
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(

    profile: ProfileUiModel,

    // Logged-in JWT
    token: String,

    onHomeClick: () -> Unit = {},

    onProjectsClick: () -> Unit = {},

    onMessagesClick: () -> Unit = {},

    onTasksClick: () -> Unit = {},

    onProjectClick: (SiteProject) -> Unit = {},

    onLogoutClick: () -> Unit = {}

) {

    BackHandler {
        onHomeClick()
    }

    val scope =
        rememberCoroutineScope()

    val scrollState =
        rememberScrollState()


    // ============================================================
    // PROFILE INFO
    // ============================================================

    val displayName =
        profile.fullName.ifBlank {
            "User"
        }

    val displayRole =
        profile.role.ifBlank {
            "User"
        }

    val initials =
        getProfileInitials(
            displayName
        )


    // ============================================================
    // USER PROJECTS
    // ============================================================

    var userProjects by remember {

        mutableStateOf<List<SiteProject>>(
            emptyList()
        )
    }

    var isLoadingProjects by remember {

        mutableStateOf(false)
    }

    var projectError by remember {

        mutableStateOf<String?>(null)
    }


    // ============================================================
    // LOAD USER PROJECTS
    //
    // SAME API USED BY ProjectsScreen
    // GET joined projects for logged-in user
    // ============================================================

    fun loadUserProjects() {

        if (
            token.isBlank() ||
            token.startsWith("LOCAL_")
        ) {

            println(
                "Profile projects: offline/local session."
            )

            return
        }


        scope.launch {

            isLoadingProjects =
                true

            projectError =
                null


            try {

                println(
                    "=================================="
                )

                println(
                    "👤 PROFILE: LOADING USER PROJECTS"
                )

                println(
                    "=================================="
                )


                val response =
                    RetrofitClient.api
                        .getJoinedProjects(

                            token =
                                "Bearer $token"
                        )


                println(
                    "PROFILE PROJECT HTTP = ${response.code()}"
                )


                if (
                    response.isSuccessful
                ) {

                    val result =
                        response.body()


                    if (
                        result?.success == true
                    ) {

                        userProjects =
                            result.data.map { project ->

                                SiteProject(

                                    id =
                                        project.id
                                            ?: "",

                                    code =
                                        project.code
                                            ?: "",

                                    name =
                                        project.name
                                            ?: "Unnamed Project",

                                    location =
                                        project.location
                                            ?: "No location",

                                    scope =
                                        project.scope
                                            ?: "No scope",

                                    client =
                                        project.client
                                            ?: "No client",

                                    budget =
                                        project.budget
                                            ?: "0",

                                    phase =
                                        project.phase
                                            ?: "No phase",

                                    status =
                                        project.status
                                            ?: "Planning",

                                    progress =
                                        project.progress
                                            ?.toInt()
                                            ?: 0,

                                    startDate =
                                        project.startDate
                                            ?: "No start date",

                                    dueDate =
                                        project.dueDate
                                            ?: "No due date",

                                    manager =
                                        project.manager
                                            ?: "Not assigned"
                                )
                            }


                        println(
                            "✅ PROFILE PROJECTS = ${userProjects.size}"
                        )

                    } else {

                        projectError =
                            "Unable to load projects."
                    }


                } else {

                    projectError =
                        "Unable to load projects (${response.code()})."
                }


            } catch (
                e: Exception
            ) {

                e.printStackTrace()

                projectError =
                    e.message
                        ?: "Unable to load projects."

            } finally {

                isLoadingProjects =
                    false
            }
        }
    }


    // ============================================================
    // USER STATS: COMPLETED TASKS & LOGGED HOURS
    // ============================================================

    var completedCount by remember {
        mutableIntStateOf(profile.completedCount)
    }

    var loggedHoursText by remember {
        mutableStateOf(profile.loggedHours)
    }

    var isLoadingStats by remember {
        mutableStateOf(false)
    }

    fun extractUserIdFromToken(rawToken: String): String? {
        try {
            val parts = rawToken.removePrefix("Bearer ").trim().split(".")
            if (parts.size >= 2) {
                val decoded = String(
                    Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING),
                    Charsets.UTF_8
                )
                val json = JSONObject(decoded)
                val id = json.optString("id").ifBlank { json.optString("userId") }
                return id.ifBlank { null }
            }
        } catch (_: Exception) {}
        return null
    }

    fun loadUserStats() {
        if (token.isBlank() || token.startsWith("LOCAL_")) {
            return
        }

        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val currentUserId = extractUserIdFromToken(token)

        scope.launch {
            isLoadingStats = true
            try {
                // 1. Fetch timelogs to calculate total logged hours
                val timelogResponse = RetrofitClient.api.getTimelogs(
                    token = authHeader,
                    engineer = displayName
                )

                var logs = timelogResponse.body()?.data.orEmpty()
                if (logs.isEmpty()) {
                    // Fallback: query all logs and match by engineer name
                    val allLogsResponse = RetrofitClient.api.getTimelogs(token = authHeader)
                    val allLogs = allLogsResponse.body()?.data.orEmpty()
                    val matchingLogs = allLogs.filter { log ->
                        log.engineerName.orEmpty().trim().equals(displayName.trim(), ignoreCase = true)
                    }
                    logs = if (matchingLogs.isNotEmpty()) matchingLogs else allLogs
                } else {
                    val matchingLogs = logs.filter { log ->
                        log.engineerName.orEmpty().trim().equals(displayName.trim(), ignoreCase = true)
                    }
                    if (matchingLogs.isNotEmpty()) {
                        logs = matchingLogs
                    }
                }

                val totalHours = logs.sumOf { log ->
                    val raw = log.totalWorkHours ?: ""
                    val cleaned = raw.replace(Regex("[^0-9.]"), "")
                    cleaned.toDoubleOrNull() ?: 0.0
                }

                loggedHoursText = if (totalHours <= 0.0) {
                    "0h"
                } else if (totalHours % 1.0 == 0.0) {
                    "${totalHours.toInt()}h"
                } else {
                    "${String.format(Locale.US, "%.1f", totalHours)}h"
                }

                // 2. Fetch tasks to calculate completed tasks count
                val tasksResponse = RetrofitClient.api.getTasks(token = authHeader)
                val taskList = tasksResponse.body()?.data.orEmpty()

                val completedTasksAssigned = taskList.count { task ->
                    val status = task.status.orEmpty().trim().lowercase(Locale.ROOT)
                    val isDone = status == "completed" || status == "done" || status == "approved"
                    val isAssignedToUser = (!currentUserId.isNullOrBlank() && task.assigneeId.equals(currentUserId, ignoreCase = true)) ||
                            (!displayName.isBlank() && task.assigneeName.orEmpty().trim().equals(displayName.trim(), ignoreCase = true))
                    isDone && isAssignedToUser
                }

                completedCount = if (completedTasksAssigned > 0) {
                    completedTasksAssigned
                } else {
                    taskList.count { task ->
                        val status = task.status.orEmpty().trim().lowercase(Locale.ROOT)
                        status == "completed" || status == "done" || status == "approved"
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingStats = false
            }
        }
    }


    // ============================================================
    // LOAD PROJECTS & STATS WHEN PROFILE OPENS
    // ============================================================

    LaunchedEffect(
        token
    ) {
        loadUserProjects()
        loadUserStats()
    }


    // ============================================================
    // SCREEN
    // ============================================================

    Scaffold(

        containerColor =
            ProfileBackground,

        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onHomeClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            loadUserProjects()
                            loadUserStats()
                        },
                        enabled = !isLoadingStats && !isLoadingProjects
                    ) {
                        if (isLoadingStats || isLoadingProjects) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = ProfileOrange
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Refresh Profile",
                                tint = Color.Black
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ProfileBackground
                )
            )
        },

        bottomBar = {

            ProfileBottomNavigationBar(

                onHomeClick =
                    onHomeClick,

                onProjectsClick =
                    onProjectsClick,

                onMessagesClick =
                    onMessagesClick,

                onTasksClick =
                    onTasksClick,

                onProfileClick = {
                    // Already here
                }
            )
        }

    ) { padding ->


        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(
                    padding
                )
                .background(
                    ProfileBackground
                )
                .verticalScroll(
                    scrollState
                )
                .padding(
                    horizontal = 14.dp
                )

        ) {


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            // ====================================================
            // PROFILE CARD
            // ====================================================

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(
                        18.dp
                    ),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color.White
                    ),

                border = BorderStroke(1.dp, CardBorderStroke),

                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)

            ) {

                Column(

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            18.dp
                        ),

                    horizontalAlignment =
                        Alignment.CenterHorizontally

                ) {


                    // =================================================
                    // AVATAR
                    // =================================================

                    Box(

                        modifier = Modifier
                            .size(
                                64.dp
                            )
                            .background(

                                Color(
                                    0xFF263238
                                ),

                                CircleShape
                            ),

                        contentAlignment =
                            Alignment.Center

                    ) {

                        Text(

                            text =
                                initials,

                            color =
                                Color.White,

                            fontSize =
                                22.sp,

                            fontWeight =
                                FontWeight.Medium
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )


                    // =================================================
                    // NAME
                    // =================================================

                    Text(

                        text =
                            displayName,

                        fontSize =
                            18.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            Color.Black
                    )


                    // =================================================
                    // EMAIL
                    // =================================================

                    if (
                        profile.email.isNotBlank()
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    2.dp
                                )
                        )

                        Text(

                            text =
                                profile.email,

                            fontSize =
                                10.sp,

                            color =
                                ProfileGray
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(
                                3.dp
                            )
                    )


                    // =================================================
                    // ROLE
                    // =================================================

                    Text(

                        text =
                            displayRole,

                        fontSize =
                            11.sp,

                        color =
                            ProfileGray
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                14.dp
                            )
                    )


                    HorizontalDivider(

                        color =
                            ProfileDividerColor
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                14.dp
                            )
                    )


                    // =================================================
                    // PROFILE STATS
                    // =================================================

                    Row(

                        modifier =
                            Modifier.fillMaxWidth()

                    ) {


                        // =================================================
                        // ACTUAL PROJECT COUNT
                        // =================================================

                        ProfileStat(

                            modifier =
                                Modifier.weight(
                                    1f
                                ),

                            value =
                                userProjects
                                    .size
                                    .toString(),

                            label =
                                "Projects",

                            valueColor =
                                ProfileOrange
                        )


                        ProfileStat(
                            modifier =
                                Modifier.weight(
                                    1f
                                ),
                            value =
                                completedCount.toString(),
                            label =
                                "Completed"
                        )


                        ProfileStat(
                            modifier =
                                Modifier.weight(
                                    1f
                                ),
                            value =
                                loggedHoursText,
                            label =
                                "Logged"
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )





            // ====================================================
            // SETTINGS
            // ====================================================

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(
                        18.dp
                    ),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color.White
                    ),

                border = BorderStroke(1.dp, CardBorderStroke),

                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)

            ) {

                Column {


                    ProfileSettingRow(

                        icon =
                            Icons.Outlined.Person,

                        title =
                            "Account Settings",

                        onClick = {}
                    )


                    ProfileDivider()


                    ProfileSettingRow(

                        icon =
                            Icons.Outlined.Notifications,

                        title =
                            "Notifications",

                        onClick = {}
                    )


                    ProfileDivider()


                    ProfileSettingRow(

                        icon =
                            Icons.Outlined.Visibility,

                        title =
                            "Appearance",

                        onClick = {}
                    )


                    ProfileDivider()


                    ProfileSettingRow(

                        icon =
                            Icons.Outlined.Storage,

                        title =
                            "Data and Storage",

                        onClick = {}
                    )


                    ProfileDivider()


                    ProfileSettingRow(

                        icon =
                            Icons.Outlined.Security,

                        title =
                            "Security",

                        onClick = {}
                    )


                    ProfileDivider()


                    ProfileSettingRow(

                        icon =
                            Icons.Outlined.HelpOutline,

                        title =
                            "Help and Support",

                        onClick = {}
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )


            // ====================================================
            // LOGOUT
            // ====================================================

            OutlinedButton(

                onClick =
                    onLogoutClick,

                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        48.dp
                    ),

                shape =
                    RoundedCornerShape(
                        14.dp
                    ),

                colors =
                    ButtonDefaults
                        .outlinedButtonColors(

                            contentColor =
                                Color.Red
                        ),

                border =
                    BorderStroke(
                        1.dp,
                        Color.Red
                    )

            ) {

                Icon(

                    imageVector =
                        Icons.Default.Logout,

                    contentDescription =
                        "Log Out",

                    modifier =
                        Modifier.size(
                            18.dp
                        )
                )


                Spacer(

                    modifier =
                        Modifier.width(
                            8.dp
                        )
                )


                Text(

                    text =
                        "Log Out",

                    fontSize =
                        13.sp,

                    fontWeight =
                        FontWeight.Medium
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        30.dp
                    )
            )
        }
    }
}


// ============================================================
// PROFILE PROJECT CARD
// ============================================================

@Composable
private fun ProfileProjectCard(

    project: SiteProject,

    onClick: () -> Unit

) {

    Card(

        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },

        shape =
            RoundedCornerShape(
                16.dp
            ),

        colors =
            CardDefaults.cardColors(
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

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                Box(

                    modifier = Modifier
                        .size(
                            42.dp
                        )
                        .background(

                            Color(
                                0xFFFFE7DD
                            ),

                            RoundedCornerShape(
                                11.dp
                            )
                        ),

                    contentAlignment =
                        Alignment.Center

                ) {

                    Icon(

                        imageVector =
                            Icons
                                .Outlined
                                .BusinessCenter,

                        contentDescription =
                            null,

                        tint =
                            ProfileOrange
                    )
                }


                Spacer(

                    modifier =
                        Modifier.width(
                            12.dp
                        )
                )


                Column(

                    modifier =
                        Modifier.weight(
                            1f
                        )

                ) {

                    Text(

                        text =
                            project.name,

                        fontSize =
                            15.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            Color.Black,

                        maxLines =
                            1,

                        overflow =
                            TextOverflow.Ellipsis
                    )


                    Text(

                        text =
                            project.code,

                        fontSize =
                            10.sp,

                        color =
                            ProfileGray
                    )
                }


                Icon(

                    imageVector =
                        Icons
                            .Outlined
                            .ChevronRight,

                    contentDescription =
                        "Open project",

                    tint =
                        ProfileGray
                )
            }


            Spacer(

                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            Row(

                modifier =
                    Modifier.fillMaxWidth()

            ) {

                Text(

                    text =
                        project.status,

                    fontSize =
                        10.sp,

                    color =
                        ProfileGray
                )


                Spacer(

                    modifier =
                        Modifier.weight(
                            1f
                        )
                )


                Text(

                    text =
                        "${project.progress}%",

                    fontSize =
                        11.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        ProfileOrange
                )
            }


            Spacer(

                modifier =
                    Modifier.height(
                        6.dp
                    )
            )


            LinearProgressIndicator(

                progress = {

                    project.progress
                        .coerceIn(
                            0,
                            100
                        ) / 100f
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        5.dp
                    ),

                color =
                    ProfileOrange,

                trackColor =
                    Color(
                        0xFFEAE4E1
                    )
            )


            if (
                project.location.isNotBlank()
            ) {

                Spacer(

                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )


                Text(

                    text =
                        project.location,

                    fontSize =
                        10.sp,

                    color =
                        ProfileGray,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )
            }
        }
    }
}


// ============================================================
// INITIALS
// ============================================================

private fun getProfileInitials(
    fullName: String
): String {

    if (
        fullName.isBlank()
    ) {

        return "U"
    }


    return fullName
        .trim()
        .split(" ")
        .filter {
            it.isNotBlank()
        }
        .take(2)
        .mapNotNull {

            it.firstOrNull()
                ?.uppercase()
        }
        .joinToString("")
        .ifBlank {
            "U"
        }
}


// ============================================================
// PROFILE STAT
// ============================================================

@Composable
private fun ProfileStat(

    modifier: Modifier =
        Modifier,

    value: String,

    label: String,

    valueColor: Color =
        Color.Black

) {

    Column(

        modifier =
            modifier,

        horizontalAlignment =
            Alignment.CenterHorizontally

    ) {

        Text(

            text =
                value,

            fontSize =
                16.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                valueColor
        )


        Spacer(
            modifier =
                Modifier.height(
                    2.dp
                )
        )


        Text(

            text =
                label,

            fontSize =
                9.sp,

            color =
                ProfileGray
        )
    }
}


// ============================================================
// SETTINGS ROW
// ============================================================

@Composable
private fun ProfileSettingRow(

    icon: ImageVector,

    title: String,

    onClick: () -> Unit

) {

    Row(

        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
            .padding(
                horizontal = 14.dp,
                vertical = 13.dp
            ),

        verticalAlignment =
            Alignment.CenterVertically

    ) {

        Icon(

            imageVector =
                icon,

            contentDescription =
                title,

            modifier =
                Modifier.size(
                    19.dp
                ),

            tint =
                ProfileGray
        )


        Spacer(

            modifier =
                Modifier.width(
                    12.dp
                )
        )


        Text(

            text =
                title,

            modifier =
                Modifier.weight(
                    1f
                ),

            fontSize =
                13.sp,

            color =
                Color.Black
        )


        Icon(

            imageVector =
                Icons
                    .Outlined
                    .ChevronRight,

            contentDescription =
                null,

            modifier =
                Modifier.size(
                    18.dp
                ),

            tint =
                ProfileGray
        )
    }
}


// ============================================================
// DIVIDER
// ============================================================

@Composable
private fun ProfileDivider() {

    HorizontalDivider(

        modifier =
            Modifier.padding(
                start = 46.dp
            ),

        color =
            ProfileDividerColor
    )
}


// ============================================================
// BOTTOM NAV
// ============================================================

@Composable
private fun ProfileBottomNavigationBar(

    onHomeClick: () -> Unit,

    onProjectsClick: () -> Unit,

    onMessagesClick: () -> Unit,

    onTasksClick: () -> Unit,

    onProfileClick: () -> Unit

) {

    NavigationBar(

        containerColor =
            Color.White,

        tonalElevation =
            3.dp

    ) {

        ProfileNavigationItem(

            title =
                "Home",

            icon =
                Icons.Outlined.Home,

            selected =
                false,

            onClick =
                onHomeClick
        )


        ProfileNavigationItem(

            title =
                "Projects",

            icon =
                Icons.Outlined.BusinessCenter,

            selected =
                false,

            onClick =
                onProjectsClick
        )


        ProfileNavigationItem(

            title =
                "Messages",

            icon =
                Icons.Outlined.ChatBubbleOutline,

            selected =
                false,

            onClick =
                onMessagesClick
        )


        ProfileNavigationItem(

            title =
                "Tasks",

            icon =
                Icons.Outlined.TaskAlt,

            selected =
                false,

            onClick =
                onTasksClick
        )


        ProfileNavigationItem(

            title =
                "Profile",

            icon =
                Icons.Outlined.Person,

            selected =
                true,

            onClick =
                onProfileClick
        )
    }
}


// ============================================================
// NAV ITEM
// ============================================================

@Composable
private fun RowScope.ProfileNavigationItem(

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
                    Modifier.size(
                        21.dp
                    )
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
            NavigationBarItemDefaults.colors(

                selectedIconColor =
                    ProfileOrange,

                selectedTextColor =
                    ProfileOrange,

                indicatorColor =
                    Color(
                        0xFFFFE7DD
                    ),

                unselectedIconColor =
                    Color.Gray,

                unselectedTextColor =
                    Color.Gray
            )
    )
}