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

import com.example.capstonesample.data.api.RetrofitClient
import com.example.capstonesample.data.model.ConversationDto
import com.example.capstonesample.data.model.CreateConversationRequest
import com.example.capstonesample.data.model.UserSearchDto

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


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
    onProfileClick: () -> Unit = {}

) {

    val scope = rememberCoroutineScope()

    var searchedUsers by remember {
        mutableStateOf<List<UserSearchDto>>(emptyList())
    }

    var isSearchingUsers by remember {
        mutableStateOf(false)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    var selectedChat by remember {
        mutableStateOf<ChatUser?>(null)
    }

    var chatList by remember {
        mutableStateOf<List<ChatUser>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }


    // ============================================================
    // SEARCH USERS
    //
    // GET /users/search?q=
    // ============================================================

    LaunchedEffect(searchQuery) {

        val query =
            searchQuery.trim()

        if (query.length < 2) {

            searchedUsers =
                emptyList()

            isSearchingUsers =
                false

            return@LaunchedEffect
        }

        delay(350)

        try {

            isSearchingUsers = true

            val response =
                RetrofitClient.api.searchUsers(
                    query
                )

            if (response.isSuccessful) {

                searchedUsers =
                    response.body()
                        ?.users
                        ?: emptyList()

            } else {

                searchedUsers =
                    emptyList()
            }

        } catch (e: Exception) {

            e.printStackTrace()

            searchedUsers =
                emptyList()

        } finally {

            isSearchingUsers =
                false
        }
    }


    // ============================================================
    // LOAD CONVERSATIONS
    //
    // GET /messages/conversations
    // ============================================================

    LaunchedEffect(Unit) {

        isLoading = true
        errorMessage = null

        try {

            val response =
                RetrofitClient.api
                    .getConversations()

            if (response.isSuccessful) {

                val body =
                    response.body()

                if (
                    body != null &&
                    body.success
                ) {

                    chatList =
                        body.conversations.map {
                                conversation: ConversationDto ->

                            ChatUser(

                                conversationId =
                                    conversation.conversationId,

                                userId =
                                    conversation.userId
                                        ?: "",

                                name =
                                    conversation.fullName
                                        ?: conversation.email
                                        ?: "Unknown User",

                                lastMessage =
                                    conversation.lastMessage
                                        ?: "Start a conversation",

                                time =
                                    formatMessageTime(
                                        conversation.lastMessageTime
                                    ),

                                unreadCount =
                                    conversation.unreadCount,

                                avatarColor =
                                    Color(0xFFDDEBFF),

                                avatarTextColor =
                                    Color(0xFF2962CC)
                            )
                        }

                } else {

                    errorMessage =
                        "Unable to load conversations."
                }

            } else {

                errorMessage =
                    "Server error: ${response.code()} - " +
                            "${response.errorBody()?.string()}"
            }

        } catch (e: Exception) {

            errorMessage =
                e.message
                    ?: "Unable to connect to server."

            e.printStackTrace()

        } finally {

            isLoading =
                false
        }
    }


    // ============================================================
    // OPEN SELECTED CHAT
    // ============================================================

    if (selectedChat != null) {

        MessageDetailScreen(

            conversationId =
                selectedChat!!.conversationId,

            userName =
                selectedChat!!.name,

            onBackClick = {

                selectedChat =
                    null
            }
        )

        return
    }


    // ============================================================
    // FILTER EXISTING CONVERSATIONS
    // ============================================================

    val filteredChats =
        chatList.filter { chat ->

            chat.name.contains(
                searchQuery,
                ignoreCase = true
            ) ||

                    chat.lastMessage.contains(
                        searchQuery,
                        ignoreCase = true
                    )
        }


    // ============================================================
    // UI
    // ============================================================

    Scaffold(

        containerColor =
            MessageBackground,

        topBar = {

            MessagesTopBar()
        },

        bottomBar = {

            MessagesBottomNavigationBar(

                onHomeClick =
                    onHomeClick,

                onProjectsClick =
                    onProjectsClick,

                onMessagesClick = {},

                onTasksClick =
                    onTasksClick,

                onProfileClick =
                    onProfileClick
            )
        }

    ) { padding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(padding)

        ) {


            // ====================================================
            // HEADER
            // ====================================================

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
                    modifier =
                        Modifier.height(10.dp)
                )

                OutlinedTextField(

                    value =
                        searchQuery,

                    onValueChange = {

                        searchQuery =
                            it

                        errorMessage =
                            null
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),

                    placeholder = {

                        Text(
                            text =
                                "Search users or conversations...",
                            fontSize = 13.sp,
                            color = MessageGray
                        )
                    },

                    leadingIcon = {

                        Icon(
                            imageVector =
                                Icons.Outlined.Search,

                            contentDescription =
                                "Search",

                            tint =
                                MessageGray,

                            modifier =
                                Modifier.size(20.dp)
                        )
                    },

                    singleLine =
                        true,

                    shape =
                        RoundedCornerShape(12.dp),

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
                                MessageOrange,

                            focusedTextColor =
                                Color.Black,

                            unfocusedTextColor =
                                Color.Black
                        )
                )
            }


            // ====================================================
            // INITIAL LOADING
            // ====================================================

            if (isLoading) {

                Box(

                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MessageBackground
                        ),

                    contentAlignment =
                        Alignment.Center

                ) {

                    CircularProgressIndicator(
                        color =
                            MessageOrange
                    )
                }

                return@Column
            }


            // ====================================================
            // ERROR
            // ====================================================

            if (
                errorMessage != null &&
                searchQuery.isBlank()
            ) {

                Box(

                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MessageBackground
                        )
                        .padding(30.dp),

                    contentAlignment =
                        Alignment.Center

                ) {

                    Column(

                        horizontalAlignment =
                            Alignment.CenterHorizontally

                    ) {

                        Text(
                            text =
                                "Unable to load messages",
                            fontWeight =
                                FontWeight.Bold,
                            fontSize =
                                16.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                errorMessage ?: "",
                            color =
                                MessageGray,
                            fontSize =
                                12.sp
                        )
                    }
                }

                return@Column
            }


            // ====================================================
            // SEARCH MODE
            // ====================================================

            if (
                searchQuery.isNotBlank()
            ) {

                LazyColumn(

                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MessageBackground
                        ),

                    contentPadding =
                        PaddingValues(
                            horizontal = 12.dp,
                            vertical = 12.dp
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)

                ) {


                    // ==============================================
                    // EXISTING CONVERSATIONS
                    // ==============================================

                    if (
                        filteredChats.isNotEmpty()
                    ) {

                        item {

                            Text(
                                text =
                                    "Conversations",
                                fontWeight =
                                    FontWeight.Bold,
                                modifier =
                                    Modifier.padding(
                                        6.dp
                                    )
                            )
                        }


                        items(

                            items =
                                filteredChats,

                            key = {
                                "chat_${it.conversationId}"
                            }

                        ) { chat ->

                            MessageCard(

                                chat =
                                    chat,

                                onClick = {

                                    selectedChat =
                                        chat
                                }
                            )
                        }
                    }


                    // ==============================================
                    // USER RESULTS
                    // ==============================================

                    if (
                        searchedUsers.isNotEmpty()
                    ) {

                        item {

                            Text(
                                text =
                                    "People",
                                fontWeight =
                                    FontWeight.Bold,
                                modifier =
                                    Modifier.padding(
                                        top = 12.dp,
                                        start = 6.dp,
                                        bottom = 6.dp
                                    )
                            )
                        }


                        items(

                            items =
                                searchedUsers,

                            key = {
                                "user_${it.id}"
                            }

                        ) { user ->

                            UserSearchCard(

                                user =
                                    user,

                                onClick = {

                                    scope.launch {

                                        try {

                                            errorMessage =
                                                null

                                            val response =
                                                RetrofitClient.api
                                                    .createConversation(

                                                        CreateConversationRequest(
                                                            receiverId =
                                                                user.id
                                                        )
                                                    )


                                            if (
                                                response.isSuccessful
                                            ) {

                                                val body =
                                                    response.body()

                                                if (
                                                    body != null &&
                                                    body.success
                                                ) {

                                                    val newChat =
                                                        ChatUser(

                                                            conversationId =
                                                                body.conversationId,

                                                            userId =
                                                                user.id,

                                                            name =
                                                                user.fullName
                                                                    ?: user.email
                                                                    ?: "User",

                                                            lastMessage =
                                                                "Start a conversation",

                                                            time =
                                                                "",

                                                            unreadCount =
                                                                0,

                                                            avatarColor =
                                                                Color(
                                                                    0xFFDDEBFF
                                                                ),

                                                            avatarTextColor =
                                                                Color(
                                                                    0xFF2962CC
                                                                )
                                                        )


                                                    // Add conversation locally
                                                    if (
                                                        chatList.none {
                                                            it.conversationId ==
                                                                    newChat.conversationId
                                                        }
                                                    ) {

                                                        chatList =
                                                            listOf(
                                                                newChat
                                                            ) +
                                                                    chatList
                                                    }


                                                    // Clear search
                                                    searchQuery =
                                                        ""

                                                    searchedUsers =
                                                        emptyList()


                                                    // OPEN CHAT
                                                    selectedChat =
                                                        newChat

                                                } else {

                                                    errorMessage =
                                                        body?.message
                                                            ?: "Unable to start conversation."
                                                }

                                            } else {

                                                errorMessage =
                                                    "Unable to start conversation: " +
                                                            "${response.code()} - " +
                                                            "${response.errorBody()?.string()}"
                                            }

                                        } catch (
                                            e: Exception
                                        ) {

                                            e.printStackTrace()

                                            errorMessage =
                                                e.message
                                                    ?: "Unable to start conversation."
                                        }
                                    }
                                }
                            )
                        }
                    }


                    // ==============================================
                    // SEARCHING
                    // ==============================================

                    if (
                        isSearchingUsers
                    ) {

                        item {

                            Box(

                                modifier =
                                    Modifier.fillMaxWidth(),

                                contentAlignment =
                                    Alignment.Center

                            ) {

                                CircularProgressIndicator(

                                    modifier =
                                        Modifier.size(
                                            24.dp
                                        ),

                                    color =
                                        MessageOrange
                                )
                            }
                        }
                    }


                    // ==============================================
                    // NO RESULTS
                    // ==============================================

                    if (
                        filteredChats.isEmpty() &&
                        searchedUsers.isEmpty() &&
                        !isSearchingUsers
                    ) {

                        item {

                            Text(
                                text =
                                    "No users found.",
                                color =
                                    MessageGray,
                                modifier =
                                    Modifier.padding(
                                        20.dp
                                    )
                            )
                        }
                    }


                    // ==============================================
                    // START CONVERSATION ERROR
                    // ==============================================

                    if (
                        errorMessage != null
                    ) {

                        item {

                            Text(
                                text =
                                    errorMessage
                                        ?: "",
                                color =
                                    Color(
                                        0xFFD32F2F
                                    ),
                                fontSize =
                                    12.sp,
                                modifier =
                                    Modifier.padding(
                                        12.dp
                                    )
                            )
                        }
                    }
                }

                return@Column
            }


            // ====================================================
            // NO CONVERSATIONS
            // ====================================================

            if (
                chatList.isEmpty()
            ) {

                Box(

                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MessageBackground
                        ),

                    contentAlignment =
                        Alignment.Center

                ) {

                    Column(

                        horizontalAlignment =
                            Alignment.CenterHorizontally

                    ) {

                        Text(
                            text =
                                "No conversations yet.",
                            color =
                                MessageGray
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    5.dp
                                )
                        )

                        Text(
                            text =
                                "Search for a user above to start a conversation.",
                            fontSize =
                                11.sp,
                            color =
                                MessageGray
                        )
                    }
                }

                return@Column
            }


            // ====================================================
            // NORMAL CONVERSATION LIST
            // ====================================================

            LazyColumn(

                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MessageBackground
                    ),

                contentPadding =
                    PaddingValues(
                        horizontal = 12.dp,
                        vertical = 12.dp
                    ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )

            ) {

                items(

                    items =
                        chatList,

                    key = {
                        it.conversationId
                    }

                ) { chat ->

                    MessageCard(

                        chat =
                            chat,

                        onClick = {

                            selectedChat =
                                chat
                        }
                    )
                }

                item {

                    Spacer(
                        modifier =
                            Modifier.height(
                                20.dp
                            )
                    )
                }
            }
        }
    }
}


