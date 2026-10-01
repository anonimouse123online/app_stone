package com.example.capstonesample

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.capstonesample.data.api.NotificationData
import com.example.capstonesample.security.TokenManager
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontStyle
import com.example.capstonesample.data.api.WeatherService
import com.example.capstonesample.data.api.LiveWeatherInfo

import com.example.capstonesample.data.api.RetrofitClient
import com.example.capstonesample.data.api.TaskResponse
import com.example.capstonesample.data.api.TimeLogCreateRequest
import java.time.OffsetDateTime
import java.time.Duration

import java.text.SimpleDateFormat
import java.util.Calendar
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


data class DashboardLocationUi(
    val latitude: Double,
    val longitude: Double,
    val label: String
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

    userName: String = "",

    userRole: String = "",

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
    var notifications by remember {
        mutableStateOf<List<NotificationData>>(emptyList())
    }

    var notificationsLoading by remember {
        mutableStateOf(false)
    }

    var notificationError by remember {
        mutableStateOf<String?>(null)
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

    var showNotifications by remember {
        mutableStateOf(false)
    }


    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentLocation by remember {
        mutableStateOf<DashboardLocationUi?>(null)
    }

    var liveWeather by remember {
        mutableStateOf<LiveWeatherInfo?>(null)
    }

    var locationMessage by remember {
        mutableStateOf("No location found")
    }

    var isLocating by remember {
        mutableStateOf(false)
    }

    var isLoggingTime by remember {
        mutableStateOf(false)
    }

    var logTimeMessage by remember {
        mutableStateOf<String?>(null)
    }

    val userSession = remember { TokenManager.getUserSession(context) }
    val resolvedUserName = remember(userName, userSession) {
        if (userName.isNotBlank()) userName
        else userSession?.fullName?.takeIf { it.isNotBlank() } ?: "Site Engineer"
    }
    val resolvedUserRole = remember(userRole, userSession) {
        if (userRole.isNotBlank()) userRole
        else userSession?.role?.takeIf { it.isNotBlank() } ?: "Field Engineer"
    }
    val initials = remember(resolvedUserName) {
        generateInitials(resolvedUserName)
    }

    var showLogTimeDialog by remember {
        mutableStateOf(false)
    }

    fun loadNotifications() {

        coroutineScope.launch {

            notificationsLoading = true
            notificationError = null

            try {

                val authorizationToken =
                    if (token.startsWith("Bearer ")) {
                        token
                    } else {
                        "Bearer $token"
                    }


                val response =
                    RetrofitClient.api.getNotifications(
                        token = authorizationToken
                    )


                if (response.isSuccessful) {

                    val body =
                        response.body()


                    if (body?.success == true) {

                        notifications =
                            body.data

                    } else {

                        notificationError =
                            "Unable to load notifications."
                    }

                } else {

                    notificationError =
                        when (response.code()) {

                            401 ->
                                "Session expired. Please login again."

                            403 ->
                                "You do not have permission to view notifications."

                            404 ->
                                "Notifications API was not found."

                            500 ->
                                "Server error while loading notifications."

                            else ->
                                "Unable to load notifications (${response.code()})."
                        }
                }

            } catch (e: Exception) {

                notificationError =
                    e.message
                        ?: "Unable to connect to SitePulse server."

            } finally {

                notificationsLoading = false
            }
        }
    }

    fun loadCurrentLocation() {
        isLocating = true
        locationMessage = "Getting current location..."

        getCurrentDeviceLocation(
            context = context,
            onSuccess = { location ->
                coroutineScope.launch {
                    val readableLocation = resolveLocationLabel(
                        context = context,
                        location = location
                    )

                    currentLocation = DashboardLocationUi(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        label = readableLocation
                    )

                    locationMessage = readableLocation
                    isLocating = false

                    launch {
                        liveWeather = WeatherService.getLiveWeather(location.latitude, location.longitude)
                    }
                }
            },
            onError = { message ->
                currentLocation = null
                locationMessage = message
                isLocating = false
            }
        )
    }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {
                loadCurrentLocation()
            } else {
                currentLocation = null
                locationMessage = "No location found"
            }
        }

    var pendingDashboardPhotoUri by remember {
        mutableStateOf<Uri?>(null)
    }
    var capturedDashboardPhotoUri by remember {
        mutableStateOf<Uri?>(null)
    }
    var showCapturedDashboardDialog by remember {
        mutableStateOf(false)
    }

    val dashboardCameraLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                capturedDashboardPhotoUri = pendingDashboardPhotoUri
                showCapturedDashboardDialog = true
            } else {
                pendingDashboardPhotoUri = null
            }
        }

    fun launchDashboardCamera() {
        try {
            val picturesDirectory = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "SitePulse"
            )
            if (!picturesDirectory.exists()) {
                picturesDirectory.mkdirs()
            }
            val imageFile = File(
                picturesDirectory,
                "site_${System.currentTimeMillis()}.jpg"
            )
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                imageFile
            )
            pendingDashboardPhotoUri = photoUri
            dashboardCameraLauncher.launch(photoUri)
        } catch (e: Exception) {
            Log.e("DASH_CAMERA", "Error launching camera", e)
            Toast.makeText(
                context,
                "Unable to open camera: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val dashboardCameraPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                launchDashboardCamera()
            } else {
                Toast.makeText(
                    context,
                    "Camera permission is required to capture site photos.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    fun openDashboardCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            launchDashboardCamera()
        } else {
            dashboardCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun requestLocation() {
        val fineGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            loadCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun submitTimeLog(
        selectedProject: String = projects.firstOrNull()?.name ?: "General Project",
        hours: String = "8",
        workCompleted: String = "",
        weather: String = "Sunny",
        temperature: String = "",
        workOnSite: String = "1",
        supervisors: String = "1",
        subContractors: String = "0",
        materialsDelivered: String = "",
        equipmentUsed: String = "",
        additionalNotes: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val trimmedProject = selectedProject.trim()
        if (trimmedProject.isBlank()) {
            Toast.makeText(context, "Please select or enter a project name.", Toast.LENGTH_SHORT).show()
            return
        }

        coroutineScope.launch {
            isLoggingTime = true
            logTimeMessage = null

            try {
                val authorizationToken =
                    if (token.startsWith("Bearer ")) {
                        token
                    } else {
                        "Bearer $token"
                    }

                val location = currentLocation
                val locationNote = if (location != null) {
                    "Location: ${location.label} (${location.latitude}, ${location.longitude})"
                } else {
                    "Location: Field / Mobile App"
                }

                val finalNotes = if (additionalNotes.isNotBlank()) {
                    "${additionalNotes.trim()} • $locationNote"
                } else {
                    locationNote
                }

                val parsedHours = hours.trim().ifBlank { "8" }
                val parsedTemp = temperature.trim().toDoubleOrNull()
                val parsedWorkOnSite = workOnSite.trim().toIntOrNull() ?: 1
                val parsedSupervisors = supervisors.trim().toIntOrNull() ?: 1
                val parsedSubContractors = subContractors.trim().toIntOrNull() ?: 0

                val response =
                    RetrofitClient.api.createTimeLog(
                        token = authorizationToken,
                        request = TimeLogCreateRequest(
                            projectName = trimmedProject,
                            engineerName = resolvedUserName,
                            date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                            workOnSite = parsedWorkOnSite,
                            supervisors = parsedSupervisors,
                            subContractors = parsedSubContractors,
                            totalWorkHours = parsedHours,
                            weather = weather.trim().ifBlank { "Sunny" },
                            temperature = parsedTemp,
                            workCompleted = if (workCompleted.isNotBlank()) workCompleted.trim() else "Daily site supervision and engineering tasks.",
                            materialsDelivered = materialsDelivered.trim(),
                            equipmentUsed = equipmentUsed.trim(),
                            additionalNotes = finalNotes,
                            hasIncident = false
                        )
                    )

                if (response.isSuccessful && response.body()?.success == true) {
                    val successMsg = "Time logged successfully ($parsedHours hrs for $trimmedProject)!"
                    logTimeMessage = successMsg
                    Toast.makeText(context, "✅ $successMsg", Toast.LENGTH_LONG).show()
                    onSuccess()
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Invalid log time data. Please check project name and hours."
                        401 -> "Session expired. Please login again."
                        403 -> "You do not have permission to log time."
                        404 -> "Time log endpoint not found."
                        500 -> "Server error while logging time."
                        else -> "Unable to log time (${response.code()})."
                    }
                    logTimeMessage = errorMsg
                    Toast.makeText(context, "❌ $errorMsg", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unable to connect to SitePulse server."
                logTimeMessage = errorMsg
                Toast.makeText(context, "❌ $errorMsg", Toast.LENGTH_LONG).show()
            } finally {
                isLoggingTime = false
            }
        }
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
                                    (project.progress ?: 0.0)
                                        .toInt()
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
    // ========================================================
    val userName = resolvedUserName
    val userRole = resolvedUserRole


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
                initials = initials,
                onNotificationClick = {

                    showNotifications = true

                    loadNotifications()
                }
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


                    Spacer(
                        modifier = Modifier.height(7.dp)
                    )

                    DashboardLocationRow(
                        locationText = if (liveWeather != null) "$locationMessage • ${liveWeather?.formattedTemp} ${liveWeather?.weather}" else locationMessage,
                        isLoading = isLocating,
                        hasLocation = currentLocation != null,
                        onClick = {
                            requestLocation()
                        }
                    )

                    logTimeMessage?.let { message ->

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        Text(
                            text = message,
                            fontSize = 10.sp,
                            color =
                                if (message.contains("success", ignoreCase = true)) {
                                    Green
                                } else {
                                    SitePulseOrange
                                }
                        )
                    }
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
                QuickActions(
                    isLoggingTime = isLoggingTime,
                    onLogTimeClick = {
                        if (currentLocation == null) {
                            requestLocation()
                        }
                        showLogTimeDialog = true
                    },
                    onCaptureClick = {
                        openDashboardCamera()
                    },
                    onReportClick = {
                        onTasksClick()
                        Toast.makeText(
                            context,
                            "Select a task to generate an AI field report or log issues",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
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

    if (showNotifications) {

        NotificationDialog(
            notifications = notifications,
            isLoading = notificationsLoading,
            errorMessage = notificationError,

            onRefresh = {
                loadNotifications()
            },

            onDismiss = {
                showNotifications = false
            }
        )
    }

    if (showCapturedDashboardDialog && capturedDashboardPhotoUri != null) {
        AlertDialog(
            onDismissRequest = { showCapturedDashboardDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF1B9A41),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Photo Captured!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Text(
                    "Your site evidence photo has been saved to the SitePulse directory.",
                    fontSize = 13.sp,
                    color = Color(0xFF555555)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCapturedDashboardDialog = false
                        onTasksClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF15A24))
                ) {
                    Text("Go to Tasks")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCapturedDashboardDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    if (showLogTimeDialog) {
        LogTimeDialog(
            projects = projects.map { it.name },
            initialEngineerName = resolvedUserName,
            currentLocation = currentLocation,
            locationMessage = locationMessage,
            isLocating = isLocating,
            isSubmitting = isLoggingTime,
            initialLiveWeather = liveWeather,
            onRequestLocation = { requestLocation() },
            onDismiss = {
                if (!isLoggingTime) {
                    showLogTimeDialog = false
                }
            },
            onSubmit = { form ->
                submitTimeLog(
                    selectedProject = form.projectName,
                    hours = form.hours,
                    workCompleted = form.workCompleted,
                    weather = form.weather,
                    temperature = form.temperature,
                    workOnSite = form.workOnSite,
                    supervisors = form.supervisors,
                    subContractors = form.subContractors,
                    materialsDelivered = form.materialsDelivered,
                    equipmentUsed = form.equipmentUsed,
                    additionalNotes = form.additionalNotes,
                    onSuccess = {
                        showLogTimeDialog = false
                    }
                )
            }
        )
    }
}



// ============================================================
// FORMAT NOTIFICATION TIME
// ============================================================

private fun formatNotificationTime(
    createdAt: String?
): String {

    if (createdAt.isNullOrBlank()) {
        return ""
    }

    return try {

        val inputFormats =
            listOf(
                SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                    Locale.US
                ),
                SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ssXXX",
                    Locale.US
                ),
                SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss.SSSXXX",
                    Locale.US
                ),
                SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ssXXX",
                    Locale.US
                )
            )

        var notificationDate: Date? = null

        for (format in inputFormats) {
            try {
                notificationDate =
                    format.parse(createdAt)

                if (notificationDate != null) {
                    break
                }
            } catch (_: Exception) {
                // Try next format
            }
        }

        if (notificationDate == null) {
            return createdAt
        }

        val now =
            Date()

        val difference =
            now.time -
                    notificationDate.time

        val seconds =
            difference / 1000

        val minutes =
            seconds / 60

        val hours =
            minutes / 60

        val days =
            hours / 24


        when {

            seconds < 60 ->
                "Just now"

            minutes < 60 ->
                "$minutes minute${if (minutes == 1L) "" else "s"} ago"

            hours < 24 ->
                "$hours hour${if (hours == 1L) "" else "s"} ago"

            days < 7 ->
                "$days day${if (days == 1L) "" else "s"} ago"

            else ->
                SimpleDateFormat(
                    "MMM dd, yyyy",
                    Locale.getDefault()
                ).format(
                    notificationDate
                )
        }

    } catch (_: Exception) {

        createdAt
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
    initials: String,
    onNotificationClick: () -> Unit
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
                onClick = onNotificationClick
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
// NOTIFICATION DIALOG
// ============================================================

@Composable
private fun NotificationDialog(

    notifications: List<NotificationData>,

    isLoading: Boolean,

    errorMessage: String?,

    onRefresh: () -> Unit,

    onDismiss: () -> Unit

) {

    Dialog(
        onDismissRequest = onDismiss
    ) {

        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.82f),

            shape =
                RoundedCornerShape(24.dp),

            colors =
                CardDefaults.cardColors(
                    containerColor = Color.White
                )
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(20.dp)
            ) {

                // ====================================================
                // HEADER
                // ====================================================

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(46.dp)
                                .background(
                                    Color(0xFFFFEEE7),
                                    CircleShape
                                ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Notifications,

                            contentDescription = null,

                            modifier =
                                Modifier.size(24.dp),

                            tint =
                                SitePulseOrange
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.width(12.dp)
                    )


                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                "Notifications",

                            fontSize =
                                22.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                Color.Black
                        )


                        Text(
                            text =
                                "Updates and announcements from SitePulse",

                            fontSize =
                                11.sp,

                            color =
                                TextGray
                        )
                    }


                    IconButton(
                        onClick = onRefresh,
                        enabled = !isLoading
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Refresh,

                            contentDescription =
                                "Refresh notifications",

                            tint =
                                SitePulseOrange
                        )
                    }


                    IconButton(
                        onClick = onDismiss
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Close,

                            contentDescription =
                                "Close notifications"
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )


                HorizontalDivider(
                    color =
                        Color(0xFFEAEAEA)
                )


                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )


                // ====================================================
                // LOADING
                // ====================================================

                if (isLoading) {

                    Box(
                        modifier =
                            Modifier.fillMaxSize(),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        CircularProgressIndicator(
                            color =
                                SitePulseOrange
                        )
                    }

                    return@Column
                }


                // ====================================================
                // ERROR
                // ====================================================

                if (errorMessage != null) {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(12.dp),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Color(0xFFFFE7E7)
                            )
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(14.dp)
                        ) {

                            Text(
                                text =
                                    errorMessage,

                                fontSize =
                                    11.sp,

                                color =
                                    Color.Red
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )


                            Text(
                                text =
                                    "Try again",

                                fontSize =
                                    11.sp,

                                fontWeight =
                                    FontWeight.Bold,

                                color =
                                    SitePulseOrange,

                                modifier =
                                    Modifier.clickable {
                                        onRefresh()
                                    }
                            )
                        }
                    }

                    return@Column
                }


                // ====================================================
                // EMPTY
                // ====================================================

                if (notifications.isEmpty()) {

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
                                    Icons.Outlined.NotificationsNone,

                                contentDescription =
                                    null,

                                modifier =
                                    Modifier.size(48.dp),

                                tint =
                                    TextGray
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(10.dp)
                            )


                            Text(
                                text =
                                    "No notifications yet",

                                fontSize =
                                    14.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )


                            Text(
                                text =
                                    "Announcements from the admin will appear here.",

                                fontSize =
                                    10.sp,

                                color =
                                    TextGray
                            )
                        }
                    }

                    return@Column
                }


                // ====================================================
                // API NOTIFICATIONS
                // ====================================================

                LazyColumn(
                    modifier =
                        Modifier.fillMaxSize(),

                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    items(
                        items =
                            notifications,

                        key = {
                            it.id
                        }
                    ) { notification ->

                        NotificationItem(
                            title =
                                notification.title,

                            message =
                                notification.message,

                            time =
                                formatNotificationTime(
                                    notification.createdAt
                                ),

                            unread =
                                true
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// LOG TIME FORM DATA
// ============================================================

data class LogTimeFormData(
    val projectName: String,
    val hours: String,
    val workCompleted: String,
    val weather: String,
    val temperature: String,
    val workOnSite: String,
    val supervisors: String,
    val subContractors: String,
    val materialsDelivered: String,
    val equipmentUsed: String,
    val additionalNotes: String
)

// ============================================================
// LOG TIME DIALOG
// ============================================================

@Composable
private fun LogTimeDialog(
    projects: List<String>,
    initialEngineerName: String,
    currentLocation: DashboardLocationUi?,
    locationMessage: String,
    isLocating: Boolean,
    isSubmitting: Boolean,
    initialLiveWeather: LiveWeatherInfo? = null,
    onRequestLocation: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (form: LogTimeFormData) -> Unit
) {
    val dialogScope = rememberCoroutineScope()

    var liveWeather by remember {
        mutableStateOf<LiveWeatherInfo?>(initialLiveWeather)
    }
    var isFetchingWeather by remember {
        mutableStateOf(false)
    }

    var selectedProject by remember {
        mutableStateOf(projects.firstOrNull() ?: "")
    }
    var customProject by remember {
        mutableStateOf("")
    }
    var isCustomProject by remember {
        mutableStateOf(projects.isEmpty())
    }
    var projectMenuExpanded by remember {
        mutableStateOf(false)
    }

    var hours by remember {
        mutableStateOf("8")
    }
    var workCompleted by remember {
        mutableStateOf("")
    }

    var weather by remember {
        val defaultWeather = if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) in 6..17) "Sunny" else "Clear"
        mutableStateOf(initialLiveWeather?.weather ?: defaultWeather)
    }
    var temperature by remember {
        mutableStateOf(initialLiveWeather?.let { String.format(Locale.US, "%.1f", it.temperature) } ?: "28")
    }

    fun fetchWeather(lat: Double, lon: Double) {
        dialogScope.launch {
            isFetchingWeather = true
            val result = WeatherService.getLiveWeather(lat, lon)
            liveWeather = result
            weather = result.weather
            temperature = String.format(Locale.US, "%.1f", result.temperature)
            isFetchingWeather = false
        }
    }

    LaunchedEffect(currentLocation) {
        if (currentLocation != null) {
            fetchWeather(currentLocation.latitude, currentLocation.longitude)
        } else {
            onRequestLocation()
        }
    }

    var workOnSite by remember {
        mutableStateOf("1")
    }
    var supervisors by remember {
        mutableStateOf("1")
    }
    var subContractors by remember {
        mutableStateOf("0")
    }

    var materialsDelivered by remember {
        mutableStateOf("")
    }
    var equipmentUsed by remember {
        mutableStateOf("")
    }
    var additionalNotes by remember {
        mutableStateOf("")
    }

    var localError by remember {
        mutableStateOf<String?>(null)
    }

    val activeProjectName = if (isCustomProject || projects.isEmpty()) customProject else selectedProject

    Dialog(
        onDismissRequest = {
            if (!isSubmitting) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // ==================== FIXED HEADER ====================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(OrangeLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = SitePulseOrange,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Log Work Hours",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Black
                        )
                        Text(
                            text = "Record daily engineering work & site log",
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ==================== SCROLLABLE BODY ====================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Engineer Badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8F9FA), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Engineer:",
                            fontSize = 12.sp,
                            color = TextGray
                        )
                        Text(
                            text = initialEngineerName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                    }

                    // Project Selector
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Project *",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )

                            if (projects.isNotEmpty()) {
                                Text(
                                    text = if (isCustomProject) "Choose from list" else "+ Custom name",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SitePulseOrange,
                                    modifier = Modifier.clickable {
                                        isCustomProject = !isCustomProject
                                        localError = null
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (!isCustomProject && projects.isNotEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedCard(
                                    onClick = { if (!isSubmitting) projectMenuExpanded = !projectMenuExpanded },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = Color.White
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (selectedProject.isNotBlank()) selectedProject else "Select Project",
                                            fontSize = 14.sp,
                                            fontWeight = if (selectedProject.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                                            color = if (selectedProject.isNotBlank()) Color.Black else TextGray,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = if (projectMenuExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                            contentDescription = "Expand",
                                            tint = TextGray
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = projectMenuExpanded,
                                    onDismissRequest = { projectMenuExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.75f)
                                ) {
                                    projects.forEach { proj ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = proj,
                                                    fontWeight = if (proj == selectedProject) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (proj == selectedProject) SitePulseOrange else Color.Black
                                                )
                                            },
                                            onClick = {
                                                selectedProject = proj
                                                projectMenuExpanded = false
                                                localError = null
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = customProject,
                                onValueChange = {
                                    customProject = it
                                    localError = null
                                },
                                placeholder = { Text("Enter project name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isSubmitting
                            )
                        }
                    }

                    // Hours Worked
                    Column {
                        Text(
                            text = "Work Hours *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = hours,
                            onValueChange = {
                                if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    hours = it
                                    localError = null
                                }
                            },
                            placeholder = { Text("8") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSubmitting
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Preset hour chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("4", "8", "10", "12").forEach { h ->
                                val isSelected = hours == h
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) SitePulseOrange else Color(0xFFF1F3F5),
                                    modifier = Modifier.clickable {
                                        hours = h
                                        localError = null
                                    }
                                ) {
                                    Text(
                                        text = "${h}h",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color.DarkGray,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Work Completed
                    Column {
                        Text(
                            text = "Work Completed / Accomplishment",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = workCompleted,
                            onValueChange = { workCompleted = it },
                            placeholder = { Text("e.g. Concrete pouring of columns, electrical piping inspection") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSubmitting
                        )
                    }

                    // ============================================================
                    // AUTOMATIC SITE CONDITIONS (AUTO-DETECTED VIA GPS)
                    // ============================================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F9FE)),
                        border = BorderStroke(1.dp, Color(0xFFDCE7F5))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header: Title & Live Badge / Refresh
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val isDay = liveWeather?.isDay ?: (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) in 6..17)
                                    val icon = when (weather.lowercase(Locale.ROOT)) {
                                        "rainy" -> "🌧️"
                                        "cloudy" -> if (isDay) "⛅" else "☁️"
                                        "windy" -> "💨"
                                        "clear" -> "🌙"
                                        else -> if (isDay) "☀️" else "🌙"
                                    }
                                    Text(
                                        text = icon,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "Site Conditions (Auto GPS)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (liveWeather != null) Color(0xFFE6F4EA) else OrangeLight
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(
                                                        if (liveWeather != null) Green else SitePulseOrange,
                                                        CircleShape
                                                    )
                                            )
                                            Text(
                                                text = if (isFetchingWeather) "Detecting..." else if (liveWeather != null) "Live GPS" else "Auto GPS",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (liveWeather != null) Color(0xFF137333) else SitePulseOrange
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            if (currentLocation != null) {
                                                fetchWeather(currentLocation.latitude, currentLocation.longitude)
                                            } else {
                                                onRequestLocation()
                                            }
                                        },
                                        modifier = Modifier.size(28.dp),
                                        enabled = !isFetchingWeather && !isSubmitting
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Refresh,
                                            contentDescription = "Refresh live weather",
                                            tint = SitePulseOrange,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            if (isFetchingWeather && liveWeather == null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = SitePulseOrange
                                    )
                                    Text(
                                        text = "Detecting real-time weather at your site location...",
                                        fontSize = 12.sp,
                                        color = TextGray
                                    )
                                }
                            } else if (liveWeather != null) {
                                val info = liveWeather!!
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "${info.roundedTemp}°C",
                                            fontSize = 30.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "Site Temperature",
                                            fontSize = 11.sp,
                                            color = TextGray
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color.White,
                                            shadowElevation = 1.dp
                                        ) {
                                            Text(
                                                text = "${info.weather} • ${info.conditionLabel}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SitePulseOrange,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "📍 ${currentLocation?.label ?: "Site Location"}",
                                            fontSize = 11.sp,
                                            color = TextGray,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (info.humidity != null) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text("💧", fontSize = 11.sp)
                                                Text(
                                                    text = "Humidity: ${info.humidity}%",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF334155),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    if (info.windSpeed != null) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text("💨", fontSize = 11.sp)
                                                Text(
                                                    text = "Wind: ${String.format(Locale.US, "%.1f", info.windSpeed)} km/h",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF334155),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = "⚡ Automatically captured from live site weather. No manual input needed.",
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = Color(0xFF64748B)
                                )
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Auto weather (${weather} • ${temperature}°C)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.Black
                                        )
                                        if (currentLocation != null) {
                                            TextButton(
                                                onClick = {
                                                    fetchWeather(currentLocation.latitude, currentLocation.longitude)
                                                }
                                            ) {
                                                Text("Detect Live", color = SitePulseOrange, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                    if (currentLocation == null) {
                                        Text(
                                            text = "Enable location to auto-fetch site weather coordinates.",
                                            fontSize = 11.sp,
                                            color = Color(0xFFB45309)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ============================================================
                    // MANPOWER (Workers, Supervisors, Sub-contractors)
                    // ============================================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF9F6))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Manpower on Site",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SitePulseOrange
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Workers
                                OutlinedTextField(
                                    value = workOnSite,
                                    onValueChange = {
                                        if (it.isEmpty() || it.matches(Regex("^\\d*$"))) workOnSite = it
                                    },
                                    label = { Text("Workers", fontSize = 11.sp) },
                                    placeholder = { Text("1") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = !isSubmitting,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )

                                // Supervisors
                                OutlinedTextField(
                                    value = supervisors,
                                    onValueChange = {
                                        if (it.isEmpty() || it.matches(Regex("^\\d*$"))) supervisors = it
                                    },
                                    label = { Text("Supervisors", fontSize = 11.sp) },
                                    placeholder = { Text("1") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = !isSubmitting,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )

                                // Sub-contractors
                                OutlinedTextField(
                                    value = subContractors,
                                    onValueChange = {
                                        if (it.isEmpty() || it.matches(Regex("^\\d*$"))) subContractors = it
                                    },
                                    label = { Text("Sub-contractors", fontSize = 10.sp) },
                                    placeholder = { Text("0") },
                                    modifier = Modifier.weight(1.1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = !isSubmitting,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // ============================================================
                    // MATERIALS & EQUIPMENT
                    // ============================================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF9F6))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Site Logistics & Equipment",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SitePulseOrange
                            )

                            // Materials Delivered
                            OutlinedTextField(
                                value = materialsDelivered,
                                onValueChange = { materialsDelivered = it },
                                label = { Text("Materials Delivered") },
                                placeholder = { Text("e.g. 50 bags cement, 100 pcs 16mm rebar, 2 loads gravel") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 3,
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isSubmitting,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )

                            // Equipment Used
                            OutlinedTextField(
                                value = equipmentUsed,
                                onValueChange = { equipmentUsed = it },
                                label = { Text("Equipment Used") },
                                placeholder = { Text("e.g. 1 Backhoe, 1 Concrete Mixer, 2 Plate Compactors") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 3,
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isSubmitting,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                        }
                    }

                    // ============================================================
                    // ADDITIONAL NOTES
                    // ============================================================
                    Column {
                        Text(
                            text = "Additional Notes",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = additionalNotes,
                            onValueChange = { additionalNotes = it },
                            placeholder = { Text("e.g. Delay caused by utility lines, shift handover note, safety reminders") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSubmitting
                        )
                    }

                    // Location Status Banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8F9FA),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = if (currentLocation != null) Green else SitePulseOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isLocating) "Locating device..." else (currentLocation?.label ?: locationMessage),
                                    fontSize = 11.sp,
                                    color = TextGray,
                                    maxLines = 1
                                )
                            }

                            if (currentLocation == null && !isLocating) {
                                Text(
                                    text = "Detect",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SitePulseOrange,
                                    modifier = Modifier.clickable { onRequestLocation() }
                                )
                            }
                        }
                    }

                    // Error message banner
                    if (localError != null) {
                        Text(
                            text = localError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ==================== FIXED FOOTER ====================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (activeProjectName.isBlank()) {
                                localError = "Please select or enter a project name."
                                return@Button
                            }
                            val numHours = hours.trim().toDoubleOrNull()
                            if (numHours == null || numHours <= 0) {
                                localError = "Please enter valid work hours (e.g. 8)."
                                return@Button
                            }
                            onSubmit(
                                LogTimeFormData(
                                    projectName = activeProjectName.trim(),
                                    hours = hours.trim(),
                                    workCompleted = workCompleted.trim(),
                                    weather = weather,
                                    temperature = temperature.trim(),
                                    workOnSite = workOnSite.trim(),
                                    supervisors = supervisors.trim(),
                                    subContractors = subContractors.trim(),
                                    materialsDelivered = materialsDelivered.trim(),
                                    equipmentUsed = equipmentUsed.trim(),
                                    additionalNotes = additionalNotes.trim()
                                )
                            )
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SitePulseOrange
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Saving...", color = Color.White, fontSize = 13.sp)
                        } else {
                            Text("Submit Log", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// NOTIFICATION ITEM
// ============================================================

@Composable
private fun NotificationItem(
    title: String,
    message: String,
    time: String,
    unread: Boolean
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    // TODO: Open notification details or mark as read later
                },

        shape =
            RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (unread) {
                        Color(0xFFFFF4EE)
                    } else {
                        Color(0xFFF8F8F8)
                    }
            )
    ) {

        Row(
            modifier =
                Modifier.padding(14.dp),

            verticalAlignment =
                Alignment.Top
        ) {

            Box(
                modifier =
                    Modifier
                        .size(42.dp)
                        .background(
                            if (unread) {
                                Color(0xFFFFE2D5)
                            } else {
                                Color(0xFFECECEC)
                            },
                            CircleShape
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Campaign,

                    contentDescription = null,

                    modifier =
                        Modifier.size(21.dp),

                    tint =
                        if (unread) {
                            SitePulseOrange
                        } else {
                            TextGray
                        }
                )
            }


            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )


            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    if (unread) {
                        Box(
                            modifier =
                                Modifier
                                    .size(8.dp)
                                    .background(
                                        SitePulseOrange,
                                        CircleShape
                                    )
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(5.dp)
                )


                Text(
                    text = message,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = TextGray
                )


                Spacer(
                    modifier =
                        Modifier.height(7.dp)
                )


                Text(
                    text = time,
                    fontSize = 9.sp,
                    color = SitePulseOrange,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


// ============================================================
// LOCATION
// ============================================================

@Composable
private fun DashboardLocationRow(
    locationText: String,
    isLoading: Boolean,
    hasLocation: Boolean,
    onClick: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = !isLoading,
                    onClick = onClick
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(
            imageVector =
                if (hasLocation) {
                    Icons.Outlined.LocationOn
                } else {
                    Icons.Outlined.LocationOff
                },
            contentDescription = "Current location",
            modifier = Modifier.size(16.dp),
            tint =
                if (hasLocation) {
                    Green
                } else {
                    SitePulseOrange
                }
        )

        Spacer(
            modifier = Modifier.width(5.dp)
        )

        Text(
            text = locationText,
            modifier = Modifier.weight(1f),
            fontSize = 11.sp,
            color =
                if (hasLocation) {
                    Color(0xFF444444)
                } else {
                    TextGray
                },
            maxLines = 2
        )

        if (isLoading) {

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = SitePulseOrange
            )

        } else {

            Text(
                text =
                    if (hasLocation) {
                        "Refresh"
                    } else {
                        "Enable"
                    },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SitePulseOrange
            )
        }
    }
}


private fun getCurrentDeviceLocation(
    context: Context,
    onSuccess: (Location) -> Unit,
    onError: (String) -> Unit
) {
    val fineGranted =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    val coarseGranted =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    if (!fineGranted && !coarseGranted) {
        onError("Location permission not granted")
        return
    }

    val locationManager =
        context.getSystemService(
            Context.LOCATION_SERVICE
        ) as? LocationManager

    if (locationManager == null) {
        onError("Location service unavailable")
        return
    }

    val enabledProviders = locationManager.getProviders(true)
    if (enabledProviders.isEmpty()) {
        onError("Please enable location services in device settings.")
        return
    }

    try {
        // Step 1: Scan all enabled providers for the freshest cached fix
        var freshestLastKnown: Location? = null
        for (p in enabledProviders) {
            val loc = try { locationManager.getLastKnownLocation(p) } catch (_: Exception) { null } ?: continue
            if (freshestLastKnown == null || loc.time > freshestLastKnown.time) {
                freshestLastKnown = loc
            }
        }

        val now = System.currentTimeMillis()
        val isFreshEnough = freshestLastKnown != null && (now - freshestLastKnown.time) < 10 * 60 * 1000L // 10 mins

        // If we have a very fresh fix, deliver immediately
        if (isFreshEnough && freshestLastKnown != null) {
            onSuccess(freshestLastKnown)
            return
        }

        // Step 2: Request fresh updates from all active providers
        var delivered = false
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (!delivered) {
                    delivered = true
                    try { locationManager.removeUpdates(this) } catch (_: Exception) {}
                    onSuccess(location)
                }
            }

            override fun onProviderDisabled(provider: String) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            @Deprecated("Deprecated in Android")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }

        // Prefer NETWORK_PROVIDER for fast indoor fix, then GPS_PROVIDER
        val providersToRequest = mutableListOf<String>()
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            providersToRequest.add(LocationManager.NETWORK_PROVIDER)
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            providersToRequest.add(LocationManager.GPS_PROVIDER)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
            locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER)) {
            providersToRequest.add(LocationManager.FUSED_PROVIDER)
        }

        if (providersToRequest.isEmpty()) {
            if (freshestLastKnown != null) {
                onSuccess(freshestLastKnown)
            } else {
                onError("No location provider available")
            }
            return
        }

        for (p in providersToRequest) {
            try {
                locationManager.requestLocationUpdates(
                    p,
                    0L,
                    0f,
                    listener,
                    android.os.Looper.getMainLooper()
                )
            } catch (_: Exception) {}
        }

        // Safety fallback timer: if no live fix arrives within 4 seconds, deliver freshestLastKnown
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!delivered) {
                delivered = true
                try { locationManager.removeUpdates(listener) } catch (_: Exception) {}
                if (freshestLastKnown != null) {
                    onSuccess(freshestLastKnown)
                } else {
                    onError("Location timeout. Please check your GPS signal.")
                }
            }
        }, 4000L)

    } catch (_: SecurityException) {
        onError("Location permission denied")
    } catch (e: Exception) {
        onError(e.message ?: "Unable to acquire location")
    }
}

private suspend fun resolveLocationLabel(
    context: Context,
    location: Location
): String {
    return withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) {
                return@withContext formatCoordinates(location)
            }

            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            val address = addresses?.firstOrNull()

            if (address == null) {
                val knownTown = when {
                    location.latitude in 9.72..9.84 && location.longitude in 123.45..123.56 -> "Dalaguete, Cebu"
                    location.latitude in 9.85..9.93 && location.longitude in 123.55..123.64 -> "Argao, Cebu"
                    location.latitude in 9.60..9.72 && location.longitude in 123.45..123.55 -> "Alcoy, Cebu"
                    location.latitude in 9.94..10.05 && location.longitude in 123.58..123.65 -> "Sibonga, Cebu"
                    else -> formatCoordinates(location)
                }
                return@withContext knownTown
            }

            buildString {
                // 1. Barangay or Sublocality
                address.subLocality
                    ?.takeIf { it.isNotBlank() }
                    ?.let { append(it) }

                // 2. Municipality or City
                val townOrCity = address.locality
                    ?: address.subAdminArea
                    ?: when {
                        address.postalCode == "6022" || (location.latitude in 9.72..9.84 && location.longitude in 123.45..123.56) -> "Dalaguete"
                        address.postalCode == "6021" || (location.latitude in 9.85..9.93 && location.longitude in 123.55..123.64) -> "Argao"
                        address.postalCode == "6023" -> "Alcoy"
                        address.postalCode == "6037" -> "Sibonga"
                        else -> null
                    }

                townOrCity
                    ?.takeIf { it.isNotBlank() && !contains(it) }
                    ?.let {
                        if (isNotEmpty()) append(", ")
                        append(it)
                    }

                // 3. Province or Admin Area
                val province = address.adminArea ?: "Cebu"
                if (province.isNotBlank() && !contains(province)) {
                    if (isNotEmpty()) append(", ")
                    append(province)
                }

                if (isEmpty()) {
                    append(address.getAddressLine(0) ?: formatCoordinates(location))
                }
            }
        } catch (_: Exception) {
            when {
                location.latitude in 9.72..9.84 && location.longitude in 123.45..123.56 -> "Dalaguete, Cebu"
                location.latitude in 9.85..9.93 && location.longitude in 123.55..123.64 -> "Argao, Cebu"
                else -> formatCoordinates(location)
            }
        }
    }
}


private fun formatCoordinates(
    location: Location
): String {

    return String.format(
        Locale.US,
        "%.6f, %.6f",
        location.latitude,
        location.longitude
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
private fun QuickActions(
    isLoggingTime: Boolean = false,
    onLogTimeClick: () -> Unit = {},
    onCaptureClick: () -> Unit = {},
    onReportClick: () -> Unit = {}
) {

    Row(

        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.spacedBy(10.dp)

    ) {

        QuickActionButton(
            title =
                if (isLoggingTime) {
                    "Logging..."
                } else {
                    "Log Time"
                },
            icon = Icons.Outlined.Schedule,
            modifier = Modifier.weight(1f),
            enabled = !isLoggingTime,
            onClick = onLogTimeClick
        )

        QuickActionButton(
            title = "Capture",
            icon = Icons.Outlined.CameraAlt,
            modifier = Modifier.weight(1f),
            onClick = onCaptureClick
        )

        QuickActionButton(
            title = "Report",
            icon = Icons.Outlined.PriorityHigh,
            modifier = Modifier.weight(1f),
            onClick = onReportClick
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

    modifier: Modifier = Modifier,

    enabled: Boolean = true,

    onClick: () -> Unit

) {

    Card(

        modifier =
            modifier
                .height(48.dp)
                .clickable(
                    enabled = enabled,
                    onClick = onClick
                ),

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