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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Tune
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
// PROJECT UI MODEL
// ============================================================

data class SiteProject(

    val id: String,

    val code: String,

    val name: String,

    val location: String,

    val scope: String,

    val client: String,

    val budget: String,

    val phase: String,

    val status: String,

    val progress: Int,

    val startDate: String,

    val dueDate: String,

    val manager: String
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
// PROJECTS SCREEN
// ============================================================

@Composable
fun ProjectsScreen(

    onHomeClick: () -> Unit,

    onMessagesClick: () -> Unit,

    onTasksClick: () -> Unit,

    onProfileClick: () -> Unit,

    onProjectClick: (SiteProject) -> Unit = {},

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

    // Project currently opened by the user.
    // Keeping this state here makes project cards work immediately,
    // even before you add a NavHost route for project details.
    var selectedProject by remember {
        mutableStateOf<SiteProject?>(null)
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
    // CONVERT API PROJECT -> UI PROJECT
    // ============================================================

    fun convertProject(
        project: com.example.capstonesample.data.model.ProjectResponse
    ): SiteProject {

        return SiteProject(

            id =
                project.id ?: "",

            code =
                project.code ?: "",

            name =
                project.name ?: "Unnamed Project",

            location =
                project.location ?: "No location",

            scope =
                project.scope ?: "No scope",

            client =
                project.client ?: "No client",

            budget =
                project.budget ?: "0",

            phase =
                project.phase ?: "No phase",

            status =
                project.status ?: "Planning",

            progress =
                project.progress
                    ?.toInt()
                    ?: 0,

            startDate =
                project.startDate ?: "No start date",

            dueDate =
                project.dueDate ?: "No due date",

            manager =
                project.manager ?: "Not assigned"
        )
    }


    // ============================================================
    // LOAD PROJECTS
    // ============================================================

    fun loadProjects() {

        scope.launch {

            if (
                token.isBlank() ||
                token.startsWith("LOCAL_")
            ) {

                println(
                    "Skipping projects API: no server JWT."
                )

                return@launch
            }


            isLoading = true


            try {

                println(
                    "===================================="
                )

                println(
                    "🌐 LOADING PROJECTS"
                )

                println(
                    "===================================="
                )


                val response =
                    RetrofitClient.api.getJoinedProjects(
                        token = "Bearer $token"
                    )


                println(
                    "GET PROJECTS HTTP = ${response.code()}"
                )


                if (
                    response.isSuccessful
                ) {

                    val result =
                        response.body()

                    println("===== JOINED PROJECT DEBUG =====")
                    println("HTTP = ${response.code()}")
                    println("BODY = $result")
                    println("SUCCESS = ${result?.success}")
                    println("COUNT = ${result?.data?.size}")
                    println("===============================")


                    println(
                        "PROJECT API RESPONSE = $result"
                    )


                    if (
                        result?.success == true
                    ) {

                        projects =
                            result.data.map {
                                    project ->

                                convertProject(
                                    project
                                )
                            }


                        println(
                            "✅ PROJECTS LOADED = ${projects.size}"
                        )


                    } else {

                        println(
                            "❌ Project response returned success=false"
                        )
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
                        "❌ GET PROJECTS FAILED"
                    )

                    println(
                        "HTTP = ${response.code()}"
                    )

                    println(
                        "ERROR = $error"
                    )
                }


            } catch (
                e: Exception
            ) {

                e.printStackTrace()

                println(
                    "Unable to load projects: ${e.message}"
                )
            }


            isLoading = false
        }
    }


    // ============================================================
    // LOAD WHEN SCREEN OPENS
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
    // FILTER
    // ============================================================

    val filteredProjects =
        projects.filter { project ->

            project.name.contains(
                searchQuery,
                ignoreCase = true
            ) ||

                    project.code.contains(
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


            onJoin = { inviteCode ->

                scope.launch {

                    isJoining =
                        true

                    joinError =
                        null

                    joinSuccess =
                        null


                    // ====================================================
                    // TOKEN CHECK
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


                    if (
                        token.startsWith("LOCAL_")
                    ) {

                        joinError =
                            "You are using offline login. Sign in online before joining a project."

                        isJoining =
                            false

                        return@launch
                    }


                    try {

                        val cleanCode =
                            inviteCode.trim()


                        println(
                            "===================================="
                        )

                        println(
                            "JOIN PROJECT"
                        )

                        println(
                            "INVITE CODE = $cleanCode"
                        )

                        println(
                            "===================================="
                        )


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
                            "JOIN HTTP = ${response.code()}"
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
                                "JOIN RESULT = $result"
                            )


                            if (
                                result?.success == true
                            ) {

                                joinSuccess =
                                    result.message


                                println(
                                    "✅ JOIN SUCCESS"
                                )


                                val joinedProjectCode =
                                    result.projectId


                                println(
                                    "JOINED PROJECT CODE = $joinedProjectCode"
                                )


                                // =================================================
                                // FETCH PROJECT DETAILS IMMEDIATELY
                                // =================================================

                                if (
                                    !joinedProjectCode.isNullOrBlank()
                                ) {

                                    try {

                                        val projectResponse =
                                            RetrofitClient.api
                                                .getProjectByCode(

                                                    token =
                                                        "Bearer $token",

                                                    code =
                                                        joinedProjectCode
                                                )


                                        println(
                                            "GET JOINED PROJECT HTTP = ${projectResponse.code()}"
                                        )


                                        if (
                                            projectResponse.isSuccessful
                                        ) {

                                            val projectResult =
                                                projectResponse.body()


                                            println(
                                                "JOINED PROJECT RESULT = $projectResult"
                                            )


                                            val apiProject =
                                                projectResult?.data


                                            if (
                                                projectResult?.success == true &&
                                                apiProject != null
                                            ) {

                                                val joinedProject =
                                                    convertProject(
                                                        apiProject
                                                    )


                                                // =====================================
                                                // REMOVE DUPLICATE
                                                // THEN ADD CURRENT PROJECT
                                                // =====================================

                                                projects =
                                                    projects
                                                        .filterNot {

                                                            it.code.equals(
                                                                joinedProject.code,
                                                                ignoreCase = true
                                                            )
                                                        } +
                                                            joinedProject


                                                println(
                                                    "✅ PROJECT ADDED TO SCREEN"
                                                )

                                                println(
                                                    "NAME = ${joinedProject.name}"
                                                )

                                                println(
                                                    "CODE = ${joinedProject.code}"
                                                )
                                            }
                                        }


                                    } catch (
                                        e: Exception
                                    ) {

                                        e.printStackTrace()

                                        println(
                                            "Unable to fetch joined project details: ${e.message}"
                                        )
                                    }
                                }


                                // =================================================
                                // REFRESH COMPLETE PROJECT LIST TOO
                                // =================================================

                                loadProjects()


                                showJoinDialog =
                                    false


                            } else {

                                joinError =
                                    result?.message
                                        ?: "Unable to join project."
                            }


                        } else {

                            // ====================================================
                            // BACKEND ERROR
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
                                "❌ JOIN FAILED"
                            )

                            println(
                                "HTTP = ${response.code()}"
                            )

                            println(
                                "SERVER ERROR = $serverError"
                            )


                            joinError =
                                when (
                                    response.code()
                                ) {

                                    400 ->
                                        serverError
                                            ?: "Invalid or expired invite code."


                                    401 ->
                                        "Your login session has expired. Please sign in again."


                                    403 ->
                                        "You are not allowed to join this project."


                                    404 ->
                                        "Project or invite code was not found."


                                    409 -> {

                                        println(
                                            "Already member — refreshing joined projects."
                                        )

                                        loadProjects()

                                        showJoinDialog = false

                                        "You are already a member of this project."
                                    }


                                    500 ->
                                        serverError
                                            ?: "Server error while joining the project."


                                    else ->
                                        serverError
                                            ?: "Unable to join project. Error ${response.code()}."
                                }
                        }


                    } catch (
                        e: Exception
                    ) {

                        e.printStackTrace()


                        joinError =
                            "Unable to connect to server: ${
                                e.message ?: "Unknown error"
                            }"
                    }


                    isJoining =
                        false
                }
            }
        )
    }


    // ============================================================
    // OPENED PROJECT DETAILS
    // ============================================================

    selectedProject?.let { project ->
        ProjectDetailsScreen(
            project = project,
            onBackClick = { selectedProject = null },
            onHomeClick = onHomeClick,
            onMessagesClick = onMessagesClick,
            onTasksClick = onTasksClick,
            onProfileClick = onProfileClick
        )
        return
    }

    // ============================================================
    // UI
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

                onProjectsClick = {},

                onMessagesClick =
                    onMessagesClick,

                onTasksClick =
                    onTasksClick,

                onProfileClick =
                    onProfileClick
            )
        },


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

                containerColor =
                    ProjectOrange,

                contentColor =
                    Color.White,

                shape =
                    CircleShape

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
                        FontWeight.Bold
                )


                Spacer(
                    Modifier.height(
                        10.dp
                    )
                )


                Row(

                    verticalAlignment =
                        Alignment.CenterVertically

                ) {


                    OutlinedTextField(

                        value =
                            searchQuery,

                        onValueChange = {
                            searchQuery = it
                        },

                        modifier =
                            Modifier
                                .weight(1f)
                                .height(
                                    54.dp
                                ),

                        placeholder = {

                            Text(
                                "Search projects..."
                            )
                        },

                        leadingIcon = {

                            Icon(

                                imageVector =
                                    Icons.Default.Search,

                                contentDescription =
                                    "Search"
                            )
                        },

                        singleLine =
                            true,

                        shape =
                            RoundedCornerShape(
                                12.dp
                            ),

                        colors =
                            OutlinedTextFieldDefaults
                                .colors(

                                    focusedContainerColor =
                                        SearchBackground,

                                    unfocusedContainerColor =
                                        SearchBackground,

                                    focusedBorderColor =
                                        Color.Transparent,

                                    unfocusedBorderColor =
                                        Color.Transparent
                                )
                    )


                    Spacer(
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
                                "Filter"
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
                // EMPTY
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
                            Modifier.height(
                                12.dp
                            )
                        )


                        Text(

                            text =
                                "No projects yet",

                            fontSize =
                                17.sp,

                            fontWeight =
                                FontWeight.Bold
                        )


                        Spacer(
                            Modifier.height(
                                5.dp
                            )
                        )


                        Text(

                            text =
                                "Tap + and enter the project invite code.",

                            fontSize =
                                12.sp,

                            color =
                                GrayText
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

                        key = { project ->

                            // Use project code where possible
                            if (
                                project.code.isNotBlank()
                            ) {

                                project.code

                            } else {

                                project.id.toString()
                            }
                        }

                    ) { project ->


                        ProjectListCard(

                            project =
                                project,

                            onClick = {
                                println("====================================")
                                println("📂 PROJECT CLICKED")
                                println("NAME = ${project.name}")
                                println("CODE = ${project.code}")
                                println("====================================")

                                // Open the project immediately inside this screen.
                                selectedProject = project

                                // Keep this callback so MainActivity/NavHost can also
                                // react to the project click later if you want.
                                onProjectClick(project)
                            }
                        )
                    }


                    item {

                        Spacer(
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
                    Modifier.height(
                        16.dp
                    )
                )


                OutlinedTextField(

                    value =
                        projectCode,

                    onValueChange = {
                        projectCode = it
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
                        .isNotEmpty() &&
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
                    FontWeight.Bold
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
                        "Notifications"
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

    val isGoodStatus =
        project.status.equals(
            "On track",
            ignoreCase = true
        ) ||
                project.status.equals(
                    "Active",
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

            // ====================================================
            // TITLE + STATUS
            // ====================================================

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
                            project.phase,

                        fontSize =
                            10.sp,

                        color =
                            GrayText
                    )


                    Text(

                        text =
                            project.name,

                        fontSize =
                            18.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            Color.Black
                    )


                    Text(

                        text =
                            project.code,

                        fontSize =
                            10.sp,

                        color =
                            GrayText
                    )
                }


                Box(

                    modifier =
                        Modifier
                            .background(

                                if (
                                    isGoodStatus
                                )
                                    Color(
                                        0xFFDDF4E1
                                    )
                                else
                                    Color(
                                        0xFFFFDEDE
                                    ),

                                RoundedCornerShape(
                                    8.dp
                                )
                            )
                            .padding(
                                horizontal = 9.dp,
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
                                isGoodStatus
                            )
                                Color(
                                    0xFF199642
                                )
                            else
                                Color.Red
                    )
                }
            }


            Spacer(
                Modifier.height(
                    14.dp
                )
            )


            // ====================================================
            // PROJECT DETAILS
            // ====================================================

            ProjectDetailRow(
                label = "Location",
                value = project.location
            )


            ProjectDetailRow(
                label = "Client",
                value = project.client
            )


            ProjectDetailRow(
                label = "Manager",
                value = project.manager
            )


            ProjectDetailRow(
                label = "Scope",
                value = project.scope
            )


            ProjectDetailRow(
                label = "Budget",
                value = project.budget
            )


            ProjectDetailRow(
                label = "Start Date",
                value = project.startDate
            )


            ProjectDetailRow(
                label = "Due Date",
                value = project.dueDate
            )


            Spacer(
                Modifier.height(
                    14.dp
                )
            )


            // ====================================================
            // PROGRESS
            // ====================================================

            Row(

                modifier =
                    Modifier.fillMaxWidth()

            ) {

                Text(

                    text =
                        "Progress",

                    fontSize =
                        11.sp,

                    color =
                        GrayText
                )


                Spacer(
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
                        ProjectOrange
                )
            }


            Spacer(
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

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            6.dp
                        ),

                color =
                    ProjectOrange,

                trackColor =
                    Color(
                        0xFFEAE4E1
                    )
            )
        }
    }
}

