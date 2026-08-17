package com.example.capstonesample

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


private val ProfileBackground = Color(0xFFF0E1D8)
private val ProfileOrange = Color(0xFFF15A24)
private val ProfileGray = Color(0xFF777777)
private val ProfileDivider = Color(0xFFF0E8E4)


@Composable
fun ProfileScreen(
    onHomeClick: () -> Unit = {},
    onProjectsClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onTasksClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {

    Scaffold(
        containerColor = ProfileBackground,

        bottomBar = {
            ProfileBottomNavigationBar(
                selectedScreen = "profile",

                onHomeClick = onHomeClick,

                onProjectsClick = onProjectsClick,

                onMessagesClick = onMessagesClick,

                onTasksClick = onTasksClick,

                onProfileClick = {
                    // Already on profile
                }
            )
        }

    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ProfileBackground)
                .padding(horizontal = 14.dp)
        ) {

            Spacer(
                modifier = Modifier.height(18.dp)
            )


            // =====================================================
            // PROFILE CARD
            // =====================================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 18.dp,
                            bottom = 16.dp,
                            start = 16.dp,
                            end = 16.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // TODO API:
                    // Replace initials from logged-in user.

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                Color(0xFF263238),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "SA",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }


                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )


                    // TODO API:
                    // Replace with user.fullName

                    Text(
                        text = "Seth Andrew",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )


                    // TODO API:
                    // Replace with user.role

                    Text(
                        text = "Product Manager",
                        fontSize = 11.sp,
                        color = ProfileGray
                    )


                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )


                    HorizontalDivider(
                        color = ProfileDivider
                    )


                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )


                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        ProfileStat(
                            modifier = Modifier.weight(1f),
                            value = "12",
                            label = "Projects",
                            valueColor = ProfileOrange
                        )

                        ProfileStat(
                            modifier = Modifier.weight(1f),
                            value = "84",
                            label = "Completed"
                        )

                        ProfileStat(
                            modifier = Modifier.weight(1f),
                            value = "320h",
                            label = "Logged"
                        )
                    }
                }
            }


            Spacer(
                modifier = Modifier.height(14.dp)
            )


            // =====================================================
            // SETTINGS CARD
            // =====================================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
            ) {

                Column {

                    ProfileSettingRow(
                        icon = Icons.Outlined.Person,
                        title = "Account Settings",
                        onClick = {
                            // TODO:
                            // Open AccountSettingsScreen
                        }
                    )

                    ProfileDivider()

                    ProfileSettingRow(
                        icon = Icons.Outlined.Notifications,
                        title = "Notifications",
                        onClick = {
                            // TODO:
                            // Open NotificationSettingsScreen
                        }
                    )

                    ProfileDivider()

                    ProfileSettingRow(
                        icon = Icons.Outlined.Visibility,
                        title = "Appearance",
                        onClick = {
                            // TODO:
                            // Open Appearance screen
                        }
                    )

                    ProfileDivider()

                    ProfileSettingRow(
                        icon = Icons.Outlined.Storage,
                        title = "Data and Storage",
                        onClick = {
                            // TODO:
                            // Open storage settings
                        }
                    )

                    ProfileDivider()

                    ProfileSettingRow(
                        icon = Icons.Outlined.Security,
                        title = "Security",
                        onClick = {
                            // TODO:
                            // Open security settings
                        }
                    )

                    ProfileDivider()

                    ProfileSettingRow(
                        icon = Icons.Outlined.HelpOutline,
                        title = "Help and Support",
                        onClick = {
                            // TODO:
                            // Open help/support
                        }
                    )
                }
            }


            Spacer(
                modifier = Modifier.height(14.dp)
            )


            // =====================================================
            // LOGOUT BUTTON
            // =====================================================

            OutlinedButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Red
                ),
                border = BorderStroke(
                    1.dp,
                    Color.Red
                )
            ) {

                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Log Out",
                    modifier = Modifier.size(18.dp)
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = "Log Out",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }


            Spacer(
                modifier = Modifier.weight(1f)
            )
        }
    }
}


// ============================================================
// PROFILE STAT
// ============================================================

@Composable
private fun ProfileStat(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    valueColor: Color = Color.Black
) {

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )

        Spacer(
            modifier = Modifier.height(2.dp)
        )

        Text(
            text = label,
            fontSize = 9.sp,
            color = ProfileGray
        )
    }
}


// ============================================================
// SETTING ROW
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
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(19.dp),
            tint = ProfileGray
        )

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = Color.Black
        )

        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = ProfileGray
        )
    }
}


// ============================================================
// DIVIDER
// ============================================================

@Composable
private fun ProfileDivider() {

    HorizontalDivider(
        modifier = Modifier.padding(
            start = 46.dp
        ),
        color = ProfileDivider
    )
}


// ============================================================
// PROFILE NAVIGATION
// ============================================================

@Composable
private fun ProfileBottomNavigationBar(
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

        ProfileNavigationItem(
            title = "Home",
            icon = Icons.Outlined.Home,
            selected = selectedScreen == "dashboard",
            onClick = onHomeClick
        )

        ProfileNavigationItem(
            title = "Projects",
            icon = Icons.Outlined.BusinessCenter,
            selected = selectedScreen == "projects",
            onClick = onProjectsClick
        )

        ProfileNavigationItem(
            title = "Messages",
            icon = Icons.Outlined.ChatBubbleOutline,
            selected = selectedScreen == "chat",
            onClick = onMessagesClick
        )

        ProfileNavigationItem(
            title = "Tasks",
            icon = Icons.Outlined.TaskAlt,
            selected = selectedScreen == "tasks",
            onClick = onTasksClick
        )

        ProfileNavigationItem(
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
private fun RowScope.ProfileNavigationItem(
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

            selectedIconColor = ProfileOrange,

            selectedTextColor = ProfileOrange,

            indicatorColor = Color(0xFFFFE7DD),

            unselectedIconColor = Color.Gray,

            unselectedTextColor = Color.Gray
        )
    )
}