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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// ============================================================
// DATA MODELS
// ============================================================

data class Task(
    val title: String,
    val engineer: String,
    val status: String,
    val subtasks: List<String>
)

data class Project(
    val name: String,
    val type: String,
    val progress: Int,
    val status: String
)

data class FieldActivity(
    val title: String,
    val description: String,
    val type: ActivityType
)

enum class ActivityType {
    WARNING,
    INFO,
    SUCCESS
}


// ============================================================
// SAMPLE DASHBOARD DATA
// ============================================================

// TODO API:
// Replace these with your backend data later.
//
// Example:
//
// val projects by viewModel.projects.collectAsState()
// val activities by viewModel.activities.collectAsState()

private val sampleProjects = listOf(

    Project(
        name = "Riverside",
        type = "BRIDGE",
        progress = 72,
        status = "Active"
    ),

    Project(
        name = "Tower A",
        type = "HIGHRISE",
        progress = 45,
        status = "Delayed"
    )
)

private val sampleActivities = listOf(

    FieldActivity(
        title = "Foundation poured (Tower A)",
        description = "Logged by Inspector Mike R. • 09:15 AM",
        type = ActivityType.WARNING
    ),

    FieldActivity(
        title = "Safety Photos Uploaded",
        description = "Riverside Bridge site check • 08:30 AM",
        type = ActivityType.INFO
    ),

    FieldActivity(
        title = "Excavation Inspection Approved",
        description = "Site Engineer approval",
        type = ActivityType.SUCCESS
    )
)


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

    selectedScreen: String = "dashboard",

    onHomeClick: () -> Unit = {},

    onProjectsClick: () -> Unit = {},

    onChatClick: () -> Unit,

    onTasksClick: () -> Unit = {},

    onProfileClick: () -> Unit
) {

    Scaffold(

        containerColor = DashboardBackground,

        topBar = {
            DashboardTopBar()
        },

        bottomBar = {

            SitePulseBottomNavigation(

                selectedScreen = selectedScreen,

                onHomeClick = onHomeClick,

                onProjectsClick = onProjectsClick,

                onMessagesClick = onChatClick,

                onTasksClick = onTasksClick,

                onProfileClick = onProfileClick
            )
        }

    ) { padding ->

        LazyColumn(

            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DashboardBackground),

            contentPadding = PaddingValues(
                horizontal = 18.dp,
                vertical = 14.dp
            ),

            verticalArrangement = Arrangement.spacedBy(14.dp)

        ) {


            // =================================================
            // USER GREETING
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
                        modifier = Modifier.height(2.dp)
                    )

                    // TODO API:
                    // Replace with logged-in user's name.

                    Text(
                        text = "Seth Andrew",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    // TODO API:
                    // Replace with user role + actual date.

                    Text(
                        text = "Product Manager • Tuesday, Oct 24",
                        fontSize = 12.sp,
                        color = TextGray
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
            // HEALTH STATUS
            // =================================================

            item {

                // TODO API:
                // Replace 86 with:
                //
                // dashboard.healthScore

                HealthStatusCard(
                    health = 86
                )
            }


            // =================================================
            // QUICK ACTIONS
            // =================================================

            item {
                QuickActions()
            }


            // =================================================
            // ACTIVE PROJECTS
            // =================================================

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Active Projects",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "See details",
                        fontSize = 11.sp,
                        color = SitePulseOrange,
                        modifier = Modifier.clickable {
                            onProjectsClick()
                        }
                    )
                }
            }


            // =================================================
            // PROJECT CARDS
            // =================================================

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    sampleProjects.forEach { project ->

                        ProjectCard(
                            project = project,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }


            // =================================================
            // RECENT FIELD ACTIVITY
            // =================================================

            item {

                Text(
                    text = "Recent Field Activity",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }


            items(sampleActivities) { activity ->

                ActivityRow(
                    activity = activity
                )
            }


            item {

                Spacer(
                    modifier = Modifier.height(10.dp)
                )
            }
        }
    }
}


// ============================================================
// TOP BAR
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar() {

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
                    contentDescription = "Notifications"
                )
            }


            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(36.dp)
                    .background(
                        Color(0xFF263238),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                // TODO API:
                // Generate initials from logged-in user.

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
// ADVISORY CARD
// ============================================================

@Composable
private fun AdvisoryCard() {

    Card(

        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(14.dp),

        colors = CardDefaults.cardColors(
            containerColor = OrangeLight
        )

    ) {

        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = SitePulseOrange
            )

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Column {

                Text(
                    text = "Heat Advisory Active",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        "Mandatory shade breaks every 2 hrs. " +
                                "Hydration post active.",
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    color = TextGray
                )
            }
        }
    }
}


