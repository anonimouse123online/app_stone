package com.example.capstonesample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TaskAlt
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


// ============================================================
// MODEL
// ============================================================

data class ChatUser(
    val name: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0,
    val avatarColor: Color,
    val avatarTextColor: Color
)


// ============================================================
// SAMPLE DATA
// TODO API:
// Replace this later with:
// GET /api/messages
// ============================================================

private val chatList = listOf(

    ChatUser(
        name = "Om Prakash",
        lastMessage = "Concrete delivery is delayed by 2 hours...",
        time = "10:45 AM",
        unreadCount = 1,
        avatarColor = Color(0xFFDDEBFF),
        avatarTextColor = Color(0xFF2962CC)
    ),

    ChatUser(
        name = "Nelisan Mando",
        lastMessage = "Safety reports for Sector 4 uploaded successfully...",
        time = "9:15 AM",
        avatarColor = Color(0xFFFFE1E1),
        avatarTextColor = Color(0xFFC84343)
    ),

    ChatUser(
        name = "Engineer Kurt",
        lastMessage = "Can you double check the beam alignment...",
        time = "Yesterday",
        unreadCount = 2,
        avatarColor = Color(0xFFDDF3FF),
        avatarTextColor = Color(0xFF1582B8)
    ),

    ChatUser(
        name = "HR Department",
        lastMessage = "Please submit your weekly hours log before Friday...",
        time = "Oct 24",
        avatarColor = Color(0xFFFFF0BD),
        avatarTextColor = Color(0xFFB16B1E)
    )
)


// ============================================================
// COLORS
// ============================================================

private val MessageBackground = Color(0xFFF0E1D8)
private val MessageOrange = Color(0xFFF15A24)
private val SearchBackground = Color(0xFFEADCD5)
private val MessageGray = Color(0xFF777777)


// ============================================================
// CHAT SCREEN
// ============================================================