// ============================================================
// FORMAT TIME
// ============================================================

private fun formatMessageTime(
    date: String?
): String {

    if (
        date.isNullOrBlank()
    ) {

        return ""
    }

    return try {

        date
            .replace(
                "T",
                " "
            )
            .take(16)

    } catch (
        _: Exception
    ) {

        date
    }
}


// ============================================================
// TOP BAR
// ============================================================

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
private fun MessagesTopBar() {

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

                modifier = Modifier
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
// MESSAGE CARD
// ============================================================

@Composable
private fun MessageCard(

    chat: ChatUser,

    onClick: () -> Unit

) {

    val initials =
        chat.name
            .split(" ")
            .take(2)
            .mapNotNull {
                    word ->

                word.firstOrNull()
                    ?.uppercase()
            }
            .joinToString("")


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

        Row(

            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        14.dp,
                    vertical =
                        12.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            Box(

                modifier = Modifier
                    .size(
                        42.dp
                    )
                    .background(
                        chat.avatarColor,
                        CircleShape
                    ),

                contentAlignment =
                    Alignment.Center

            ) {

                Text(
                    text =
                        initials,
                    color =
                        chat.avatarTextColor,
                    fontSize =
                        13.sp,
                    fontWeight =
                        FontWeight.Bold
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
                        chat.name,
                    fontSize =
                        14.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        Color.Black
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            3.dp
                        )
                )


                Text(
                    text =
                        chat.lastMessage,
                    fontSize =
                        11.sp,
                    color =
                        MessageGray,
                    maxLines =
                        1,
                    overflow =
                        TextOverflow.Ellipsis
                )
            }


            Spacer(
                modifier =
                    Modifier.width(
                        8.dp
                    )
            )


            Column(

                horizontalAlignment =
                    Alignment.End

            ) {

                Text(
                    text =
                        chat.time,
                    fontSize =
                        10.sp,
                    color =
                        MessageGray
                )


                if (
                    chat.unreadCount >
                    0
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )


                    Box(

                        modifier = Modifier
                            .size(
                                19.dp
                            )
                            .background(
                                MessageOrange,
                                CircleShape
                            ),

                        contentAlignment =
                            Alignment.Center

                    ) {

                        Text(
                            text =
                                chat.unreadCount
                                    .toString(),
                            color =
                                Color.White,
                            fontSize =
                                9.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


// ============================================================
// SEARCHED USER CARD
// ============================================================

@Composable
private fun UserSearchCard(

    user: UserSearchDto,

    onClick: () -> Unit

) {

    val name =
        user.fullName
            ?: user.email
            ?: "User"


    val initials =
        name
            .split(" ")
            .take(2)
            .mapNotNull {

                it.firstOrNull()
                    ?.uppercase()
            }
            .joinToString("")


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
            CardDefaults
                .cardColors(
                    containerColor =
                        Color.White
                )

    ) {

        Row(

            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    14.dp
                ),

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
                            0xFFDDEBFF
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
                        Color(
                            0xFF2962CC
                        ),
                    fontWeight =
                        FontWeight.Bold
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
                        name,
                    fontWeight =
                        FontWeight.Bold
                )


                Text(
                    text =
                        user.email
                            ?: "",
                    fontSize =
                        11.sp,
                    color =
                        MessageGray
                )
            }


            Text(
                text =
                    "Message",
                color =
                    MessageOrange,
                fontSize =
                    12.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


// ============================================================
// BOTTOM NAVIGATION
// ============================================================

@Composable
private fun MessagesBottomNavigationBar(

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


        MessagesNavigationItem(

            title =
                "Home",

            icon =
                Icons.Outlined.Home,

            selected =
                false,

            onClick =
                onHomeClick
        )


        MessagesNavigationItem(

            title =
                "Projects",

            icon =
                Icons.Outlined.BusinessCenter,

            selected =
                false,

            onClick =
                onProjectsClick
        )


        MessagesNavigationItem(

            title =
                "Messages",

            icon =
                Icons.Outlined.ChatBubbleOutline,

            selected =
                true,

            onClick =
                onMessagesClick
        )


        MessagesNavigationItem(

            title =
                "Tasks",

            icon =
                Icons.Outlined.TaskAlt,

            selected =
                false,

            onClick =
                onTasksClick
        )


        MessagesNavigationItem(

            title =
                "Profile",

            icon =
                Icons.Outlined.Person,

            selected =
                false,

            onClick =
                onProfileClick
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
                        MessageOrange,

                    selectedTextColor =
                        MessageOrange,

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