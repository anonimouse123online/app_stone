package com.example.capstonesample

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.GroupAdd
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstonesample.ui.theme.*

import com.example.capstonesample.data.api.RetrofitClient
import com.example.capstonesample.data.model.ConversationDto
import com.example.capstonesample.data.model.CreateConversationRequest
import com.example.capstonesample.data.model.UserSearchDto
import com.example.capstonesample.security.TokenManager

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


// ============================================================
// COLORS
// ============================================================

private val MessageBackground = Color(0xFFF6F8FA)
private val MessageOrange = Color(0xFFF15A24)
private val SearchBackground = Color.White
private val MessageGray = Color(0xFF64748B)


// ============================================================
// CHAT SCREEN
// ============================================================

@Composable
fun ChatScreen(

    onHomeClick: () -> Unit = {},
    onProjectsClick: () -> Unit = {},
    onTasksClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    userName: String = ""

) {

    val context = LocalContext.current
    val userSession = remember { TokenManager.getUserSession(context) }
    val resolvedUserName = remember(userName, userSession) {
        if (userName.isNotBlank()) userName
        else userSession?.fullName?.takeIf { it.isNotBlank() } ?: "User"
    }
    val initials = remember(resolvedUserName) {
        generateChatInitials(resolvedUserName)
    }

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

    var showCreateGroupDialog by remember {
        mutableStateOf(false)
    }

    var groupNameInput by remember {
        mutableStateOf("")
    }

    var selectedUserIdsForGroup by remember {
        mutableStateOf<Set<String>>(emptySet())
    }

    var isCreatingGroup by remember {
        mutableStateOf(false)
    }

    var availableGroupMembers by remember {
        mutableStateOf<List<UserSearchDto>>(emptyList())
    }

    var isLoadingMembers by remember {
        mutableStateOf(false)
    }

    suspend fun refreshConversations() {
        try {
            val response = RetrofitClient.api.getConversations()
            if (response.isSuccessful && response.body()?.success == true) {
                chatList = response.body()!!.conversations.map { conversation ->
                    val isGroup = conversation.isGroup == true
                    val displayName = if (isGroup) {
                        conversation.groupName?.takeIf { it.isNotBlank() } ?: "Group Chat"
                    } else {
                        conversation.fullName ?: conversation.email ?: "Unknown User"
                    }

                    ChatUser(
                        conversationId = conversation.conversationId,
                        userId = conversation.userId ?: "",
                        name = displayName,
                        lastMessage = conversation.lastMessage ?: "Start a conversation",
                        time = formatMessageTime(conversation.lastMessageTime),
                        unreadCount = conversation.unreadCount,
                        isGroup = isGroup,
                        memberCount = conversation.memberCount,
                        avatarColor = if (isGroup) Color(0xFFFFEAD6) else Color(0xFFDDEBFF),
                        avatarTextColor = if (isGroup) Color(0xFFF15A24) else Color(0xFF2962CC)
                    )
                }
            }
        } catch (_: Exception) {}
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

                            val isGroup = conversation.isGroup == true
                            val displayName = if (isGroup) {
                                conversation.groupName?.takeIf { it.isNotBlank() } ?: "Group Chat"
                            } else {
                                conversation.fullName ?: conversation.email ?: "Unknown User"
                            }

                            ChatUser(
                                conversationId = conversation.conversationId,
                                userId = conversation.userId ?: "",
                                name = displayName,
                                lastMessage = conversation.lastMessage ?: "Start a conversation",
                                time = formatMessageTime(conversation.lastMessageTime),
                                unreadCount = conversation.unreadCount,
                                isGroup = isGroup,
                                memberCount = conversation.memberCount,
                                avatarColor = if (isGroup) Color(0xFFFFEAD6) else Color(0xFFDDEBFF),
                                avatarTextColor = if (isGroup) Color(0xFFF15A24) else Color(0xFF2962CC)
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

            isGroup =
                selectedChat!!.isGroup,

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

            MessagesTopBar(
                initials = initials,
                onProfileClick = onProfileClick
            )
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
        },

        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    showCreateGroupDialog = true
                    scope.launch {
                        try {
                            isLoadingMembers = true
                            val resp = RetrofitClient.api.searchUsers("")
                            if (resp.isSuccessful) {
                                availableGroupMembers = resp.body()?.users ?: emptyList()
                            }
                        } catch (_: Exception) {
                        } finally {
                            isLoadingMembers = false
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.GroupAdd,
                        contentDescription = "New Group"
                    )
                },
                text = {
                    Text(
                        text = "New Group",
                        fontWeight = FontWeight.Bold
                    )
                },
                containerColor = MessageOrange,
                contentColor = Color.White
            )
        }

    ) { padding ->

        if (showCreateGroupDialog) {
            CreateGroupDialog(
                groupName = groupNameInput,
                onGroupNameChange = { groupNameInput = it },
                availableMembers = availableGroupMembers,
                selectedIds = selectedUserIdsForGroup,
                onToggleMember = { id ->
                    selectedUserIdsForGroup = if (selectedUserIdsForGroup.contains(id)) {
                        selectedUserIdsForGroup - id
                    } else {
                        selectedUserIdsForGroup + id
                    }
                },
                isLoading = isLoadingMembers,
                isCreating = isCreatingGroup,
                onDismiss = {
                    showCreateGroupDialog = false
                    groupNameInput = ""
                    selectedUserIdsForGroup = emptySet()
                },
                onCreate = {
                    scope.launch {
                        try {
                            isCreatingGroup = true
                            errorMessage = null
                            val response = RetrofitClient.api.createConversation(
                                CreateConversationRequest(
                                    isGroup = true,
                                    name = groupNameInput.trim(),
                                    receiverIds = selectedUserIdsForGroup.toList()
                                )
                            )
                            if (response.isSuccessful && response.body()?.success == true) {
                                val newConvId = response.body()?.conversationId ?: 0
                                val createdGroupName = groupNameInput.trim()
                                val count = selectedUserIdsForGroup.size + 1
                                showCreateGroupDialog = false
                                groupNameInput = ""
                                selectedUserIdsForGroup = emptySet()
                                refreshConversations()
                                selectedChat = ChatUser(
                                    conversationId = newConvId,
                                    userId = "",
                                    name = createdGroupName,
                                    lastMessage = "Group created",
                                    time = "",
                                    unreadCount = 0,
                                    isGroup = true,
                                    memberCount = count,
                                    avatarColor = Color(0xFFFFEAD6),
                                    avatarTextColor = Color(0xFFF15A24)
                                )
                            } else {
                                errorMessage = response.body()?.message ?: "Unable to create group."
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Failed to create group."
                        } finally {
                            isCreatingGroup = false
                        }
                    }
                }
            )
        }

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
                                MessageOrange,

                            unfocusedBorderColor =
                                CardBorderStroke,

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

private fun generateChatInitials(name: String): String {
    val words = name.trim().split(" ").filter { it.isNotBlank() }
    if (words.isEmpty()) return "SP"
    if (words.size == 1) return words[0].take(2).uppercase()
    return "${words.first().first().uppercaseChar()}${words.last().first().uppercaseChar()}"
}

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
private fun MessagesTopBar(
    initials: String = "SP",
    onProfileClick: () -> Unit = {}
) {

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
                    .clip(CircleShape)
                    .background(
                        Color(
                            0xFF263238
                        ),
                        CircleShape
                    )
                    .clickable { onProfileClick() },

                contentAlignment =
                    Alignment.Center

            ) {

                Text(
                    text =
                        initials,
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

        border = BorderStroke(1.dp, CardBorderStroke),

        elevation =
            CardDefaults
                .cardElevation(
                    defaultElevation =
                        1.dp
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
                    .size(42.dp)
                    .background(
                        chat.avatarColor,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (chat.isGroup) {
                    Icon(
                        imageVector = Icons.Outlined.Group,
                        contentDescription = "Group",
                        tint = chat.avatarTextColor,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        text = initials,
                        color = chat.avatarTextColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (chat.isGroup) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFEAD6)
                        ) {
                            Text(
                                text = if (chat.memberCount != null && chat.memberCount > 0) "GC • ${chat.memberCount}" else "GC",
                                color = Color(0xFFF15A24),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = if (chat.lastMessage.isBlank()) "No messages yet" else chat.lastMessage,
                    fontSize = 11.sp,
                    color = MessageGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                ),

        border = BorderStroke(1.dp, CardBorderStroke),

        elevation =
            CardDefaults
                .cardElevation(
                    defaultElevation =
                        1.dp
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


                val subInfo = if (!user.sharedProjects.isNullOrEmpty()) {
                    "Projects: ${user.sharedProjects.joinToString(", ")}"
                } else if (!user.role.isNullOrBlank()) {
                    user.role
                } else {
                    user.email ?: ""
                }

                Text(
                    text = subInfo,
                    fontSize = 11.sp,
                    color = MessageGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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

// ============================================================
// CREATE GROUP DIALOG
// ============================================================

@Composable
private fun CreateGroupDialog(
    groupName: String,
    onGroupNameChange: (String) -> Unit,
    availableMembers: List<UserSearchDto>,
    selectedIds: Set<String>,
    onToggleMember: (String) -> Unit,
    isLoading: Boolean,
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    var searchFilter by remember { mutableStateOf("") }
    val filteredMembers = remember(availableMembers, searchFilter) {
        if (searchFilter.isBlank()) availableMembers
        else availableMembers.filter {
            (it.fullName ?: "").contains(searchFilter, ignoreCase = true) ||
            (it.email ?: "").contains(searchFilter, ignoreCase = true) ||
            (it.role ?: "").contains(searchFilter, ignoreCase = true) ||
            (it.sharedProjects ?: emptyList()).any { p -> p.contains(searchFilter, ignoreCase = true) }
        }
    }

    val selectedMembersList = remember(availableMembers, selectedIds) {
        availableMembers.filter { selectedIds.contains(it.id) }
    }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.GroupAdd,
                    contentDescription = null,
                    tint = MessageOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "New Group Chat", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                // 1. Group Name Field
                OutlinedTextField(
                    value = groupName,
                    onValueChange = onGroupNameChange,
                    label = { Text("Group Name") },
                    placeholder = { Text("e.g. Project Site Team") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Selected Members Preview
                if (selectedMembersList.isNotEmpty()) {
                    Text(
                        text = "Added to group (${selectedMembersList.size})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MessageOrange
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        items(selectedMembersList, key = { it.id }) { member ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFFFEAD6),
                                border = BorderStroke(1.dp, Color(0xFFF8B595))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                                ) {
                                    Text(
                                        text = member.fullName ?: member.email ?: "User",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF9E3710)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { onToggleMember(member.id) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Remove",
                                            tint = Color(0xFF9E3710),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 3. Search members in list
                OutlinedTextField(
                    value = searchFilter,
                    onValueChange = { searchFilter = it },
                    placeholder = { Text("Filter members...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 4. List of available colleagues
                Text(
                    text = "Project Colleagues & Admins",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MessageOrange, modifier = Modifier.size(24.dp))
                    }
                } else if (filteredMembers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No members available to add.", fontSize = 12.sp, color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredMembers, key = { it.id }) { user ->
                            val isSelected = selectedIds.contains(user.id)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFFFFF4EC) else Color(0xFFF9F9F9),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleMember(user.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                if (isSelected) Color(0xFFF15A24) else Color(0xFFDDEBFF),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (user.fullName ?: user.email ?: "U").take(1).uppercase(),
                                            color = if (isSelected) Color.White else Color(0xFF2962CC),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = user.fullName ?: user.email ?: "User",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        val sub = if (!user.sharedProjects.isNullOrEmpty()) {
                                            user.sharedProjects.joinToString(", ")
                                        } else {
                                            user.role ?: user.email ?: ""
                                        }
                                        Text(
                                            text = sub,
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { onToggleMember(user.id) },
                                        colors = CheckboxDefaults.colors(checkedColor = MessageOrange)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCreate,
                enabled = groupName.isNotBlank() && selectedIds.isNotEmpty() && !isCreating,
                colors = ButtonDefaults.buttonColors(containerColor = MessageOrange)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create (${selectedIds.size})")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}