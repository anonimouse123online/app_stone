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
import com.example.capstonesample.data.model.workStatusLabel
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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.capstonesample.ui.theme.*

import com.example.capstonesample.data.api.RetrofitClient
import com.example.capstonesample.data.api.TaskResponse
import com.example.capstonesample.data.api.TimeLogCreateRequest
import com.example.capstonesample.data.local.AppDatabase
import com.example.capstonesample.data.local.toTaskResponse
import com.example.capstonesample.data.local.toTaskEntity
import com.example.capstonesample.sync.OfflineSyncManager
import com.example.capstonesample.network.NetworkMonitor
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
    val status: String,
    val code: String? = null,
    val geofenceLatitude: Double? = null,
    val geofenceLongitude: Double? = null,
    val geofenceRadius: Double? = null,
    val geofenceEnabled: Boolean? = null
)


data class DashboardLocationUi(
    val latitude: Double,
    val longitude: Double,
    val label: String
)




data class DashboardFieldActivityUi(
    val id: String? = null,
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

private val DashboardBackground = Color(0xFFF6F8FA)
private val SitePulseOrange = Color(0xFFF15A24)
private val OrangeLight = Color(0xFFFFF7ED)
private val TextGray = Color(0xFF64748B)
private val Green = Color(0xFF10B981)
private val Blue = Color(0xFF3B82F6)


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

    var isOfflineDashboard by remember {
        mutableStateOf(false)
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

    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360

    val activeShift = remember { mutableStateOf(TimeTrackingManager.getActiveShift(context)) }
    var isTimedIn by remember { mutableStateOf(activeShift.value.isTimedIn) }
    var elapsedDuration by remember {
        val initialDiff = (System.currentTimeMillis() - activeShift.value.timeInMs).coerceAtLeast(0L)
        mutableStateOf(if (activeShift.value.isTimedIn) TimeTrackingManager.formatDuration(initialDiff) else "00h 00m 00s")
    }
    var showTimeInDialog by remember { mutableStateOf(false) }
    var showLogTimeDialog by remember { mutableStateOf(false) }


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

    LaunchedEffect(isTimedIn, activeShift.value.timeInMs) {
        var tick = 0
        while (isTimedIn && activeShift.value.timeInMs > 0L) {
            val now = System.currentTimeMillis()
            elapsedDuration = TimeTrackingManager.formatDuration(now - activeShift.value.timeInMs)
            tick++
            if (tick % 30 == 0) {
                requestLocation()
            }
            kotlinx.coroutines.delay(1000L)
        }
    }

    LaunchedEffect(currentLocation, isTimedIn) {
        val currLoc = currentLocation
        if (isTimedIn && currLoc != null) {
            val projName = activeShift.value.projectName
            val matchingProj = projects.firstOrNull { it.name.equals(projName.trim(), ignoreCase = true) }
            if (matchingProj?.geofenceEnabled == true && matchingProj.geofenceLatitude != null && matchingProj.geofenceLongitude != null) {
                val check = com.example.capstonesample.security.GeofenceHelper.evaluateGeofence(
                    userLat = currLoc.latitude,
                    userLng = currLoc.longitude,
                    fenceLat = matchingProj.geofenceLatitude,
                    fenceLng = matchingProj.geofenceLongitude,
                    radiusMeters = matchingProj.geofenceRadius,
                    enabled = matchingProj.geofenceEnabled
                )
                val updatedShift = TimeTrackingManager.recordGeofenceEvent(context, check.isInside)
                activeShift.value = updatedShift
            }
        }
    }


    fun submitTimeLog(
        selectedProject: String = projects.firstOrNull()?.name ?: "General Project",
        timeIn: String? = null,
        timeOut: String? = null,
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

        val location = currentLocation
        val matchingProject = projects.firstOrNull { it.name.equals(trimmedProject, ignoreCase = true) }
        val geofenceCheck = if (matchingProject?.geofenceEnabled == true && matchingProject.geofenceLatitude != null && matchingProject.geofenceLongitude != null) {
            com.example.capstonesample.security.GeofenceHelper.evaluateGeofence(
                userLat = location?.latitude,
                userLng = location?.longitude,
                fenceLat = matchingProject.geofenceLatitude,
                fenceLng = matchingProject.geofenceLongitude,
                radiusMeters = matchingProject.geofenceRadius,
                enabled = matchingProject.geofenceEnabled
            )
        } else null

        // Enforce strict geofence boundary: cannot Time Out outside the zone
        if (matchingProject?.geofenceEnabled == true) {
            if (location == null) {
                Toast.makeText(context, "Cannot Time Out: GPS location required to verify site geofence.", Toast.LENGTH_LONG).show()
                return
            }
            if (geofenceCheck?.isInside != true) {
                val dist = geofenceCheck?.distanceMeters?.toInt() ?: 0
                val rad = matchingProject.geofenceRadius?.toInt() ?: 200
                Toast.makeText(context, "Cannot Time Out: Outside site geofence (${dist}m from center, limit: ${rad}m).", Toast.LENGTH_LONG).show()
                return
            }
        }

        val geofenceTag = when {
            geofenceCheck == null -> ""
            geofenceCheck.isInside -> " • [Geofence: VERIFIED ON-SITE (${geofenceCheck.distanceMeters}m)]"
            else -> " • [Geofence: OUTSIDE SITE PERIMETER (${geofenceCheck.distanceMeters}m)]"
        }

        val locationNote = if (location != null) {
            "Location: ${location.label} (${location.latitude}, ${location.longitude})$geofenceTag"
        } else {
            "Location: Field / Mobile App$geofenceTag"
        }

        val timeTag = if (!timeIn.isNullOrBlank() && !timeOut.isNullOrBlank()) {
            "[Time In: ${timeIn.trim()}] • [Time Out: ${timeOut.trim()}]"
        } else if (!timeIn.isNullOrBlank()) {
            "[Time In: ${timeIn.trim()}]"
        } else ""

        val geofenceAlertTag = TimeTrackingManager.getGeofenceAlertTag(context) ?: ""
        val durationTag = if (hours.isNotBlank()) "[Duration: ${hours.trim()}]" else ""

        val finalNotes = listOf(timeTag, durationTag, geofenceAlertTag, additionalNotes.trim(), locationNote)
            .filter { it.isNotBlank() }
            .joinToString(" • ")

        val parsedHours = hours.trim().ifBlank { "8" }
        val tempToSend = temperature.trim().ifBlank { null }
        val parsedWorkOnSite = workOnSite.trim().toIntOrNull() ?: 1
        val parsedSupervisors = supervisors.trim().toIntOrNull() ?: 1
        val parsedSubContractors = subContractors.trim().toIntOrNull() ?: 0

        val logRequest = TimeLogCreateRequest(
            projectName = trimmedProject,
            engineerName = resolvedUserName,
            date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
            timeIn = timeIn?.trim(),
            timeOut = timeOut?.trim(),
            workOnSite = parsedWorkOnSite,
            supervisors = parsedSupervisors,
            subContractors = parsedSubContractors,
            totalWorkHours = parsedHours,
            weather = weather.trim().ifBlank { "Sunny" },
            temperature = tempToSend,
            workCompleted = if (workCompleted.isNotBlank()) workCompleted.trim() else "Daily site supervision and engineering tasks.",
            materialsDelivered = materialsDelivered.trim(),
            equipmentUsed = equipmentUsed.trim(),
            additionalNotes = finalNotes
        )

        coroutineScope.launch {
            isLoggingTime = true
            logTimeMessage = null

            val authorizationToken =
                if (token.startsWith("Bearer ")) {
                    token
                } else {
                    "Bearer $token"
                }

            try {

                val isDeviceOffline = isOfflineDashboard || !NetworkMonitor.isInternetAvailable(context)
                if (isDeviceOffline) {
                    val offlineRequest = logRequest.copy(
                        additionalNotes = "${logRequest.additionalNotes ?: ""} • [Offline Record: Queued for Sync]"
                    )
                    com.example.capstonesample.sync.OfflineSyncManager.queueTimeLog(context, authorizationToken, offlineRequest)
                    val offlineMsg = "Offline mode: Log saved locally ($parsedHours hrs for $trimmedProject). It will sync automatically when back online."
                    logTimeMessage = offlineMsg
                    Toast.makeText(context, "💾 $offlineMsg", Toast.LENGTH_LONG).show()
                    onSuccess()
                    return@launch
                }

                val response = RetrofitClient.api.createTimeLog(
                    token = authorizationToken,
                    request = logRequest
                )

                val isSuccess = response.isSuccessful && (response.body()?.success == true || response.code() in 200..299)
                if (isSuccess) {
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
                try {
                    val offlineRequest = logRequest.copy(
                        additionalNotes = "${logRequest.additionalNotes ?: ""} • [Offline Record: Queued for Sync]"
                    )
                    com.example.capstonesample.sync.OfflineSyncManager.queueTimeLog(context, authorizationToken, offlineRequest)
                    val offlineMsg = "Network unavailable: Log queued locally ($parsedHours hrs for $trimmedProject). Will sync when online."
                    logTimeMessage = offlineMsg
                    Toast.makeText(context, "💾 $offlineMsg", Toast.LENGTH_LONG).show()
                    onSuccess()
                } catch (_: Exception) {
                    val errorMsg = e.message ?: "Unable to connect to SitePulse server."
                    logTimeMessage = errorMsg
                    Toast.makeText(context, "❌ $errorMsg", Toast.LENGTH_LONG).show()
                }
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
                                        ?: "Active",

                                code = project.code,
                                geofenceLatitude = project.geofenceLatitude,
                                geofenceLongitude = project.geofenceLongitude,
                                geofenceRadius = project.geofenceRadius,
                                geofenceEnabled = project.geofenceEnabled
                            )
                        }
                    OfflineSyncManager.cacheProjects(context, body.data)
                    NavBadgeState.ongoingProjectsCount = projects.count { NavBadgeState.isOngoingProject(it.status) }

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

                    try {
                        val db = AppDatabase.getDatabase(context)
                        val entities = tasks.map { it.toTaskEntity() }
                        db.taskDao().deleteAllTasks()
                        db.taskDao().insertTasks(entities)
                    } catch (_: Exception) {}

                    val currentUid = NavBadgeState.getUserIdFromJwt(authorizationToken)
                    val myTasks = if (!currentUid.isNullOrBlank()) {
                        tasks.filter { it.assigneeId.isNullOrBlank() || it.assigneeId.equals(currentUid, ignoreCase = true) }
                    } else {
                        tasks
                    }
                    NavBadgeState.ongoingTasksCount = myTasks.count {
                        NavBadgeState.isOngoingTask(it.status, it.dueDate, it.progress, it.subtasks)
                    }
                    NavBadgeState.refresh(context, authorizationToken)

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

            val isNetError = !NetworkMonitor.isInternetAvailable(context) ||
                    e is java.net.ConnectException ||
                    e is java.net.UnknownHostException ||
                    e is java.net.SocketTimeoutException ||
                    e.message?.contains("Failed to connect", ignoreCase = true) == true

            if (isNetError) {
                // Restore cached projects
                val cachedProjects = OfflineSyncManager.getCachedProjects(context)
                if (cachedProjects.isNotEmpty()) {
                    projects = cachedProjects.map { project ->
                        DashboardProjectUi(
                            name = project.name ?: "Unnamed Project",
                            type = "PROJECT",
                            progress = (project.progress ?: 0.0).toInt().coerceIn(0, 100),
                            status = project.status ?: "Active",
                            code = project.code,
                            geofenceLatitude = project.geofenceLatitude,
                            geofenceLongitude = project.geofenceLongitude,
                            geofenceRadius = project.geofenceRadius,
                            geofenceEnabled = project.geofenceEnabled
                        )
                    }
                    NavBadgeState.ongoingProjectsCount = projects.count { NavBadgeState.isOngoingProject(it.status) }
                }

                // Restore cached tasks from Room
                try {
                    val db = AppDatabase.getDatabase(context)
                    val cachedTasks = db.taskDao().getAllTasksList()
                    if (cachedTasks.isNotEmpty()) {
                        tasks = cachedTasks.map { it.toTaskResponse() }
                    }
                } catch (_: Exception) {}

                if (projects.isNotEmpty() || tasks.isNotEmpty()) {
                    isOfflineDashboard = true
                    errorMessage = null
                } else {
                    isOfflineDashboard = true
                    errorMessage = "Offline Mode: No cached projects or tasks available. Connect once to synchronize."
                }
            } else {
                errorMessage =
                    e.message
                        ?: "Unable to connect to SitePulse server."
            }

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

                    id =
                        task.id,

                    title =
                        task.title,

                    description =
                        buildString {

                            append(
                                workStatusLabel(task.status, task.dueDate)
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
                            workStatusLabel(task.status, task.dueDate)
                                .uppercase()
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
            val label = workStatusLabel(task.status, task.dueDate)
            label.equals("Delayed", ignoreCase = true) ||
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
                    ) ||
                    project.status.equals(
                        "Ongoing",
                        ignoreCase = true
                    ) ||
                    project.status.equals(
                        "Planning",
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
                },
                onProfileClick = onProfileClick
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
            // OFFLINE BANNER / ERROR
            // =================================================

            if (isOfflineDashboard) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF1F5F9)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📡", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Offline Mode Active",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    "Showing cached projects and tasks. Field time logs can be submitted offline and will automatically sync when reconnected.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            if (!isOfflineDashboard) {
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
                        text = "FIELD OPERATIONS COMMAND",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SitePulseOrange,
                        letterSpacing = 0.8.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (isLoading) "Loading..." else userName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSlate900
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (userRole.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .background(IndigoBlueBg, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = userRole.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoBlueText
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = currentDate,
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    DashboardLocationRow(
                        locationText = if (liveWeather != null) "$locationMessage • ${liveWeather?.formattedTemp} ${liveWeather?.weather}" else locationMessage,
                        isLoading = isLocating,
                        hasLocation = currentLocation != null,
                        onClick = {
                            requestLocation()
                        }
                    )

                    val activeGeofenceProject = remember(projects) {
                        projects.firstOrNull { it.geofenceEnabled == true && it.geofenceLatitude != null && it.geofenceLongitude != null }
                    }

                    if (activeGeofenceProject != null) {
                        val gfResult = remember(currentLocation, activeGeofenceProject) {
                            com.example.capstonesample.security.GeofenceHelper.evaluateGeofence(
                                userLat = currentLocation?.latitude,
                                userLng = currentLocation?.longitude,
                                fenceLat = activeGeofenceProject.geofenceLatitude,
                                fenceLng = activeGeofenceProject.geofenceLongitude,
                                radiusMeters = activeGeofenceProject.geofenceRadius,
                                enabled = activeGeofenceProject.geofenceEnabled
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (gfResult.isInside) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, if (gfResult.isInside) Color(0xFFA7F3D0) else Color(0xFFFECACA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (gfResult.isInside) Color(0xFF10B981) else Color(0xFFEF4444))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (gfResult.isInside) "GEOFENCE VERIFIED: ${gfResult.message}" else "GEOFENCE ALERT: ${gfResult.message}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (gfResult.isInside) Color(0xFF065F46) else Color(0xFF991B1B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

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
                        totalTasks,

                    onClick =
                        onTasksClick
                )
            }


            // =================================================
            // QUICK ACTIONS
            // =================================================

            item {
                QuickActions(
                    isTimedIn = isTimedIn,
                    activeShift = activeShift.value,
                    elapsedDuration = elapsedDuration,
                    isLoggingTime = isLoggingTime,
                    onTimeInClick = {
                        if (currentLocation == null) {
                            requestLocation()
                        }
                        showTimeInDialog = true
                    },
                    onTimeOutClick = {
                        if (currentLocation == null) {
                            requestLocation()
                        }
                        val matchingProj = projects.firstOrNull { it.name.equals(activeShift.value.projectName.trim(), ignoreCase = true) }
                        if (matchingProj?.geofenceEnabled == true && matchingProj.geofenceLatitude != null && matchingProj.geofenceLongitude != null && currentLocation != null) {
                            val check = com.example.capstonesample.security.GeofenceHelper.evaluateGeofence(
                                userLat = currentLocation?.latitude,
                                userLng = currentLocation?.longitude,
                                fenceLat = matchingProj.geofenceLatitude,
                                fenceLng = matchingProj.geofenceLongitude,
                                radiusMeters = matchingProj.geofenceRadius,
                                enabled = matchingProj.geofenceEnabled
                            )
                            if (!check.isInside) {
                                val dist = check.distanceMeters?.toInt() ?: 0
                                val rad = matchingProj.geofenceRadius?.toInt() ?: 200
                                Toast.makeText(context, "Cannot Time Out: Outside site geofence (${dist}m from center, limit: ${rad}m). Please return to site to Time Out.", Toast.LENGTH_LONG).show()
                                return@QuickActions
                            }
                        }
                        showLogTimeDialog = true
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

            } else if (isSmallScreen) {

                items(projects.take(2), key = { "proj-${it.name}" }) { project ->
                    ProjectCard(
                        project = project,
                        modifier = Modifier.fillMaxWidth()
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

                itemsIndexed(
                    items = activities,
                    key = { index, activity ->
                        "${activity.id ?: "activity"}-$index"
                    }
                ) { _, activity ->

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

    if (showTimeInDialog) {
        TimeInDialog(
            projectsList = projects,
            initialEngineerName = resolvedUserName,
            currentLocation = currentLocation,
            isLocating = isLocating,
            onRequestLocation = { requestLocation() },
            onDismiss = { showTimeInDialog = false },
            onConfirmTimeIn = { chosenProject ->
                val proj = projects.firstOrNull { it.name.equals(chosenProject.trim(), ignoreCase = true) }
                if (proj?.geofenceEnabled == true && proj.geofenceLatitude != null && proj.geofenceLongitude != null) {
                    val check = com.example.capstonesample.security.GeofenceHelper.evaluateGeofence(
                        userLat = currentLocation?.latitude,
                        userLng = currentLocation?.longitude,
                        fenceLat = proj.geofenceLatitude,
                        fenceLng = proj.geofenceLongitude,
                        radiusMeters = proj.geofenceRadius,
                        enabled = proj.geofenceEnabled
                    )
                    if (!check.isInside) {
                        val dist = check.distanceMeters?.toInt() ?: 0
                        val rad = proj.geofenceRadius?.toInt() ?: 200
                        Toast.makeText(
                            context,
                            "Cannot Time In: Outside site perimeter (${dist}m from center, limit: ${rad}m). Please move on-site to Time In.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@TimeInDialog
                    }
                }

                val newShift = TimeTrackingManager.setTimeIn(context, chosenProject)
                activeShift.value = newShift
                isTimedIn = true
                showTimeInDialog = false
                Toast.makeText(context, "Timed In at ${newShift.timeInFormatted} for $chosenProject", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showLogTimeDialog) {
        LogTimeDialog(
            projectsList = projects,
            activeShift = activeShift.value,
            initialEngineerName = resolvedUserName,
            currentLocation = currentLocation,
            locationMessage = locationMessage,
            isLocating = isLocating,
            isSubmitting = isLoggingTime,
            isOffline = isOfflineDashboard || !NetworkMonitor.isInternetAvailable(context),
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
                    timeIn = form.timeIn,
                    timeOut = form.timeOut,
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
                        TimeTrackingManager.clearShift(context)
                        activeShift.value = TimeTrackingManager.getActiveShift(context)
                        isTimedIn = false
                        showLogTimeDialog = false
                    }
                )
            }
        )
    }
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
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit = {}
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
                        .clip(CircleShape)
                        .background(
                            Color(0xFF263238),
                            CircleShape
                        )
                        .clickable { onProfileClick() },

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
                                TextGray,

                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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

                    itemsIndexed(
                        items =
                            notifications,

                        key = { index, notification ->
                            "${notification.id.ifEmpty { "notification" }}-$index"
                        }
                    ) { _, notification ->

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
                                true,

                            audience =
                                notification.audience,

                            projectName =
                                notification.projectName ?: notification.projectId,

                            senderName =
                                notification.senderName
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// TIME TRACKING MANAGER (TIME IN / TIME OUT PERSISTENCE & TIMER)
// ============================================================

object TimeTrackingManager {
    private const val PREFS_NAME = "sitepulse_time_tracking"
    private const val KEY_IS_TIMED_IN = "is_timed_in"
    private const val KEY_TIME_IN_MS = "time_in_ms"
    private const val KEY_TIME_IN_FORMATTED = "time_in_formatted"
    private const val KEY_TIME_IN_PROJECT = "time_in_project"
    private const val KEY_TIME_IN_DATE = "time_in_date"
    private const val KEY_GEOFENCE_LEFT_TIME = "geofence_left_time"
    private const val KEY_GEOFENCE_RETURNED_TIME = "geofence_returned_time"
    private const val KEY_CURRENTLY_OUTSIDE_GEOFENCE = "currently_outside_geofence"
    private const val KEY_HAS_GEOFENCE_ALERT = "has_geofence_alert"
    private const val KEY_GEOFENCE_HISTORY = "geofence_events_history"

    data class ActiveShift(
        val isTimedIn: Boolean = false,
        val timeInMs: Long = 0L,
        val timeInFormatted: String = "",
        val projectName: String = "",
        val date: String = "",
        val geofenceLeftTime: String = "",
        val geofenceReturnedTime: String = "",
        val currentlyOutsideGeofence: Boolean = false,
        val hasGeofenceAlert: Boolean = false,
        val incidentsCount: Int = 0
    )

    fun getActiveShift(context: Context): ActiveShift {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isTimedIn = prefs.getBoolean(KEY_IS_TIMED_IN, false)
        val timeInMs = prefs.getLong(KEY_TIME_IN_MS, 0L)
        val formatted = prefs.getString(KEY_TIME_IN_FORMATTED, "") ?: ""
        val project = prefs.getString(KEY_TIME_IN_PROJECT, "") ?: ""
        val date = prefs.getString(KEY_TIME_IN_DATE, "") ?: ""
        val leftTime = prefs.getString(KEY_GEOFENCE_LEFT_TIME, "") ?: ""
        val returnedTime = prefs.getString(KEY_GEOFENCE_RETURNED_TIME, "") ?: ""
        val isOutside = prefs.getBoolean(KEY_CURRENTLY_OUTSIDE_GEOFENCE, false)
        val hasAlert = prefs.getBoolean(KEY_HAS_GEOFENCE_ALERT, false)
        val history = prefs.getString(KEY_GEOFENCE_HISTORY, "") ?: ""
        val historyCount = if (history.isBlank()) 0 else history.split(";;").filter { it.isNotBlank() }.size
        val totalIncidents = historyCount + (if (isOutside) 1 else 0)
        return ActiveShift(isTimedIn, timeInMs, formatted, project, date, leftTime, returnedTime, isOutside, hasAlert, totalIncidents)
    }

    fun setTimeIn(context: Context, projectName: String, timeInMs: Long = System.currentTimeMillis()): ActiveShift {
        val formattedTime = SimpleDateFormat("hh:mm a", Locale.US).format(Date(timeInMs))
        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timeInMs))
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_IS_TIMED_IN, true)
            .putLong(KEY_TIME_IN_MS, timeInMs)
            .putString(KEY_TIME_IN_FORMATTED, formattedTime)
            .putString(KEY_TIME_IN_PROJECT, projectName)
            .putString(KEY_TIME_IN_DATE, formattedDate)
            .remove(KEY_GEOFENCE_LEFT_TIME)
            .remove(KEY_GEOFENCE_RETURNED_TIME)
            .remove(KEY_GEOFENCE_HISTORY)
            .putBoolean(KEY_CURRENTLY_OUTSIDE_GEOFENCE, false)
            .putBoolean(KEY_HAS_GEOFENCE_ALERT, false)
            .apply()
        return ActiveShift(true, timeInMs, formattedTime, projectName, formattedDate, "", "", false, false, 0)
    }

    fun clearShift(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_IS_TIMED_IN, false)
            .putLong(KEY_TIME_IN_MS, 0L)
            .putString(KEY_TIME_IN_FORMATTED, "")
            .putString(KEY_TIME_IN_PROJECT, "")
            .putString(KEY_TIME_IN_DATE, "")
            .remove(KEY_GEOFENCE_LEFT_TIME)
            .remove(KEY_GEOFENCE_RETURNED_TIME)
            .remove(KEY_GEOFENCE_HISTORY)
            .putBoolean(KEY_CURRENTLY_OUTSIDE_GEOFENCE, false)
            .putBoolean(KEY_HAS_GEOFENCE_ALERT, false)
            .apply()
    }

    fun recordGeofenceEvent(
        context: Context,
        isInside: Boolean,
        timestampMs: Long = System.currentTimeMillis()
    ): ActiveShift {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isTimedIn = prefs.getBoolean(KEY_IS_TIMED_IN, false)
        if (!isTimedIn) return getActiveShift(context)

        val currentlyOutside = prefs.getBoolean(KEY_CURRENTLY_OUTSIDE_GEOFENCE, false)
        val editor = prefs.edit()
        val timeFormatted = SimpleDateFormat("hh:mm a", Locale.US).format(Date(timestampMs))

        if (!isInside && !currentlyOutside) {
            // Engineer just stepped outside the geofenced site perimeter!
            editor.putBoolean(KEY_CURRENTLY_OUTSIDE_GEOFENCE, true)
            editor.putBoolean(KEY_HAS_GEOFENCE_ALERT, true)
            editor.putString(KEY_GEOFENCE_LEFT_TIME, timeFormatted)
            editor.putString(KEY_GEOFENCE_RETURNED_TIME, "")
            editor.apply()
        } else if (isInside && currentlyOutside) {
            // Engineer returned back to the geofenced perimeter!
            val leftTime = prefs.getString(KEY_GEOFENCE_LEFT_TIME, "") ?: timeFormatted
            val existingHistory = prefs.getString(KEY_GEOFENCE_HISTORY, "") ?: ""
            val newEntry = "$leftTime|$timeFormatted"
            val updatedHistory = if (existingHistory.isBlank()) newEntry else "$existingHistory;;$newEntry"

            editor.putBoolean(KEY_CURRENTLY_OUTSIDE_GEOFENCE, false)
            editor.putString(KEY_GEOFENCE_RETURNED_TIME, timeFormatted)
            editor.putString(KEY_GEOFENCE_HISTORY, updatedHistory)
            editor.apply()
        }

        return getActiveShift(context)
    }

    fun getGeofenceAlertTag(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasAlert = prefs.getBoolean(KEY_HAS_GEOFENCE_ALERT, false)
        if (!hasAlert) return null

        val history = prefs.getString(KEY_GEOFENCE_HISTORY, "") ?: ""
        val isStillOutside = prefs.getBoolean(KEY_CURRENTLY_OUTSIDE_GEOFENCE, false)
        val currentLeftTime = prefs.getString(KEY_GEOFENCE_LEFT_TIME, "") ?: ""
        val currentReturnedTime = prefs.getString(KEY_GEOFENCE_RETURNED_TIME, "") ?: ""

        val incidentTags = mutableListOf<String>()

        if (history.isNotBlank()) {
            val entries = history.split(";;").filter { it.isNotBlank() }
            for (entry in entries) {
                val parts = entry.split("|")
                if (parts.size >= 2) {
                    incidentTags.add("[Geofence Alert: Left at ${parts[0]} • Returned at ${parts[1]}]")
                } else if (parts.size == 1) {
                    incidentTags.add("[Geofence Alert: Left at ${parts[0]} • Returned at site]")
                }
            }
        }

        if (isStillOutside && currentLeftTime.isNotBlank()) {
            incidentTags.add("[Geofence Alert: Left at $currentLeftTime • Currently Outside Perimeter]")
        } else if (incidentTags.isEmpty() && currentLeftTime.isNotBlank()) {
            if (currentReturnedTime.isNotBlank()) {
                incidentTags.add("[Geofence Alert: Left at $currentLeftTime • Returned at $currentReturnedTime]")
            } else {
                incidentTags.add("[Geofence Alert: Left at $currentLeftTime • Returned at site]")
            }
        }

        return if (incidentTags.isNotEmpty()) {
            incidentTags.joinToString(" • ")
        } else null
    }

    fun calculateWorkHours(timeInMs: Long, timeOutMs: Long = System.currentTimeMillis()): Pair<Double, String> {
        val diffMs = (timeOutMs - timeInMs).coerceAtLeast(0L)
        val totalSecs = (diffMs / 1000).coerceAtLeast(0L)
        val hours = totalSecs / 3600
        val minutes = (totalSecs % 3600) / 60
        val seconds = totalSecs % 60
        val formatted = String.format(Locale.US, "%02dh %02dm %02ds", hours, minutes, seconds)
        val decimalHours = totalSecs.toDouble() / 3600.0
        return Pair(decimalHours, formatted)
    }

    fun formatDuration(diffMs: Long): String {
        val totalSecs = (diffMs / 1000).coerceAtLeast(0L)
        val hours = totalSecs / 3600
        val minutes = (totalSecs % 3600) / 60
        val seconds = totalSecs % 60
        return String.format(Locale.US, "%02dh %02dm %02ds", hours, minutes, seconds)
    }
}

// ============================================================
// LOG TIME FORM DATA
// ============================================================

data class LogTimeFormData(
    val projectName: String,
    val timeIn: String = "",
    val timeOut: String = "",
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
// MANPOWER COUNTER COMPONENT
// ============================================================

@Composable
private fun ManpowerCounter(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val count = value.toIntOrNull() ?: 0
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.5.dp, Color(0xFF94A3B8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (count > 0 && enabled) Color(0xFFE2E8F0) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(enabled = count > 0 && enabled) {
                            onValueChange((count - 1).coerceAtLeast(0).toString())
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "−",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (count > 0 && enabled) Color(0xFF0F172A) else Color(0xFF94A3B8)
                        )
                    }
                }

                Text(
                    text = value.ifBlank { "0" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (enabled) OrangeLight else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (enabled) SitePulseOrange else Color(0xFF94A3B8)),
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(enabled = enabled) {
                            onValueChange((count + 1).toString())
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "+",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (enabled) SitePulseOrange else Color(0xFF94A3B8)
                        )
                    }
                }
            }

        }
    }
}

// ============================================================
// TIME IN DIALOG
// ============================================================

@Composable
private fun TimeInDialog(
    projectsList: List<DashboardProjectUi> = emptyList(),
    initialEngineerName: String,
    currentLocation: DashboardLocationUi?,
    isLocating: Boolean,
    onRequestLocation: () -> Unit,
    onDismiss: () -> Unit,
    onConfirmTimeIn: (projectName: String) -> Unit
) {
    val projects = remember(projectsList) { projectsList.map { it.name }.filter { it.isNotBlank() } }
    var selectedProject by remember { mutableStateOf(projects.firstOrNull() ?: "") }
    var customProject by remember { mutableStateOf("") }
    var isCustomProject by remember { mutableStateOf(projects.isEmpty()) }
    var projectMenuExpanded by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val currentTime = remember { SimpleDateFormat("hh:mm a", Locale.US).format(Date()) }

    val activeProject = if (isCustomProject) customProject.trim() else selectedProject.trim()
    val matchedProject = remember(activeProject, projectsList) {
        projectsList.firstOrNull { it.name.equals(activeProject.trim(), ignoreCase = true) }
    }
    val geofenceResult = remember(matchedProject, currentLocation) {
        if (matchedProject != null && matchedProject.geofenceEnabled == true && matchedProject.geofenceLatitude != null && matchedProject.geofenceLongitude != null) {
            com.example.capstonesample.security.GeofenceHelper.evaluateGeofence(
                userLat = currentLocation?.latitude,
                userLng = currentLocation?.longitude,
                fenceLat = matchedProject.geofenceLatitude,
                fenceLng = matchedProject.geofenceLongitude,
                radiusMeters = matchedProject.geofenceRadius,
                enabled = matchedProject.geofenceEnabled
            )
        } else null
    }
    val isGeofenceLocked = matchedProject?.geofenceEnabled == true && (currentLocation == null || geofenceResult?.isInside != true)

    LaunchedEffect(Unit) {
        if (currentLocation == null) {
            onRequestLocation()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFFDCFCE7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Login,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Site Time In",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Start daily site shift & active timer",
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Time Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "START TIME",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = currentTime,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF166534)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "ENGINEER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = initialEngineerName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A),
                                maxLines = 1
                            )
                        }
                    }
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
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
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
                                onClick = { projectMenuExpanded = !projectMenuExpanded },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.5.dp, Color(0xFF64748B))
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
                                        fontWeight = if (selectedProject.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (selectedProject.isNotBlank()) Color(0xFF0F172A) else Color(0xFF64748B),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (projectMenuExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                        contentDescription = "Expand",
                                        tint = Color(0xFF334155)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = projectMenuExpanded,
                                onDismissRequest = { projectMenuExpanded = false },
                                containerColor = Color.White,
                                shadowElevation = 8.dp,
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(Color.White, RoundedCornerShape(12.dp))
                            ) {
                                projects.forEach { proj ->
                                    val isSelected = proj == selectedProject
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = proj,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = if (isSelected) SitePulseOrange else Color(0xFF0F172A)
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Check,
                                                        contentDescription = "Selected",
                                                        tint = SitePulseOrange,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        },
                                        colors = MenuDefaults.itemColors(
                                            textColor = Color(0xFF0F172A)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isSelected) Color(0xFFFFF7ED) else Color.White),
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
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // LIVE GEOFENCING STATUS FOR SELECTED PROJECT
                    if (matchedProject?.geofenceEnabled == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val isInside = geofenceResult?.isInside == true
                        val distance = geofenceResult?.distanceMeters ?: 0f
                        val radius = matchedProject.geofenceRadius?.toInt() ?: 200

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentLocation == null) Color(0xFFFFFBEB)
                                else if (isInside) Color(0xFFF0FDF4)
                                else Color(0xFFFEF2F2)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (currentLocation == null) Color(0xFFFDE68A)
                                else if (isInside) Color(0xFFBBF7D0)
                                else Color(0xFFFECACA)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (currentLocation == null) Icons.Outlined.LocationSearching
                                    else if (isInside) Icons.Outlined.CheckCircle
                                    else Icons.Outlined.WarningAmber,
                                    contentDescription = null,
                                    tint = if (currentLocation == null) Color(0xFFD97706)
                                    else if (isInside) Color(0xFF16A34A)
                                    else Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (currentLocation == null) "GEOFENCE: WAITING FOR GPS"
                                        else if (isInside) "GEOFENCE VERIFIED (ON-SITE)"
                                        else "GEOFENCE ALERT (OUTSIDE PERIMETER)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentLocation == null) Color(0xFFB45309)
                                        else if (isInside) Color(0xFF15803D)
                                        else Color(0xFFB91C1C)
                                    )
                                    Text(
                                        text = if (currentLocation == null) "Acquiring GPS fix to verify presence within ${radius}m boundary"
                                        else if (isInside) "${String.format(Locale.US, "%.1f", distance)}m from site center • Perimeter: ${radius}m"
                                        else "${String.format(Locale.US, "%.1f", distance)}m from site center • Outside ${radius}m boundary. Time In locked until on-site.",
                                        fontSize = 10.sp,
                                        color = if (currentLocation == null) Color(0xFF92400E)
                                        else if (isInside) Color(0xFF166534)
                                        else Color(0xFF991B1B)
                                    )
                                }
                            }
                        }
                    }
                }

                if (localError != null) {
                    Text(
                        text = localError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val activeProj = if (isCustomProject) customProject.trim() else selectedProject.trim()
                            if (activeProj.isBlank()) {
                                localError = "Please select or enter a project name."
                                return@Button
                            }
                            if (isGeofenceLocked) {
                                val radius = matchedProject?.geofenceRadius?.toInt() ?: 200
                                val distStr = geofenceResult?.distanceMeters?.let { String.format(Locale.US, "%.1f", it) } ?: "unknown"
                                localError = "Cannot Time In: Outside project perimeter (${distStr}m from center, max ${radius}m). Please move on-site to start your shift."
                                return@Button
                            }
                            onConfirmTimeIn(activeProj)
                        },
                        enabled = !isGeofenceLocked,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isGeofenceLocked) Color(0xFFEF4444) else Color(0xFF16A34A),
                            disabledContainerColor = Color(0xFFFCA5A5)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isGeofenceLocked) Icons.Outlined.Lock else Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isGeofenceLocked) "Geofence Locked" else "Confirm Time In",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

        }
    }
}

