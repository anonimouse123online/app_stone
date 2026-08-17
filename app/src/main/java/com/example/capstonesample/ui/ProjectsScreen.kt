package com.example.capstonesample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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
import com.example.capstonesample.data.model.JoinProjectRequest

import kotlinx.coroutines.launch


// ============================================================
// PROJECT MODEL
// ============================================================

data class SiteProject(
    val id: Int,
    val category: String,
    val name: String,
    val manager: String,
    val progress: Int,
    val status: String,
    val dueDate: String,
    val remainingText: String
)


// ============================================================
// COLORS
// ============================================================

private val ProjectBackground =
    Color(0xFFF0E1D8)

private val ProjectOrange =
    Color(0xFFF15A24)

private val SearchBackground =
    Color(0xFFEADCD5)

private val GrayText =
    Color(0xFF777777)


// ============================================================
// PROJECT SCREEN
// ============================================================

@Composable
fun ProjectsScreen(

    onHomeClick: () -> Unit,

    onMessagesClick: () -> Unit,

    onTasksClick: () -> Unit,

    onProfileClick: () -> Unit,

    onProjectClick: (SiteProject) -> Unit = {},

    // JWT received from LoginScreen
    token: String = ""

) {

    var searchQuery by remember {
        mutableStateOf("")
    }


    var projects by remember {
        mutableStateOf<List<SiteProject>>(
            emptyList()
        )
    }


    var showJoinDialog by remember {
        mutableStateOf(false)
    }


    var isLoading by remember {
        mutableStateOf(false)
    }


    var isJoining by remember {
        mutableStateOf(false)
    }


    var joinError by remember {
        mutableStateOf<String?>(null)
    }


    var joinSuccess by remember {
        mutableStateOf<String?>(null)
    }


    val scope =
        rememberCoroutineScope()


    // ============================================================
    // DEBUG TOKEN
    // ============================================================

    LaunchedEffect(token) {

        println(
            "===================================="
        )

        println(
            "PROJECT SCREEN TOKEN"
        )

        println(
            "TOKEN EMPTY = ${token.isBlank()}"
        )

        println(
            "LOCAL TOKEN = ${token.startsWith("LOCAL_")}"
        )

        if (
            token.isNotBlank() &&
            !token.startsWith("LOCAL_")
        ) {

            println(
                "TOKEN START = ${token.take(20)}..."
            )
        }

        println(
            "===================================="
        )
    }


    // ============================================================
    // LOAD PROJECTS FROM BACKEND
    // ============================================================

    fun loadProjects() {

        scope.launch {

            // ========================================================
            // DO NOT CALL BACKEND WITH LOCAL TOKEN
            // ========================================================

            if (
                token.isBlank() ||
                token.startsWith("LOCAL_")
            ) {

                println(
                    "Skipping project API because there is no server JWT."
                )

                return@launch
            }


            isLoading = true


            try {

                val response =
                    RetrofitClient.api
                        .getProjects(

                            token =
                                "Bearer $token"
                        )


                println(
                    "GET PROJECTS HTTP = ${response.code()}"
                )


                if (
                    response.isSuccessful
                ) {

                    val apiProjects =
                        response.body()
                            ?: emptyList()


                    projects =
                        apiProjects.map { project ->

                            SiteProject(

                                id =
                                    project.id,

                                category =
                                    project.category
                                        ?: "PROJECT",

                                name =
                                    project.name,

                                manager =
                                    project.manager
                                        ?: "Not assigned",

                                progress =
                                    project.progress
                                        ?: 0,

                                status =
                                    project.status
                                        ?: "Active",

                                dueDate =
                                    project.dueDate
                                        ?: "No due date",

                                remainingText =
                                    project.remainingText
                                        ?: ""
                            )
                        }


                    println(
                        "✅ PROJECTS LOADED = ${projects.size}"
                    )


                } else {

                    val serverError =
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
                        "❌ Failed to load projects"
                    )

                    println(
                        "HTTP ${response.code()}"
                    )

                    println(
                        "SERVER = $serverError"
                    )
                }


            } catch (
                e: Exception
            ) {

                e.printStackTrace()

                println(
                    "Unable to refresh projects: ${e.message}"
                )
            }


            isLoading = false
        }
    }


    // ============================================================
    // LOAD PROJECTS WHEN SCREEN OPENS
    // ============================================================

    LaunchedEffect(token) {

        if (
            token.isNotBlank() &&
            !token.startsWith("LOCAL_")
        ) {

            loadProjects()
        }
    }


    // ============================================================
    // SEARCH
    // ============================================================

    val filteredProjects =
        projects.filter { project ->

            project.name.contains(
                searchQuery,
                ignoreCase = true
            ) ||

                    project.manager.contains(
                        searchQuery,
                        ignoreCase = true
                    )
        }


    // ============================================================
    // JOIN PROJECT DIALOG
    // ============================================================

    if (
        showJoinDialog
    ) {

        JoinProjectDialog(

            isLoading =
                isJoining,

            errorMessage =
                joinError,

            successMessage =
                joinSuccess,


            onDismiss = {

                if (
                    !isJoining
                ) {

                    showJoinDialog =
                        false

                    joinError =
                        null

                    joinSuccess =
                        null
                }
            },


            onJoin = { code ->

                scope.launch {

                    isJoining =
                        true

                    joinError =
                        null

                    joinSuccess =
                        null


                    // ====================================================
                    // CHECK AUTH TOKEN
                    // ====================================================

                    if (
                        token.isBlank()
                    ) {

                        joinError =
                            "No login session found. Please sign in again."

                        isJoining =
                            false

                        return@launch
                    }


                    // ====================================================
                    // LOCAL TOKEN CANNOT JOIN SERVER PROJECT
                    // ====================================================

                    if (
                        token.startsWith("LOCAL_")
                    ) {

                        joinError =
                            "This account is using offline login. Sign in online before joining a project."

                        isJoining =
                            false

                        return@launch
                    }


                    try {

                        val cleanCode =
                            code.trim()


                        // ====================================================
                        // DEBUG
                        // ====================================================

                        println(
                            "===================================="
                        )

                        println(
                            "JOIN PROJECT REQUEST"
                        )

                        println(
                            "INVITE CODE = $cleanCode"
                        )

                        println(
                            "TOKEN START = ${token.take(20)}..."
                        )

                        println(
                            "JSON FIELD = invite_code"
                        )

                        println(
                            "===================================="
                        )


                        // ====================================================
                        // JOIN PROJECT API
                        //
                        // Android sends:
                        //
                        // {
                        //     "invite_code": "XXXX-XXXX"
                        // }
                        //
                        // ====================================================

                        val response =
                            RetrofitClient.api
                                .joinProject(

                                    token =
                                        "Bearer $token",

                                    request =
                                        JoinProjectRequest(

                                            inviteCode =
                                                cleanCode
                                        )
                                )


                        println(
                            "JOIN HTTP STATUS = ${response.code()}"
                        )


                        // ====================================================
                        // SUCCESS
                        // ====================================================

                        if (
                            response.isSuccessful
                        ) {

                            val result =
                                response.body()


                            println(
                                "JOIN RESPONSE = $result"
                            )


                            if (
                                result?.success == true
                            ) {

                                joinSuccess =
                                    result.message


                                println(
                                    "✅ JOIN PROJECT SUCCESS"
                                )


                                println(
                                    "PROJECT ID = ${result.projectId}"
                                )


                                // Refresh project list from server
                                loadProjects()


                                // Close dialog
                                showJoinDialog =
                                    false


                            } else {

                                joinError =
                                    result?.message
                                        ?: "Unable to join project."
                            }


                        } else {

                            // ====================================================
                            // READ REAL BACKEND ERROR
                            // ====================================================

                            val serverError =
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
                                "❌ JOIN PROJECT FAILED"
                            )

                            println(
                                "JOIN HTTP STATUS = ${response.code()}"
                            )

                            println(
                                "JOIN SERVER ERROR = $serverError"
                            )


                            // ====================================================
                            // SHOW ERROR
                            // ====================================================

                            joinError =
                                when (
                                    response.code()
                                ) {

                                    400 -> {

                                        serverError
                                            ?: "Invalid or expired invite code."
                                    }


                                    401 -> {

                                        "Your login session is invalid or expired. Please sign out and sign in again."
                                    }


                                    403 -> {

                                        "You do not have permission to join this project."
                                    }


                                    404 -> {

                                        "Project or invite code was not found."
                                    }


                                    409 -> {

                                        "You are already a member of this project."
                                    }


                                    500 -> {

                                        serverError
                                            ?: "The server encountered an error while joining the project."
                                    }


                                    else -> {

                                        serverError
                                            ?: "Unable to join project. Error ${response.code()}."
                                    }
                                }
                        }


                    } catch (
                        e: Exception
                    ) {

                        e.printStackTrace()


                        println(
                            "❌ JOIN EXCEPTION"
                        )

                        println(
                            "${e.javaClass.simpleName}: ${e.message}"
                        )


                        joinError =
                            "Unable to connect to the server: ${
                                e.message
                                    ?: "Unknown network error"
                            }"
                    }


                    isJoining =
                        false
                }
            }
        )
    }


    // ============================================================
    // SCREEN
    // ============================================================

    Scaffold(

        containerColor =
            ProjectBackground,


        topBar = {

            ProjectsTopBar()
        },


        bottomBar = {

            ProjectsBottomNavigationBar(

                selectedScreen =
                    "projects",

                onHomeClick =
                    onHomeClick,

                onProjectsClick = {

                    // already on projects screen
                },

                onMessagesClick =
                    onMessagesClick,

                onTasksClick =
                    onTasksClick,

                onProfileClick =
                    onProfileClick
            )
        },


        // ========================================================
        // JOIN PROJECT BUTTON
        // ========================================================

        floatingActionButton = {

            FloatingActionButton(

                onClick = {

                    joinError =
                        null

                    joinSuccess =
                        null

                    showJoinDialog =
                        true
                },

                shape =
                    CircleShape,

                containerColor =
                    ProjectOrange,

                contentColor =
                    Color.White

            ) {

                Icon(

                    imageVector =
                        Icons.Default.Add,

                    contentDescription =
                        "Join Project"
                )
            }
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
            // HEADER
            // ====================================================

            Column(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White
                        )
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = 12.dp,
                            bottom = 14.dp
                        )

            ) {


                Text(

                    text =
                        "Projects",

                    fontSize =
                        24.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        Color.Black
                )


                Spacer(

                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )


                // =================================================
                // SEARCH
                // =================================================

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically

                ) {


                    OutlinedTextField(

                        value =
                            searchQuery,

                        onValueChange = {

                            searchQuery =
                                it
                        },

                        modifier =
                            Modifier
                                .weight(1f)
                                .height(
                                    54.dp
                                ),

                        placeholder = {

                            Text(

                                text =
                                    "Search projects...",

                                fontSize =
                                    13.sp,

                                color =
                                    GrayText
                            )
                        },

                        leadingIcon = {

                            Icon(

                                imageVector =
                                    Icons.Default.Search,

                                contentDescription =
                                    "Search",

                                tint =
                                    GrayText,

                                modifier =
                                    Modifier.size(
                                        20.dp
                                    )
                            )
                        },

                        singleLine =
                            true,

                        shape =
                            RoundedCornerShape(
                                12.dp
                            ),

                        colors =
                            OutlinedTextFieldDefaults.colors(

                                focusedContainerColor =
                                    SearchBackground,

                                unfocusedContainerColor =
                                    SearchBackground,

                                focusedBorderColor =
                                    Color.Transparent,

                                unfocusedBorderColor =
                                    Color.Transparent,

                                cursorColor =
                                    ProjectOrange,

                                focusedTextColor =
                                    Color.Black,

                                unfocusedTextColor =
                                    Color.Black
                            )
                    )


                    Spacer(

                        modifier =
                            Modifier.width(
                                8.dp
                            )
                    )


                    Box(

                        modifier =
                            Modifier
                                .size(
                                    46.dp
                                )
                                .background(

                                    SearchBackground,

                                    RoundedCornerShape(
                                        12.dp
                                    )
                                ),

                        contentAlignment =
                            Alignment.Center

                    ) {

                        Icon(

                            imageVector =
                                Icons.Outlined.Tune,

                            contentDescription =
                                "Filter",

                            tint =
                                Color.Black
                        )
                    }
                }
            }


            // ====================================================
            // LOADING
            // ====================================================

            if (
                isLoading &&
                projects.isEmpty()
            ) {

                Box(

                    modifier =
                        Modifier.fillMaxSize(),

                    contentAlignment =
                        Alignment.Center

                ) {

                    CircularProgressIndicator(

                        color =
                            ProjectOrange
                    )
                }


            } else if (
                filteredProjects.isEmpty()
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
                                Icons.Outlined.BusinessCenter,

                            contentDescription =
                                null,

                            modifier =
                                Modifier.size(
                                    55.dp
                                ),

                            tint =
                                GrayText
                        )


                        Spacer(

                            modifier =
                                Modifier.height(
                                    12.dp
                                )
                        )


                        Text(

                            text =
                                "No projects yet",

                            fontWeight =
                                FontWeight.Bold,

                            fontSize =
                                17.sp
                        )


                        Spacer(

                            modifier =
                                Modifier.height(
                                    5.dp
                                )
                        )


                        Text(

                            text =
                                "Tap + and enter the invite code provided by your administrator.",

                            color =
                                GrayText,

                            fontSize =
                                12.sp
                        )
                    }
                }


            } else {


                // =================================================
                // PROJECT LIST
                // =================================================

                LazyColumn(

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                ProjectBackground
                            ),

                    contentPadding =
                        PaddingValues(
                            horizontal = 18.dp,
                            vertical = 14.dp
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        )

                ) {


                    items(

                        items =
                            filteredProjects,

                        key = {

                            it.id
                        }

                    ) { project ->


                        ProjectListCard(

                            project =
                                project,

                            onClick = {

                                onProjectClick(
                                    project
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


// ============================================================
// JOIN PROJECT DIALOG
// ============================================================

@Composable
private fun JoinProjectDialog(

    isLoading: Boolean,

    errorMessage: String?,

    successMessage: String?,

    onDismiss: () -> Unit,

    onJoin: (String) -> Unit

) {


    var projectCode by remember {

        mutableStateOf("")
    }


    AlertDialog(

        onDismissRequest = {

            if (
                !isLoading
            ) {

                onDismiss()
            }
        },


        title = {

            Text(

                text =
                    "Join Project",

                fontWeight =
                    FontWeight.Bold
            )
        },


        text = {

            Column {


                Text(

                    text =
                        "Enter the invite code provided by your administrator.",

                    fontSize =
                        13.sp,

                    color =
                        GrayText
                )


                Spacer(

                    modifier =
                        Modifier.height(
                            16.dp
                        )
                )


                OutlinedTextField(

                    value =
                        projectCode,

                    onValueChange = {

                        // Preserve code exactly as typed.
                        projectCode =
                            it
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    label = {

                        Text(
                            "Invite Code"
                        )
                    },

                    placeholder = {

                        Text(
                            "Example: E9F7-DEB3"
                        )
                    },

                    enabled =
                        !isLoading,

                    singleLine =
                        true,

                    shape =
                        RoundedCornerShape(
                            12.dp
                        )
                )


                if (
                    errorMessage != null
                ) {

                    Spacer(

                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )


                    Text(

                        text =
                            errorMessage,

                        color =
                            MaterialTheme
                                .colorScheme
                                .error,

                        fontSize =
                            12.sp
                    )
                }


                if (
                    successMessage != null
                ) {

                    Spacer(

                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )


                    Text(

                        text =
                            successMessage,

                        color =
                            Color(
                                0xFF199642
                            ),

                        fontSize =
                            12.sp
                    )
                }
            }
        },


        confirmButton = {

            Button(

                onClick = {

                    val cleanCode =
                        projectCode.trim()


                    if (
                        cleanCode.isNotEmpty()
                    ) {

                        onJoin(
                            cleanCode
                        )
                    }
                },

                enabled =

                    projectCode
                        .trim()
                        .isNotEmpty()

                            &&

                            !isLoading,

                colors =
                    ButtonDefaults
                        .buttonColors(

                            containerColor =
                                ProjectOrange
                        )

            ) {


                if (
                    isLoading
                ) {

                    CircularProgressIndicator(

                        modifier =
                            Modifier.size(
                                18.dp
                            ),

                        strokeWidth =
                            2.dp,

                        color =
                            Color.White
                    )


                } else {

                    Text(
                        "Join"
                    )
                }
            }
        },


        dismissButton = {

            TextButton(

                onClick =
                    onDismiss,

                enabled =
                    !isLoading

            ) {

                Text(
                    "Cancel"
                )
            }
        }
    )
}


// ============================================================
// TOP BAR
// ============================================================

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
private fun ProjectsTopBar() {


    TopAppBar(

        title = {

            Text(

                text =
                    "SitePulse",

                fontSize =
                    20.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    Color.Black
            )
        },


        actions = {

            IconButton(

                onClick = {}

            ) {

                Icon(

                    imageVector =
                        Icons.Outlined.Notifications,

                    contentDescription =
                        "Notifications",

                    tint =
                        Color.Black
                )
            }


            Box(

                modifier =
                    Modifier
                        .padding(
                            end = 14.dp
                        )
                        .size(
                            36.dp
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
                        "SA",

                    color =
                        Color.White,

                    fontSize =
                        12.sp,

                    fontWeight =
                        FontWeight.Bold
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
// PROJECT CARD
// ============================================================

@Composable
private fun ProjectListCard(

    project: SiteProject,

    onClick: () -> Unit

) {


    val onTrack =
        project.status.equals(

            "On track",

            ignoreCase =
                true
        )


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
                ),

        elevation =
            CardDefaults
                .cardElevation(

                    defaultElevation =
                        0.dp
                )

    ) {


        Column(

            modifier =
                Modifier.padding(
                    14.dp
                )

        ) {


            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically

            ) {


                Column(

                    modifier =
                        Modifier.weight(
                            1f
                        )

                ) {


                    Text(

                        text =
                            project.category,

                        fontSize =
                            9.sp,

                        color =
                            GrayText
                    )


                    Text(

                        text =
                            project.name,

                        fontSize =
                            16.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            Color.Black
                    )
                }


                Box(

                    modifier =
                        Modifier
                            .background(

                                color =

                                    if (
                                        onTrack
                                    )

                                        Color(
                                            0xFFDDF4E1
                                        )

                                    else

                                        Color(
                                            0xFFFFDEDE
                                        ),

                                shape =
                                    RoundedCornerShape(
                                        8.dp
                                    )
                            )
                            .padding(
                                horizontal = 8.dp,
                                vertical = 5.dp
                            )

                ) {


                    Text(

                        text =
                            project.status,

                        fontSize =
                            9.sp,

                        color =

                            if (
                                onTrack
                            )

                                Color(
                                    0xFF199642
                                )

                            else

                                Color.Red,

                        fontWeight =
                            FontWeight.Medium
                    )
                }
            }


            Spacer(

                modifier =
                    Modifier.height(
                        10.dp
                    )
            )


            Row {


                Text(

                    text =
                        "Manager: ",

                    fontSize =
                        11.sp,

                    color =
                        GrayText
                )


                Text(

                    text =
                        project.manager,

                    fontSize =
                        11.sp,

                    fontWeight =
                        FontWeight.SemiBold,

                    color =
                        Color.Black
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
                        "Progress",

                    fontSize =
                        10.sp,

                    color =
                        GrayText
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
                        10.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =

                        if (
                            onTrack
                        )

                            ProjectOrange

                        else

                            Color.Black
                )
            }


            Spacer(

                modifier =
                    Modifier.height(
                        5.dp
                    )
            )


            LinearProgressIndicator(

                progress = {

                    project.progress /
                            100f
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            5.dp
                        ),

                color =

                    if (
                        onTrack
                    )

                        ProjectOrange

                    else

                        Color.Black,

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


            HorizontalDivider(

                color =
                    Color(
                        0xFFF0EBE8
                    )
            )


            Spacer(

                modifier =
                    Modifier.height(
                        10.dp
                    )
            )


            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically

            ) {


                Row {


                    Text(

                        text =
                            "Due Date: ",

                        fontSize =
                            10.sp,

                        color =
                            GrayText
                    )


                    Text(

                        text =
                            project.dueDate,

                        fontSize =
                            10.sp,

                        fontWeight =
                            FontWeight.Medium,

                        color =
                            Color.Black
                    )
                }


                Spacer(

                    modifier =
                        Modifier.weight(
                            1f
                        )
                )


                Text(

                    text =
                        project.remainingText,

                    fontSize =
                        10.sp,

                    fontWeight =
                        FontWeight.Medium,

                    color =
                        Color.Red
                )
            }
        }
    }
}


// ============================================================
// BOTTOM NAVIGATION
// ============================================================

@Composable
private fun ProjectsBottomNavigationBar(

    selectedScreen: String,

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


        ProjectsNavigationItem(

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


        ProjectsNavigationItem(

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


        ProjectsNavigationItem(

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


        ProjectsNavigationItem(

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


        ProjectsNavigationItem(

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
// NAV ITEM
// ============================================================

@Composable
private fun RowScope.ProjectsNavigationItem(

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
            NavigationBarItemDefaults
                .colors(

                    selectedIconColor =
                        ProjectOrange,

                    selectedTextColor =
                        ProjectOrange,

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