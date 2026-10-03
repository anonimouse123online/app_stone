package com.example.capstonesample

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstonesample.data.api.RetrofitClient
import com.example.capstonesample.data.model.ForgotPasswordRequest
import com.example.capstonesample.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.util.Locale


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


                    // ------------------------------------------------
                    // Which settings sheet is open
                    // ------------------------------------------------
                    var showAccountSettings    by remember { mutableStateOf(false) }
                    var showNotifications      by remember { mutableStateOf(false) }
                    var showAppearance         by remember { mutableStateOf(false) }
                    var showDataStorage        by remember { mutableStateOf(false) }
                    var showSecurity           by remember { mutableStateOf(false) }
                    var showHelp               by remember { mutableStateOf(false) }

                    ProfileSettingRow(
                        icon  = Icons.Outlined.Person,
                        title = "Account Settings",
                        onClick = { showAccountSettings = true }
                    )

                    ProfileDivider()

                    ProfileSettingRow(
                        icon  = Icons.Outlined.Notifications,
                        title = "Notifications",
                        onClick = { showNotifications = true }
                    )

                    ProfileDivider()

                    ProfileSettingRow(
                        icon  = Icons.Outlined.Visibility,
                        title = "Appearance",
                        onClick = { showAppearance = true }
                    )

                    ProfileDivider()

                    ProfileSettingRow(
                        icon  = Icons.Outlined.Storage,
                        title = "Data and Storage",
                        onClick = { showDataStorage = true }
                    )

                    ProfileDivider()

                    ProfileSettingRow(
                        icon  = Icons.Outlined.Security,
                        title = "Security",
                        onClick = { showSecurity = true }
                    )

                    ProfileDivider()

                    ProfileSettingRow(
                        icon  = Icons.Outlined.HelpOutline,
                        title = "Help and Support",
                        onClick = { showHelp = true }
                    )

                    // ------------------------------------------------
                    // SETTINGS BOTTOM SHEETS
                    // ------------------------------------------------
                    if (showAccountSettings) {
                        AccountSettingsSheet(
                            profile = profile,
                            onDismiss = { showAccountSettings = false }
                        )
                    }
                    if (showNotifications) {
                        NotificationsSheet(
                            onDismiss = { showNotifications = false }
                        )
                    }
                    if (showAppearance) {
                        AppearanceSheet(
                            onDismiss = { showAppearance = false }
                        )
                    }
                    if (showDataStorage) {
                        DataStorageSheet(
                            onDismiss = { showDataStorage = false }
                        )
                    }
                    if (showSecurity) {
                        SecuritySheet(
                            email  = profile.email,
                            onDismiss = { showSecurity = false }
                        )
                    }
                    if (showHelp) {
                        HelpSupportSheet(
                            onDismiss = { showHelp = false }
                        )
                    }
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


// ============================================================
// HELPERS
// ============================================================

private fun getAppPrefs(context: Context): SharedPreferences =
    context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

/** Returns the total size of a directory in bytes. */
private fun dirSizeBytes(dir: File): Long =
    dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000L -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000L     -> "%.1f KB".format(bytes / 1_000.0)
    else                -> "$bytes B"
}


// ============================================================
// 1. ACCOUNT SETTINGS SHEET
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsSheet(
    profile: ProfileUiModel,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Account Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Your current account information",
                fontSize = 12.sp,
                color = ProfileGray
            )

            Spacer(Modifier.height(20.dp))

            // Full name field (read-only)
            AccountInfoItem(
                icon  = Icons.Outlined.Person,
                label = "Full Name",
                value = profile.fullName.ifBlank { "—" }
            )

            HorizontalDivider(color = ProfileDividerColor, modifier = Modifier.padding(vertical = 10.dp))

            // Email field (read-only)
            AccountInfoItem(
                icon  = Icons.Outlined.Email,
                label = "Email",
                value = profile.email.ifBlank { "—" }
            )

            HorizontalDivider(color = ProfileDividerColor, modifier = Modifier.padding(vertical = 10.dp))

            // Role field (read-only)
            AccountInfoItem(
                icon  = Icons.Outlined.Info,
                label = "Role",
                value = profile.role.ifBlank { "—" }
            )

            Spacer(Modifier.height(24.dp))

            Surface(
                shape  = RoundedCornerShape(12.dp),
                color  = Color(0xFFFFF3ED),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint  = ProfileOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text  = "To update your account info, please contact your administrator.",
                        fontSize = 11.sp,
                        color = ProfileOrange
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountInfoItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ProfileGray,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 10.sp, color = ProfileGray)
            Text(text = value, fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Medium)
        }
    }
}