@Composable
private fun ProjectDetailRow(

    label: String,

    value: String

) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 3.dp
                )

    ) {

        Text(

            text =
                "$label: ",

            fontSize =
                11.sp,

            color =
                GrayText
        )


        Text(

            text =
                value,

            fontSize =
                11.sp,

            fontWeight =
                FontWeight.Medium,

            color =
                Color.Black
        )
    }
}
// ============================================================
// PROJECT DETAILS SCREEN
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectDetailsScreen(
    project: SiteProject,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onMessagesClick: () -> Unit,
    onTasksClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Scaffold(
        containerColor = ProjectBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = project.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = project.code,
                            fontSize = 10.sp,
                            color = GrayText
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            ProjectsBottomNavigationBar(
                selectedScreen = "projects",
                onHomeClick = onHomeClick,
                onProjectsClick = onBackClick,
                onMessagesClick = onMessagesClick,
                onTasksClick = onTasksClick,
                onProfileClick = onProfileClick
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Project Overview",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(14.dp))

                        ProjectDetailRow("Project Code", project.code)
                        ProjectDetailRow("Location", project.location)
                        ProjectDetailRow("Client", project.client)
                        ProjectDetailRow("Manager", project.manager)
                        ProjectDetailRow("Scope", project.scope)
                        ProjectDetailRow("Budget", project.budget)
                        ProjectDetailRow("Phase", project.phase)
                        ProjectDetailRow("Status", project.status)
                        ProjectDetailRow("Start Date", project.startDate)
                        ProjectDetailRow("Due Date", project.dueDate)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Project Progress",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = "${project.progress.coerceIn(0, 100)}%",
                                fontWeight = FontWeight.Bold,
                                color = ProjectOrange
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = {
                                project.progress.coerceIn(0, 100) / 100f
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = ProjectOrange,
                            trackColor = Color(0xFFEAE4E1)
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Project Modules",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "This is now the opened project. You can connect Documents, Tasks, Issues, Reports, and other project-specific screens here using project.code = ${project.code}.",
                            fontSize = 12.sp,
                            color = GrayText
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(60.dp))
            }
        }
    }
}

// ============================================================
// BOTTOM NAV
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
            Color.White

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
                        ProjectOrange,

                    selectedTextColor =
                        ProjectOrange,

                    indicatorColor =
                        Color(
                            0xFFFFE7DD
                        )
                )
    )
}