// ============================================================
// HEALTH STATUS
// ============================================================

@Composable
private fun HealthStatusCard(
    health: Int
) {

    Card(

        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )

    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),

            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(62.dp)
                    .background(
                        Color(0xFFFFEEE7),
                        CircleShape
                    ),

                contentAlignment = Alignment.Center
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
                modifier = Modifier.width(16.dp)
            )


            Column {

                Text(
                    text = "AGGREGATED STATUS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGray
                )

                Text(
                    text = "All Sites Operational",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(7.dp)
                )


                Row {

                    StatusBadge(
                        text = "● 2 On Track",
                        background = Color(0xFFE1F5E7),
                        textColor = Green
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    StatusBadge(
                        text = "● 1 Delayed",
                        background = Color(0xFFFFEED7),
                        textColor = Color(0xFFFF8A00)
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
private fun StatusBadge(
    text: String,
    background: Color,
    textColor: Color
) {

    Box(
        modifier = Modifier
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
        modifier = Modifier.fillMaxWidth(),
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

        modifier = modifier
            .height(48.dp)
            .clickable {

                // TODO:
                // Connect actions later.

            },

        shape = RoundedCornerShape(10.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )

    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.Center
        ) {

            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(18.dp),
                tint = SitePulseOrange
            )

            Spacer(
                modifier = Modifier.width(7.dp)
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
    project: Project,
    modifier: Modifier = Modifier
) {

    Card(

        modifier = modifier,

        shape = RoundedCornerShape(15.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )

    ) {

        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = project.type,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGray
                )

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                StatusBadge(
                    text = project.status,

                    background =
                        if (project.status == "Active")
                            Color(0xFFE1F5E7)
                        else
                            Color(0xFFFFEED7),

                    textColor =
                        if (project.status == "Active")
                            Green
                        else
                            Color(0xFFFF8A00)
                )
            }


            Spacer(
                modifier = Modifier.height(10.dp)
            )


            Text(
                text = project.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )


            Text(
                text = "${project.progress}% Complete",
                fontSize = 10.sp,
                color = TextGray
            )


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            LinearProgressIndicator(

                progress = {
                    project.progress / 100f
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp),

                color =
                    if (project.status == "Active")
                        SitePulseOrange
                    else
                        Color(0xFFFF9800),

                trackColor = Color(0xFFE9E4E1)
            )
        }
    }
}


// ============================================================
// FIELD ACTIVITY
// ============================================================

@Composable
private fun ActivityRow(
    activity: FieldActivity
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {

        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(9.dp)
                .background(
                    when (activity.type) {

                        ActivityType.WARNING ->
                            SitePulseOrange

                        ActivityType.INFO ->
                            Blue

                        ActivityType.SUCCESS ->
                            Green
                    },

                    CircleShape
                )
        )


        Spacer(
            modifier = Modifier.width(12.dp)
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
// SHARED BOTTOM NAVIGATION
// KEEP ONLY ONE COPY OF THIS FUNCTION IN YOUR WHOLE PROJECT
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
            selected = selectedScreen == "dashboard",
            onClick = onHomeClick
        )

        SitePulseNavigationItem(
            title = "Projects",
            icon = Icons.Outlined.BusinessCenter,
            selected = selectedScreen == "projects",
            onClick = onProjectsClick
        )

        SitePulseNavigationItem(
            title = "Messages",
            icon = Icons.Outlined.ChatBubbleOutline,
            selected = selectedScreen == "chat",
            onClick = onMessagesClick
        )

        SitePulseNavigationItem(
            title = "Tasks",
            icon = Icons.Outlined.TaskAlt,
            selected = selectedScreen == "tasks",
            onClick = onTasksClick
        )

        SitePulseNavigationItem(
            title = "Profile",
            icon = Icons.Outlined.Person,
            selected = selectedScreen == "profile",
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

            selectedIconColor = SitePulseOrange,

            selectedTextColor = SitePulseOrange,

            indicatorColor = Color(0xFFFFE6DC),

            unselectedIconColor = Color(0xFF777777),

            unselectedTextColor = Color(0xFF777777)
        )
    )
}