// ============================================================
// 2. NOTIFICATIONS SHEET
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs   = remember { getAppPrefs(context) }

    var pushEnabled  by remember { mutableStateOf(prefs.getBoolean("notif_push",  true)) }
    var emailEnabled by remember { mutableStateOf(prefs.getBoolean("notif_email", true)) }
    var taskEnabled  by remember { mutableStateOf(prefs.getBoolean("notif_task",  true)) }
    var chatEnabled  by remember { mutableStateOf(prefs.getBoolean("notif_chat",  true)) }

    fun save() {
        prefs.edit()
            .putBoolean("notif_push",  pushEnabled)
            .putBoolean("notif_email", emailEnabled)
            .putBoolean("notif_task",  taskEnabled)
            .putBoolean("notif_chat",  chatEnabled)
            .apply()
    }

    ModalBottomSheet(
        onDismissRequest = { save(); onDismiss() },
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text("Notifications", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(Modifier.height(4.dp))
            Text("Manage how you receive alerts", fontSize = 12.sp, color = ProfileGray)
            Spacer(Modifier.height(20.dp))

            NotifToggleRow(
                title       = "Push Notifications",
                subtitle    = "Receive alerts on your device",
                checked     = pushEnabled,
                onToggle    = { pushEnabled = it; save() }
            )
            HorizontalDivider(color = ProfileDividerColor, modifier = Modifier.padding(vertical = 8.dp))

            NotifToggleRow(
                title       = "Email Notifications",
                subtitle    = "Get updates sent to your email",
                checked     = emailEnabled,
                onToggle    = { emailEnabled = it; save() }
            )
            HorizontalDivider(color = ProfileDividerColor, modifier = Modifier.padding(vertical = 8.dp))

            NotifToggleRow(
                title       = "Task Reminders",
                subtitle    = "Alerts for upcoming due dates",
                checked     = taskEnabled,
                onToggle    = { taskEnabled = it; save() }
            )
            HorizontalDivider(color = ProfileDividerColor, modifier = Modifier.padding(vertical = 8.dp))

            NotifToggleRow(
                title       = "Chat Messages",
                subtitle    = "Notifications for new messages",
                checked     = chatEnabled,
                onToggle    = { chatEnabled = it; save() }
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { save(); onDismiss() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape  = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ProfileOrange)
            ) {
                Text("Save Preferences", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun NotifToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title,    fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Medium)
            Text(text = subtitle, fontSize = 11.sp, color = ProfileGray)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor  = Color.White,
                checkedTrackColor  = ProfileOrange,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFCBD5E1)
            )
        )
    }
}