// ============================================================
// LOG TIME DIALOG (TIME OUT & SITE LOG)
// ============================================================

@Composable
private fun LogTimeDialog(
    projectsList: List<DashboardProjectUi> = emptyList(),
    activeShift: TimeTrackingManager.ActiveShift,
    initialEngineerName: String,
    currentLocation: DashboardLocationUi?,
    locationMessage: String,
    isLocating: Boolean,
    isSubmitting: Boolean,
    isOffline: Boolean = false,
    initialLiveWeather: LiveWeatherInfo? = null,
    onRequestLocation: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (form: LogTimeFormData) -> Unit
) {
    val dialogScope = rememberCoroutineScope()
    val projects = remember(projectsList) { projectsList.map { it.name }.filter { it.isNotBlank() } }

    var liveWeather by remember {
        mutableStateOf<LiveWeatherInfo?>(initialLiveWeather)
    }
    var isFetchingWeather by remember {
        mutableStateOf(false)
    }

    var selectedProject by remember {
        mutableStateOf(activeShift.projectName.ifBlank { projects.firstOrNull() ?: "" })
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

    val timeOutMs = remember { System.currentTimeMillis() }
    val formattedTimeOut = remember(timeOutMs) {
        SimpleDateFormat("hh:mm a", Locale.US).format(Date(timeOutMs))
    }
    val effectiveTimeInMs = if (activeShift.timeInMs > 0L) activeShift.timeInMs else (timeOutMs - 8 * 3600 * 1000L)
    val effectiveTimeInFormatted = if (activeShift.timeInFormatted.isNotBlank()) activeShift.timeInFormatted else "08:00 AM"
    val (calculatedHoursVal, calculatedHoursDisplay) = remember(effectiveTimeInMs, timeOutMs) {
        TimeTrackingManager.calculateWorkHours(effectiveTimeInMs, timeOutMs)
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

    LaunchedEffect(projects) {
        if (selectedProject.isBlank() && projects.isNotEmpty()) {
            selectedProject = projects.first()
            if (isCustomProject && customProject.isBlank()) {
                isCustomProject = false
            }
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

    val matchedProject = remember(activeProjectName, projectsList) {
        projectsList.firstOrNull { it.name.equals(activeProjectName.trim(), ignoreCase = true) }
    }

    val geofenceResult = remember(matchedProject, currentLocation) {
        if (matchedProject != null && matchedProject.geofenceEnabled == true && matchedProject.geofenceLatitude != null && matchedProject.geofenceLongitude != null) {
            com.example.capstonesample.security.GeofenceHelper.evaluateGeofence(
                userLat = currentLocation?.latitude,
                userLng = currentLocation?.longitude,
                fenceLat = matchedProject.geofenceLatitude,
                fenceLng = matchedProject.geofenceLongitude,
                radiusMeters = matchedProject.geofenceRadius,
                enabled = matchedProject.geofenceEnabled
            )
        } else null
    }

    val outdoorFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color(0xFFF8FAFC),
        focusedBorderColor = SitePulseOrange,
        unfocusedBorderColor = Color(0xFF64748B),
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF0F172A),
        focusedPlaceholderColor = Color(0xFF64748B),
        unfocusedPlaceholderColor = Color(0xFF64748B),
        focusedLabelColor = SitePulseOrange,
        unfocusedLabelColor = Color(0xFF334155),
        cursorColor = SitePulseOrange
    )

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
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                            text = "Time Out & Site Log",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Black
                        )
                        Text(
                            text = "Record shift completion & engineering work",
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
                    // Offline Mode Banner (if offline)
                    if (isOffline) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudOff,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Offline Mode Active",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E40AF)
                                    )
                                    Text(
                                        text = "Log will be recorded to local device storage with timestamp and auto-synced when back online.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF3B82F6),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

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
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
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
                                    colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = BorderStroke(1.5.dp, Color(0xFF64748B))
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
                                            fontWeight = if (selectedProject.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (selectedProject.isNotBlank()) Color(0xFF0F172A) else Color(0xFF64748B),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = if (projectMenuExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                            contentDescription = "Expand",
                                            tint = Color(0xFF334155)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = projectMenuExpanded,
                                    onDismissRequest = { projectMenuExpanded = false },
                                    containerColor = Color.White,
                                    shadowElevation = 8.dp,
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .background(Color.White, RoundedCornerShape(12.dp))
                                ) {
                                    projects.forEach { proj ->
                                        val isSelected = proj == selectedProject
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = proj,
                                                        fontSize = 14.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                        color = if (isSelected) SitePulseOrange else Color(0xFF0F172A)
                                                    )
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Check,
                                                            contentDescription = "Selected",
                                                            tint = SitePulseOrange,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = Color(0xFF0F172A)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isSelected) Color(0xFFFFF7ED) else Color.White),
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
                                enabled = !isSubmitting,
                                colors = outdoorFieldColors
                            )
                        }

                        // LIVE GEOFENCING STATUS FOR SELECTED PROJECT
                        if (matchedProject?.geofenceEnabled == true) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val isInside = geofenceResult?.isInside == true
                            val distance = geofenceResult?.distanceMeters ?: 0f
                            val radius = matchedProject.geofenceRadius?.toInt() ?: 200

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (currentLocation == null) Color(0xFFFFFBEB)
                                    else if (isInside) Color(0xFFF0FDF4)
                                    else Color(0xFFFEF2F2)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (currentLocation == null) Color(0xFFFDE68A)
                                    else if (isInside) Color(0xFFBBF7D0)
                                    else Color(0xFFFECACA)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (currentLocation == null) Icons.Outlined.LocationSearching
                                        else if (isInside) Icons.Outlined.CheckCircle
                                        else Icons.Outlined.WarningAmber,
                                        contentDescription = null,
                                        tint = if (currentLocation == null) Color(0xFFD97706)
                                        else if (isInside) Color(0xFF16A34A)
                                        else Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (currentLocation == null) "GEOFENCE: WAITING FOR GPS"
                                            else if (isInside) "GEOFENCE VERIFIED (ON-SITE)"
                                            else "GEOFENCE ALERT (OUTSIDE PERIMETER)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (currentLocation == null) Color(0xFFB45309)
                                            else if (isInside) Color(0xFF15803D)
                                            else Color(0xFFB91C1C)
                                        )
                                        Text(
                                            text = if (currentLocation == null) "Acquiring GPS fix to verify presence within ${radius}m boundary"
                                            else if (isInside) "${String.format(Locale.US, "%.1f", distance)}m from site center • Perimeter: ${radius}m"
                                            else "${String.format(Locale.US, "%.1f", distance)}m from site center • Outside ${radius}m boundary. Time Out locked until on-site.",
                                            fontSize = 10.sp,
                                            color = if (currentLocation == null) Color(0xFF92400E)
                                            else if (isInside) Color(0xFF166534)
                                            else Color(0xFF991B1B)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ========================================================
                    // TIME IN & TIME OUT SUMMARY CARD (AUTO-CALCULATED HOURS)
                    // ========================================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                        border = BorderStroke(1.5.dp, Color(0xFFFDBA74))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Time In:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF9A3412),
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = effectiveTimeInFormatted,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Outlined.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFFEA580C),
                                    modifier = Modifier.size(16.dp)
                                )

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Time Out:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF9A3412),
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = formattedTimeOut,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0xFFFED7AA), thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Work Hours:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF9A3412),
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "Auto-calculated from shift duration",
                                        fontSize = 10.sp,
                                        color = Color(0xFF7C2D12)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFEA580C)
                                ) {
                                    Text(
                                        text = calculatedHoursDisplay,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            if (activeShift.hasGeofenceAlert) {
                                HorizontalDivider(color = Color(0xFFFED7AA), thickness = 1.dp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.WarningAmber,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "ALERT: GEOFENCE BREACH RECORDED",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB91C1C)
                                        )
                                        Text(
                                            text = "Left zone at ${activeShift.geofenceLeftTime} • Returned to zone at ${activeShift.geofenceReturnedTime.ifBlank { "Recorded on-site" }}",
                                            fontSize = 10.sp,
                                            color = Color(0xFF991B1B)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Work Completed
                    Column {
                        Text(
                            text = "Work Completed / Accomplishment",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
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
                            enabled = !isSubmitting,
                            colors = outdoorFieldColors
                        )
                    }

                    // Site Conditions (Auto GPS) - Professional & NO EMOJIS
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
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val weatherIcon = when (weather.lowercase(Locale.ROOT)) {
                                        "rainy" -> Icons.Outlined.WaterDrop
                                        "cloudy" -> Icons.Outlined.CloudQueue
                                        "windy" -> Icons.Outlined.Air
                                        "clear" -> Icons.Outlined.WbSunny
                                        else -> Icons.Outlined.WbSunny
                                    }
                                    Icon(
                                        imageVector = weatherIcon,
                                        contentDescription = null,
                                        tint = SitePulseOrange,
                                        modifier = Modifier.size(20.dp)
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

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.LocationOn,
                                                contentDescription = null,
                                                tint = TextGray,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = currentLocation?.label ?: "Site Location",
                                                fontSize = 11.sp,
                                                color = TextGray,
                                                maxLines = 1
                                            )
                                        }
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
                                                Icon(
                                                    imageVector = Icons.Outlined.WaterDrop,
                                                    contentDescription = null,
                                                    tint = SitePulseOrange,
                                                    modifier = Modifier.size(14.dp)
                                                )
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
                                                Icon(
                                                    imageVector = Icons.Outlined.Air,
                                                    contentDescription = null,
                                                    tint = SitePulseOrange,
                                                    modifier = Modifier.size(14.dp)
                                                )
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

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Automatically captured from live site weather. No manual input needed.",
                                        fontSize = 11.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = Color(0xFF64748B)
                                    )
                                }
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

                    // Manpower on Site (Workers, Supervisors, Sub-contractors) with Glove-Friendly Stepper Counters
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Groups,
                                    contentDescription = null,
                                    tint = SitePulseOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Manpower on Site",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SitePulseOrange
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ManpowerCounter(
                                    title = "Workers",
                                    value = workOnSite,
                                    onValueChange = { workOnSite = it },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isSubmitting
                                )
                                ManpowerCounter(
                                    title = "Supervisors",
                                    value = supervisors,
                                    onValueChange = { supervisors = it },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isSubmitting
                                )
                                ManpowerCounter(
                                    title = "Sub-contractors",
                                    value = subContractors,
                                    onValueChange = { subContractors = it },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isSubmitting
                                )
                            }
                        }
                    }

                    // Site Logistics & Equipment
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Inventory2,
                                    contentDescription = null,
                                    tint = SitePulseOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Site Logistics & Equipment",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SitePulseOrange
                                )
                            }

                            OutlinedTextField(
                                value = materialsDelivered,
                                onValueChange = { materialsDelivered = it },
                                label = { Text("Materials Delivered", fontWeight = FontWeight.SemiBold) },
                                placeholder = { Text("e.g. 50 bags cement, 100 pcs 16mm rebar, 2 loads gravel") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 3,
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isSubmitting,
                                colors = outdoorFieldColors
                            )

                            OutlinedTextField(
                                value = equipmentUsed,
                                onValueChange = { equipmentUsed = it },
                                label = { Text("Equipment Used", fontWeight = FontWeight.SemiBold) },
                                placeholder = { Text("e.g. 1 Backhoe, 1 Concrete Mixer, 2 Plate Compactors") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 3,
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isSubmitting,
                                colors = outdoorFieldColors
                            )
                        }
                    }

                    // Additional Notes
                    Column {
                        Text(
                            text = "Additional Notes",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
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
                            enabled = !isSubmitting,
                            colors = outdoorFieldColors
                        )
                    }

                    // Location Status Banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
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

                    val isGeofenceLocked = matchedProject?.geofenceEnabled == true && (currentLocation == null || geofenceResult?.isInside != true)

                    Button(
                        onClick = {
                            if (activeProjectName.isBlank()) {
                                localError = "Please select or enter a project name."
                                return@Button
                            }
                            if (matchedProject?.geofenceEnabled == true) {
                                if (currentLocation == null) {
                                    localError = "Cannot Time Out: Waiting for GPS location to verify site geofence."
                                    onRequestLocation()
                                    return@Button
                                }
                                if (geofenceResult?.isInside != true) {
                                    val distance = geofenceResult?.distanceMeters?.toInt() ?: 0
                                    val radius = matchedProject.geofenceRadius?.toInt() ?: 200
                                    localError = "Cannot Time Out: You are outside the project geofence (${distance}m from site center, limit is ${radius}m). You must be on-site to Time Out."
                                    return@Button
                                }
                            }
                            val finalHours = TimeTrackingManager.formatDuration(timeOutMs - effectiveTimeInMs)
                            onSubmit(
                                LogTimeFormData(
                                    projectName = activeProjectName.trim(),
                                    timeIn = effectiveTimeInFormatted,
                                    timeOut = formattedTimeOut,
                                    hours = finalHours,
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
                        enabled = !isSubmitting && !isGeofenceLocked,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isGeofenceLocked) Color(0xFFEF4444) else SitePulseOrange,
                            disabledContainerColor = Color(0xFFF1F5F9)
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
                        } else if (isGeofenceLocked) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Geofence Locked",
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        } else {
                            Icon(
                                imageVector = if (isOffline) Icons.Outlined.Save else Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOffline) "Save Offline Time Out" else "Confirm Time Out",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
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
    unread: Boolean,
    audience: String? = null,
    projectName: String? = null,
    senderName: String? = null
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
                        when (audience) {
                            "individual" -> Icons.Outlined.Person
                            "project" -> Icons.Outlined.Apartment
                            else -> Icons.Outlined.Campaign
                        },

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

                // Project & Direct Badges
                if (!projectName.isNullOrBlank() || audience == "individual") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!projectName.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEDE9FE)
                            ) {
                                Text(
                                    text = projectName,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF6D28D9)
                                )
                            }
                        }
                        if (audience == "individual") {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = "Direct",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
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


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = time,
                        fontSize = 9.sp,
                        color = SitePulseOrange,
                        fontWeight = FontWeight.Medium
                    )

                    if (!senderName.isNullOrBlank()) {
                        Text(
                            text = "From: $senderName",
                            fontSize = 9.sp,
                            color = TextGray,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, Color(0xFFFED7AA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFFFF7ED), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = SitePulseOrange,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Operations Advisory",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Text(
                    text = "Track assigned work orders, inspection milestones, and daily safety logs.",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = TextGray
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
    totalTasks: Int,
    onClick: () -> Unit = {}
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

    val healthColor = when {
        health >= 90 -> Green
        health >= 75 -> Color(0xFFFF8A00)
        else -> Color(0xFFE53935)
    }

    val healthBg = when {
        health >= 90 -> Color(0xFFE1F5E7)
        health >= 75 -> Color(0xFFFFEED7)
        else -> Color(0xFFFFEBEE)
    }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clickable { onClick() },
        shape =
            RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            ),
        border = BorderStroke(1.dp, CardBorderStroke),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier =
                    Modifier
                        .size(68.dp)
                        .background(
                            healthBg,
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
                        fontWeight = FontWeight.Bold,
                        color = healthColor
                    )

                    Text(
                        text = "HEALTH",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = healthColor
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.width(16.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
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
                        FontWeight.Bold,
                    color =
                        Color.Black
                )

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                val displayDelayed = if (delayed > 0) delayed else delayedTasks

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatusBadge(
                        text =
                            "● $onTrack On Track",
                        background =
                            Color(0xFFE1F5E7),
                        textColor =
                            Green
                    )

                    StatusBadge(
                        text =
                            "● $displayDelayed Delayed",
                        background =
                            if (displayDelayed > 0) Color(0xFFFFEED7) else Color(0xFFF1F3F5),
                        textColor =
                            if (displayDelayed > 0) Color(0xFFFF8A00) else TextGray
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(
                    text =
                        if (totalTasks > 0) {
                            "$completedTasks completed • $delayedTasks delayed • $totalTasks total tasks"
                        } else {
                            "No tasks currently scheduled"
                        },
                    fontSize =
                        11.sp,
                    color =
                        TextGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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
// QUICK ACTIONS - TIME IN & TIME OUT
// ============================================================

@Composable
private fun QuickActions(
    isTimedIn: Boolean,
    activeShift: TimeTrackingManager.ActiveShift,
    elapsedDuration: String,
    isLoggingTime: Boolean = false,
    onTimeInClick: () -> Unit = {},
    onTimeOutClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Active Shift Card when timed in
        if (isTimedIn) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                border = BorderStroke(1.5.dp, Color(0xFFFDBA74))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFFEA580C), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF16A34A), CircleShape)
                                )
                                Text(
                                    text = "SHIFT ACTIVE • ${activeShift.timeInFormatted}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC2410C),
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = activeShift.projectName.ifBlank { "Site Operations" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = elapsedDuration,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFEA580C)
                        )
                        Text(
                            text = "Elapsed Time",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            if (activeShift.hasGeofenceAlert) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (activeShift.currentlyOutsideGeofence) Color(0xFFFEF2F2) else Color(0xFFFFFBEB)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (activeShift.currentlyOutsideGeofence) Color(0xFFFECACA) else Color(0xFFFDE68A)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = if (activeShift.currentlyOutsideGeofence) Color(0xFFDC2626) else Color(0xFFD97706),
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            val incidentSuffix = if (activeShift.incidentsCount > 1) " (${activeShift.incidentsCount} Incidents)" else ""
                            Text(
                                text = if (activeShift.currentlyOutsideGeofence) "ALERT: OUTSIDE GEOFENCE ZONE$incidentSuffix" else "GEOFENCE ALERT RECORDED$incidentSuffix",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeShift.currentlyOutsideGeofence) Color(0xFFB91C1C) else Color(0xFFB45309)
                            )
                            Text(
                                text = if (activeShift.currentlyOutsideGeofence)
                                    "Left geofence zone at ${activeShift.geofenceLeftTime}. Move on-site to resume shift."
                                else if (activeShift.incidentsCount > 1)
                                    "Latest: Left zone at ${activeShift.geofenceLeftTime} • Returned at ${activeShift.geofenceReturnedTime}"
                                else
                                    "Left zone at ${activeShift.geofenceLeftTime} • Returned to zone at ${activeShift.geofenceReturnedTime}",
                                fontSize = 10.sp,
                                color = if (activeShift.currentlyOutsideGeofence) Color(0xFF991B1B) else Color(0xFF92400E)
                            )
                        }
                    }
                }
            }
        }

        // Two Buttons: Time In & Time Out
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Time In Button
            Button(
                onClick = onTimeInClick,
                enabled = !isTimedIn && !isLoggingTime,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTimedIn) Color(0xFFE2E8F0) else Color(0xFF16A34A),
                    disabledContainerColor = Color(0xFFF1F5F9)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isTimedIn) Icons.Outlined.CheckCircle else Icons.Outlined.Login,
                        contentDescription = null,
                        tint = if (isTimedIn) Color(0xFF64748B) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isTimedIn) "Timed In" else "Time In",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isTimedIn) Color(0xFF64748B) else Color.White
                    )
                }
            }

            // Time Out Button
            Button(
                onClick = onTimeOutClick,
                enabled = isTimedIn && !isLoggingTime,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEA580C),
                    disabledContainerColor = Color(0xFFF1F5F9)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Logout,
                        contentDescription = null,
                        tint = if (isTimedIn) Color.White else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLoggingTime) "Saving..." else "Time Out",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isTimedIn) Color.White else Color(0xFF94A3B8)
                    )
                }
            }
        }
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
                fontSize = 13.sp,
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
                ) ||
                project.status.equals(
                    "Ongoing",
                    ignoreCase = true
                ) ||
                project.status.equals(
                    "In Progress",
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
                    color = TextGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )


                Spacer(
                    modifier =
                        Modifier.width(6.dp)
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
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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
                    project.progress.coerceIn(0, 100) / 100f
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),

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


        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = activity.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )


            Text(
                text = activity.description,
                fontSize = 10.sp,
                color = TextGray,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
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
            badgeCount = 0,
            onClick = onHomeClick
        )


        SitePulseNavigationItem(
            title = "Projects",
            icon = Icons.Outlined.BusinessCenter,
            selected =
                selectedScreen == "projects",
            badgeCount = NavBadgeState.ongoingProjectsCount,
            onClick = onProjectsClick
        )


        SitePulseNavigationItem(
            title = "Messages",
            icon =
                Icons.Outlined.ChatBubbleOutline,
            selected =
                selectedScreen == "chat",
            badgeCount = NavBadgeState.unreadMessagesCount,
            onClick = onMessagesClick
        )


        SitePulseNavigationItem(
            title = "Tasks",
            icon = Icons.Outlined.TaskAlt,
            selected =
                selectedScreen == "tasks",
            badgeCount = NavBadgeState.ongoingTasksCount,
            onClick = onTasksClick
        )


        SitePulseNavigationItem(
            title = "Profile",
            icon = Icons.Outlined.Person,
            selected =
                selectedScreen == "profile",
            badgeCount = 0,
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

    badgeCount: Int = 0,

    onClick: () -> Unit

) {

    NavigationBarItem(

        selected =
            selected,

        onClick =
            onClick,

        icon = {

            BadgedBox(
                badge = {
                    if (badgeCount > 0) {
                        Badge(
                            containerColor = SitePulseOrange,
                            contentColor = Color.White
                        ) {
                            Text(
                                text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector =
                        icon,
                    contentDescription =
                        title,
                    modifier =
                        Modifier.size(21.dp)
                )
            }
        },

        label = {

            Text(
                text = title,
                fontSize = 9.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
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