@Composable
fun ChatScreen(

    onHomeClick: () -> Unit = {},

    onProjectsClick: () -> Unit = {},

    onTasksClick: () -> Unit = {},

    onProfileClick: () -> Unit = {},

    onChatClick: (ChatUser) -> Unit = {}

) {

    var searchQuery by remember {
        mutableStateOf("")
    }


    val filteredChats = chatList.filter { chat ->

        chat.name.contains(
            searchQuery,
            ignoreCase = true
        ) ||

                chat.lastMessage.contains(
                    searchQuery,
                    ignoreCase = true
                )
    }


    Scaffold(

        containerColor = MessageBackground,

        topBar = {
            MessagesTopBar()
        },

        bottomBar = {

            MessagesBottomNavigationBar(

                selectedScreen = "chat",

                onHomeClick = onHomeClick,

                onProjectsClick = onProjectsClick,

                onMessagesClick = {
                    // Already on messages screen
                },

                onTasksClick = onTasksClick,

                onProfileClick = onProfileClick
            )
        }

    ) { padding ->


        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(padding)

        ) {


            // =================================================
            // HEADER
            // =================================================

            Column(

                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 18.dp,
                        bottom = 12.dp
                    )

            ) {

                Text(
                    text = "Messages",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )


                Spacer(
                    modifier = Modifier.height(10.dp)
                )


                // SEARCH BAR

                OutlinedTextField(

                    value = searchQuery,

                    onValueChange = {
                        searchQuery = it
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),

                    placeholder = {

                        Text(
                            text = "Search messages or team members...",
                            fontSize = 13.sp,
                            color = MessageGray
                        )
                    },

                    leadingIcon = {

                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = MessageGray,
                            modifier = Modifier.size(20.dp)
                        )
                    },

                    singleLine = true,

                    shape = RoundedCornerShape(12.dp),

                    colors = OutlinedTextFieldDefaults.colors(

                        focusedContainerColor = SearchBackground,

                        unfocusedContainerColor = SearchBackground,

                        focusedBorderColor = Color.Transparent,

                        unfocusedBorderColor = Color.Transparent,

                        cursorColor = MessageOrange,

                        focusedTextColor = Color.Black,

                        unfocusedTextColor = Color.Black
                    )
                )
            }


            // =================================================
            // MESSAGE LIST
            // =================================================

            LazyColumn(

                modifier = Modifier
                    .fillMaxSize()
                    .background(MessageBackground),

                contentPadding = PaddingValues(
                    horizontal = 12.dp,
                    vertical = 12.dp
                ),

                verticalArrangement = Arrangement.spacedBy(8.dp)

            ) {


                items(
                    items = filteredChats,
                    key = { it.name }
                ) { chat ->


                    MessageCard(
                        chat = chat,
                        onClick = {
                            onChatClick(chat)
                        }
                    )
                }


                item {

                    Spacer(
                        modifier = Modifier.height(20.dp)
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
private fun MessagesTopBar() {

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

                // TODO API:
                // Replace with logged-in user's initials.

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
// MESSAGE CARD
// ============================================================

@Composable
private fun MessageCard(
    chat: ChatUser,
    onClick: () -> Unit
) {

    val initials = chat.name
        .split(" ")
        .take(2)
        .mapNotNull { word ->
            word.firstOrNull()?.uppercase()
        }
        .joinToString("")


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


        Row(

            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                ),

            verticalAlignment = Alignment.CenterVertically

        ) {


            // =================================================
            // AVATAR
            // =================================================

            Box(

                modifier = Modifier
                    .size(42.dp)
                    .background(
                        chat.avatarColor,
                        CircleShape
                    ),

                contentAlignment = Alignment.Center

            ) {

                Text(
                    text = initials,
                    color = chat.avatarTextColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }


            Spacer(
                modifier = Modifier.width(12.dp)
            )


            // =================================================
            // NAME + MESSAGE
            // =================================================

            Column(
                modifier = Modifier.weight(1f)
            ) {


                Text(
                    text = chat.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )


                Spacer(
                    modifier = Modifier.height(3.dp)
                )


                Text(
                    text = chat.lastMessage,
                    fontSize = 11.sp,
                    color = MessageGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }


            Spacer(
                modifier = Modifier.width(8.dp)
            )


            // =================================================
            // TIME + UNREAD COUNT
            // =================================================

            Column(
                horizontalAlignment = Alignment.End
            ) {

                Text(
                    text = chat.time,
                    fontSize = 10.sp,
                    color = MessageGray
                )


                if (chat.unreadCount > 0) {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )


                    Box(

                        modifier = Modifier
                            .size(19.dp)
                            .background(
                                MessageOrange,
                                CircleShape
                            ),

                        contentAlignment = Alignment.Center

                    ) {

                        Text(
                            text = chat.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


// ============================================================
// MESSAGES BOTTOM NAVIGATION
// Unique name so it will NOT conflict with Dashboard/Projects
// ============================================================

@Composable
private fun MessagesBottomNavigationBar(

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


        MessagesNavigationItem(
            title = "Home",
            icon = Icons.Outlined.Home,
            selected = selectedScreen == "dashboard",
            onClick = onHomeClick
        )


        MessagesNavigationItem(
            title = "Projects",
            icon = Icons.Outlined.BusinessCenter,
            selected = selectedScreen == "projects",
            onClick = onProjectsClick
        )


        MessagesNavigationItem(
            title = "Messages",
            icon = Icons.Outlined.ChatBubbleOutline,
            selected = selectedScreen == "chat",
            onClick = onMessagesClick
        )


        MessagesNavigationItem(
            title = "Tasks",
            icon = Icons.Outlined.TaskAlt,
            selected = selectedScreen == "tasks",
            onClick = onTasksClick
        )


        MessagesNavigationItem(
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
private fun RowScope.MessagesNavigationItem(

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

            selectedIconColor = MessageOrange,

            selectedTextColor = MessageOrange,

            indicatorColor = Color(0xFFFFE7DD),

            unselectedIconColor = Color.Gray,

            unselectedTextColor = Color.Gray
        )
    )
}