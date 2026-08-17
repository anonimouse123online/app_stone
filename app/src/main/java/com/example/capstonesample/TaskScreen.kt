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
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// ============================================================
// TASK LIST MODEL
// ============================================================

data class SiteTask(
    val id: Int,
    val title: String,
    val project: String,
    val status: String,
    val schedule: String,
    val assigneeInitials: String,
    val assigneeColor: Color,
    val indicatorColor: Color,
    val overdue: Boolean = false
)


// ============================================================
// SAMPLE DATA
// TODO API:
// Later replace with GET /api/tasks
// ============================================================

private val sampleTasks = listOf(

    SiteTask(
        id = 1,
        title = "Foundation pouring",
        project = "Riverside Bridge",
        status = "Ready",
        schedule = "Today · 10:30 AM",
        assigneeInitials = "SA",
        assigneeColor = Color(0xFF263238),
        indicatorColor = Color(0xFF1B9A41)
    ),

    SiteTask(
        id = 2,
        title = "Crane #02 inspection",
        project = "Tower A",
        status = "Urgent",
        schedule = "Overdue yesterday",
        assigneeInitials = "OP",
        assigneeColor = Color(0xFFDDEBFF),
        indicatorColor = Color.Red,
        overdue = true
    ),

    SiteTask(
        id = 3,
        title = "Steel rebar delivery",
        project = "Sector 4 Roads",
        status = "In Progress",
        schedule = "Tomorrow",
        assigneeInitials = "NM",
        assigneeColor = Color(0xFFFFE1E1),
        indicatorColor = Color(0xFFF15A24)
    )
)


// ============================================================
// COLORS
// ============================================================

private val TaskBackground = Color(0xFFF0E1D8)
private val TaskOrange = Color(0xFFF15A24)
private val TaskGray = Color(0xFF777777)


// ============================================================
// TASKS SCREEN
// ============================================================

@Composable
fun TasksScreen(
    onHomeClick: () -> Unit,
    onProjectsClick: () -> Unit,
    onMessagesClick: () -> Unit,
    onProfileClick: () -> Unit,
    onTaskClick: (SiteTask) -> Unit = {},
    onAddTaskClick: () -> Unit = {}
) {

    var selectedFilter by remember {
        mutableStateOf("All")
    }

    val filteredTasks = when (selectedFilter) {

        "To Do" ->
            sampleTasks.filter {
                it.status == "Ready"
            }

        "In Progress" ->
            sampleTasks.filter {
                it.status == "In Progress"
            }

        "Done" ->
            emptyList()

        else ->
            sampleTasks
    }


    Scaffold(

        containerColor = TaskBackground,

        topBar = {
            TasksTopBar()
        },

        bottomBar = {

            TasksBottomNavigationBar(
                selectedScreen = "tasks",

                onHomeClick = onHomeClick,

                onProjectsClick = onProjectsClick,

                onMessagesClick = onMessagesClick,

                onTasksClick = {
                    // Already on Tasks
                },

                onProfileClick = onProfileClick
            )
        },

        floatingActionButton = {

            FloatingActionButton(
                onClick = onAddTaskClick,
                containerColor = TaskOrange,
                contentColor = Color.White,
                shape = CircleShape
            ) {

                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Task"
                )
            }
        }

    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {


            // =================================================
            // WHITE HEADER
            // =================================================

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 16.dp,
                        bottom = 12.dp
                    )
            ) {

                Text(
                    text = "Tasks",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )


                // =============================================
                // FILTER BUTTONS
                // =============================================

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    TaskFilterButton(
                        text = "All",
                        selected = selectedFilter == "All",
                        onClick = {
                            selectedFilter = "All"
                        }
                    )

                    TaskFilterButton(
                        text = "To Do",
                        selected = selectedFilter == "To Do",
                        onClick = {
                            selectedFilter = "To Do"
                        }
                    )

                    TaskFilterButton(
                        text = "In Progress",
                        selected = selectedFilter == "In Progress",
                        onClick = {
                            selectedFilter = "In Progress"
                        }
                    )

                    TaskFilterButton(
                        text = "Done",
                        selected = selectedFilter == "Done",
                        onClick = {
                            selectedFilter = "Done"
                        }
                    )
                }
            }


            // =================================================
            // TASK LIST
            // =================================================

            LazyColumn(

                modifier = Modifier
                    .fillMaxSize()
                    .background(TaskBackground),

                contentPadding = PaddingValues(
                    horizontal = 14.dp,
                    vertical = 14.dp
                ),

                verticalArrangement = Arrangement.spacedBy(12.dp)

            ) {


                items(
                    items = filteredTasks,
                    key = { it.id }
                ) { task ->

                    TaskListCard(
                        task = task,
                        onClick = {
                            onTaskClick(task)
                        }
                    )
                }


                item {

                    Spacer(
                        modifier = Modifier.height(70.dp)
                    )
                }
            }
        }
    }
}