// ============================================================
// 3. APPEARANCE SHEET
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs   = remember { getAppPrefs(context) }

    var darkMode     by remember { mutableStateOf(prefs.getBoolean("appearance_dark",    false)) }
    var compactMode  by remember { mutableStateOf(prefs.getBoolean("appearance_compact", false)) }
    var largeText    by remember { mutableStateOf(prefs.getBoolean("appearance_large_text", false)) }

    // Selected accent: 0=Orange (default), 1=Blue, 2=Green
    var selectedAccent by remember { mutableIntStateOf(prefs.getInt("appearance_accent", 0)) }

    val accentColors = listOf(
        Color(0xFFF15A24) to "Orange",
        Color(0xFF3B82F6) to "Blue",
        Color(0xFF10B981) to "Green"
    )

    fun save() {
        prefs.edit()
            .putBoolean("appearance_dark",       darkMode)
            .putBoolean("appearance_compact",    compactMode)
            .putBoolean("appearance_large_text", largeText)
            .putInt("appearance_accent",         selectedAccent)
            .apply()
    }

    ModalBottomSheet(
        onDismissRequest = { save(); onDismiss() },
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text("Appearance", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(Modifier.height(4.dp))
            Text("Customize how the app looks", fontSize = 12.sp, color = ProfileGray)
            Spacer(Modifier.height(20.dp))

            // Dark Mode
            NotifToggleRow(
                title    = "Dark Mode",
                subtitle = "Switch to a dark color scheme",
                checked  = darkMode,
                onToggle = { darkMode = it; save() }
            )
            HorizontalDivider(color = ProfileDividerColor, modifier = Modifier.padding(vertical = 8.dp))

            // Compact Mode
            NotifToggleRow(
                title    = "Compact Layout",
                subtitle = "Show more content with smaller spacing",
                checked  = compactMode,
                onToggle = { compactMode = it; save() }
            )
            HorizontalDivider(color = ProfileDividerColor, modifier = Modifier.padding(vertical = 8.dp))

            // Large Text
            NotifToggleRow(
                title    = "Large Text",
                subtitle = "Increase font size for readability",
                checked  = largeText,
                onToggle = { largeText = it; save() }
            )

            Spacer(Modifier.height(16.dp))

            Text("Accent Color", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                accentColors.forEachIndexed { idx, (color, name) ->
                    val selected = selectedAccent == idx
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { selectedAccent = idx; save() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(color, CircleShape)
                                .then(
                                    if (selected) Modifier.background(color, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.Visibility,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = name,
                            fontSize = 10.sp,
                            color = if (selected) color else ProfileGray,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { save(); onDismiss() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape  = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ProfileOrange)
            ) {
                Text("Apply Changes", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


// ============================================================
// 4. DATA AND STORAGE SHEET
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataStorageSheet(onDismiss: () -> Unit) {
    val context  = LocalContext.current
    var cacheCleared by remember { mutableStateOf(false) }

    // Measure cache size (run once)
    val cacheDir = context.cacheDir
    var cacheSizeText by remember {
        mutableStateOf(formatBytes(dirSizeBytes(cacheDir)))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text("Data and Storage", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(Modifier.height(4.dp))
            Text("Manage cached data and storage usage", fontSize = 12.sp, color = ProfileGray)
            Spacer(Modifier.height(20.dp))

            // Cache card
            Surface(
                shape  = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ProfileDividerColor),
                color  = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFFFFF3ED), RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Storage,
                            contentDescription = null,
                            tint = ProfileOrange,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cache", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        Text(
                            text = if (cacheCleared) "Cleared successfully" else cacheSizeText,
                            fontSize = 12.sp,
                            color = if (cacheCleared) Color(0xFF10B981) else ProfileGray
                        )
                    }
                    if (!cacheCleared) {
                        OutlinedButton(
                            onClick = {
                                // Clear cache
                                cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                                cacheSizeText = formatBytes(dirSizeBytes(cacheDir))
                                cacheCleared = true
                            },
                            shape  = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ProfileOrange),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ProfileOrange),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Clear",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Clear", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Storage info rows
            StorageInfoRow(label = "App Data", value = formatBytes(dirSizeBytes(context.filesDir)))
            HorizontalDivider(color = ProfileDividerColor, modifier = Modifier.padding(vertical = 8.dp))
            StorageInfoRow(label = "Downloads", value = formatBytes(dirSizeBytes(context.getExternalFilesDir(null) ?: context.filesDir)))
            HorizontalDivider(color = ProfileDividerColor, modifier = Modifier.padding(vertical = 8.dp))
            StorageInfoRow(label = "Database", value = formatBytes(dirSizeBytes(context.getDatabasePath("placeholder").parentFile ?: context.filesDir)))

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape  = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ProfileOrange)
            ) {
                Text("Done", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StorageInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 13.sp, color = Color.Black, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = ProfileGray, fontWeight = FontWeight.Medium)
    }
}


// ============================================================
// 5. SECURITY SHEET  (change password via email OTP flow)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySheet(
    email: String,
    onDismiss: () -> Unit
) {
    val scope   = rememberCoroutineScope()
    val context = LocalContext.current

    // Step: 0 = home, 1 = OTP sent / verify, 2 = done
    var step       by remember { mutableIntStateOf(0) }
    var isLoading  by remember { mutableStateOf(false) }
    var errorMsg   by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    var otpValue      by remember { mutableStateOf("") }
    var newPassword   by remember { mutableStateOf("") }
    var showPassword  by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text("Security", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(Modifier.height(4.dp))
            Text("Change your account password", fontSize = 12.sp, color = ProfileGray)
            Spacer(Modifier.height(20.dp))

            when (step) {

                // -----------------------------------------------
                // STEP 0: Request password-reset OTP
                // -----------------------------------------------
                0 -> {
                    Surface(
                        shape  = RoundedCornerShape(14.dp),
                        color  = Color(0xFFF6F8FA),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = null,
                                tint = ProfileGray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Registered Email", fontSize = 10.sp, color = ProfileGray)
                                Text(email.ifBlank { "No email on record" }, fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "We will send a one-time password reset code to your registered email address.",
                        fontSize = 12.sp,
                        color = ProfileGray
                    )

                    errorMsg?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it, fontSize = 12.sp, color = Color.Red)
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (email.isBlank()) {
                                errorMsg = "No email address associated with your account."
                                return@Button
                            }
                            isLoading = true
                            errorMsg  = null
                            scope.launch {
                                try {
                                    val resp = RetrofitClient.api.forgotPassword(
                                        ForgotPasswordRequest(email = email)
                                    )
                                    if (resp.isSuccessful && resp.body()?.success == true) {
                                        step = 1
                                    } else {
                                        errorMsg = resp.body()?.message ?: "Failed to send OTP. Try again."
                                    }
                                } catch (e: Exception) {
                                    errorMsg = e.message ?: "Network error."
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape  = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ProfileOrange),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Send Reset Code", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // -----------------------------------------------
                // STEP 1: Enter OTP + new password
                // -----------------------------------------------
                1 -> {
                    Text(
                        text = "Enter the 6-digit code sent to $email and choose a new password.",
                        fontSize = 12.sp,
                        color = ProfileGray
                    )
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value         = otpValue,
                        onValueChange = { otpValue = it.take(6) },
                        label         = { Text("Reset Code") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        leadingIcon   = { Icon(Icons.Outlined.Security, null, tint = ProfileGray, modifier = Modifier.size(18.dp)) }
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value         = newPassword,
                        onValueChange = { newPassword = it },
                        label         = { Text("New Password") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon  = { Icon(Icons.Filled.Lock, null, tint = ProfileGray, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null,
                                    tint = ProfileGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )

                    errorMsg?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, fontSize = 12.sp, color = Color.Red)
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (otpValue.length < 6) { errorMsg = "Enter the 6-digit code."; return@Button }
                            if (newPassword.length < 6) { errorMsg = "Password must be at least 6 characters."; return@Button }
                            isLoading = true
                            errorMsg  = null
                            scope.launch {
                                try {
                                    val resp = RetrofitClient.api.verifyResetCode(
                                        com.example.capstonesample.data.model.VerifyResetCodeRequest(
                                            email = email,
                                            code  = otpValue
                                        )
                                    )
                                    if (resp.isSuccessful && resp.body()?.success == true) {
                                        val resetResp = RetrofitClient.api.resetPassword(
                                            com.example.capstonesample.data.model.ResetPasswordRequest(
                                                email       = email,
                                                code        = otpValue,
                                                newPassword = newPassword
                                            )
                                        )
                                        if (resetResp.isSuccessful && resetResp.body()?.success == true) {
                                            step = 2
                                        } else {
                                            errorMsg = resetResp.body()?.message ?: "Failed to reset password."
                                        }
                                    } else {
                                        errorMsg = resp.body()?.message ?: "Invalid or expired code."
                                    }
                                } catch (e: Exception) {
                                    errorMsg = e.message ?: "Network error."
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape  = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ProfileOrange),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Change Password", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    TextButton(onClick = { step = 0; errorMsg = null }) {
                        Text("← Back", color = ProfileGray, fontSize = 13.sp)
                    }
                }

                // -----------------------------------------------
                // STEP 2: Success
                // -----------------------------------------------
                2 -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFFD1FAE5), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Security,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Password Changed!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Your password has been updated successfully. Use your new password next time you log in.",
                            fontSize = 13.sp,
                            color = ProfileGray
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape  = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ProfileOrange)
                        ) {
                            Text("Done", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}


// ============================================================
// 6. HELP AND SUPPORT SHEET
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportSheet(onDismiss: () -> Unit) {

    val faqs = listOf(
        "How do I join a project?" to
            "Go to the Projects screen and tap 'Join Project'. Enter the project code provided by your project manager.",
        "How do I log hours?" to
            "Open any task assigned to you and use the 'Log Hours' button inside the task details.",
        "Can I change my role?" to
            "Roles are assigned by the administrator. Contact your manager if your role needs to be updated.",
        "How do I complete a task?" to
            "Open the task, fill in your report, then tap 'Mark as Completed'. It will be sent for review.",
        "What happens if I'm offline?" to
            "The app will queue your actions and sync them automatically when connectivity is restored."
    )

    var expandedIndex by remember { mutableIntStateOf(-1) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Text("Help & Support", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(Modifier.height(4.dp))
            Text("Frequently asked questions", fontSize = 12.sp, color = ProfileGray)
            Spacer(Modifier.height(20.dp))

            faqs.forEachIndexed { idx, (question, answer) ->
                val isOpen = expandedIndex == idx
                Surface(
                    shape  = RoundedCornerShape(14.dp),
                    color  = if (isOpen) Color(0xFFFFF3ED) else Color(0xFFF6F8FA),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedIndex = if (isOpen) -1 else idx }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = question,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = if (isOpen) ProfileOrange else ProfileGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (isOpen) {
                            Spacer(Modifier.height(8.dp))
                            Text(answer, fontSize = 12.sp, color = ProfileGray)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(16.dp))

            Text("Contact Support", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
            Spacer(Modifier.height(10.dp))

            // Email support card
            Surface(
                shape  = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ProfileDividerColor),
                color  = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFFFFF3ED), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Email, null, tint = ProfileOrange, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Email Us", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        Text("support@constructionapp.ph", fontSize = 11.sp, color = ProfileGray)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Phone support card
            Surface(
                shape  = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ProfileDividerColor),
                color  = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFFFFF3ED), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Phone, null, tint = ProfileOrange, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Call Us", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        Text("+63 2 8123 4567 · Mon–Fri, 8AM–5PM", fontSize = 11.sp, color = ProfileGray)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape  = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ProfileOrange),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ProfileOrange)
            ) {
                Text("Close", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}