// ============================================================
// TOP BAR
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksTopBar() {

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
                    // TODO API:
                    // GET /api/notifications
                }
            ) {

                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.Black
                )
            }


            Box(
                modifier = Modifier
                    .padding(end = 14.dp)
                    .size(36.dp)
                    .background(
                        Color(0xFF263238),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "SA",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },

        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
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
        modifier = Modifier
            .background(
                color =
                    if (selected)
                        TaskOrange
                    else
                        Color(0xFFE9DED8),

                shape = RoundedCornerShape(10.dp)
            )
            .clickable {
                onClick()
            }
            .padding(
                horizontal = 14.dp,
                vertical = 8.dp
            )
    ) {

        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color =
                if (selected)
                    Color.White
                else
                    TaskGray
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {


            // =================================================
            // TITLE + STATUS
            // =================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            task.indicatorColor,
                            CircleShape
                        )
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = task.title,
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                TaskStatusBadge(
                    status = task.status
                )
            }


            Spacer(
                modifier = Modifier.height(10.dp)
            )


            // =================================================
            // PROJECT
            // =================================================

            Row {

                Text(
                    text = "Project: ",
                    fontSize = 11.sp,
                    color = TaskGray
                )

                Text(
                    text = task.project,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            }


            Spacer(
                modifier = Modifier.height(10.dp)
            )

            HorizontalDivider(
                color = Color(0xFFF0EBE8)
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )


            // =================================================
            // TIME + AVATAR
            // =================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,

                    modifier = Modifier.size(16.dp),

                    tint =
                        if (task.overdue)
                            Color.Red
                        else
                            TaskGray
                )

                Spacer(
                    modifier = Modifier.width(6.dp)
                )

                Text(
                    text = task.schedule,

                    fontSize = 10.sp,

                    color =
                        if (task.overdue)
                            Color.Red
                        else
                            TaskGray,

                    fontWeight =
                        if (task.overdue)
                            FontWeight.Medium
                        else
                            FontWeight.Normal
                )


                Spacer(
                    modifier = Modifier.weight(1f)
                )


                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            task.assigneeColor,
                            CircleShape
                        ),

                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = task.assigneeInitials,

                        fontSize = 8.sp,

                        fontWeight = FontWeight.Bold,

                        color =
                            if (task.assigneeColor == Color(0xFF263238))
                                Color.White
                            else
                                Color(0xFF34558B)
                    )
                }
            }
        }
    }
}


// ============================================================
// STATUS BADGE
// ============================================================

@Composable
private fun TaskStatusBadge(
    status: String
) {

    val background: Color

    val textColor: Color


    when (status) {

        "Ready" -> {

            background =
                Color(0xFFDDF4E1)

            textColor =
                Color(0xFF1B9A41)
        }

        "Urgent" -> {

            background =
                Color(0xFFFFDEDE)

            textColor =
                Color.Red
        }

        else -> {

            background =
                Color(0xFFFFE5D8)

            textColor =
                TaskOrange
        }
    }


    Box(
        modifier = Modifier
            .background(
                background,
                RoundedCornerShape(8.dp)
            )
            .padding(
                horizontal = 8.dp,
                vertical = 5.dp
            )
    ) {

        Text(
            text = status,
            fontSize = 9.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}


// ============================================================
// TASKS BOTTOM NAVIGATION
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
        containerColor = Color.White,
        tonalElevation = 3.dp
    ) {

        TasksNavigationItem(
            title = "Home",
            icon = Icons.Outlined.Home,
            selected = selectedScreen == "dashboard",
            onClick = onHomeClick
        )

        TasksNavigationItem(
            title = "Projects",
            icon = Icons.Outlined.BusinessCenter,
            selected = selectedScreen == "projects",
            onClick = onProjectsClick
        )

        TasksNavigationItem(
            title = "Messages",
            icon = Icons.Outlined.ChatBubbleOutline,
            selected = selectedScreen == "chat",
            onClick = onMessagesClick
        )

        TasksNavigationItem(
            title = "Tasks",
            icon = Icons.Outlined.TaskAlt,
            selected = selectedScreen == "tasks",
            onClick = onTasksClick
        )

        TasksNavigationItem(
            title = "Profile",
            icon = Icons.Outlined.Person,
            selected = selectedScreen == "profile",
            onClick = onProfileClick
        )
    }
}


// ============================================================
// NAV ITEM
// ============================================================

@Composable
private fun RowScope.TasksNavigationItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {

    NavigationBarItem(

        selected = selected,

        onClick = onClick,

        icon = {

            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(21.dp)
            )
        },

        label = {

            Text(
                text = title,
                fontSize = 9.sp
            )
        },

        colors = NavigationBarItemDefaults.colors(

            selectedIconColor = TaskOrange,

            selectedTextColor = TaskOrange,

            indicatorColor = Color(0xFFFFE7DD),

            unselectedIconColor = Color.Gray,

            unselectedTextColor = Color.Gray
        )
    )
}