package com.example.capstonesample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.capstonesample.data.api.RetrofitClient
import com.example.capstonesample.data.model.MessageDto
import com.example.capstonesample.data.model.SendMessageRequest

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


// ============================================================
// MESSAGE DETAIL SCREEN
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailScreen(

    conversationId: Int,

    userName: String,

    onBackClick: () -> Unit

) {

    val scope =
        rememberCoroutineScope()

    val listState =
        rememberLazyListState()


    // ============================================================
    // STATE
    // ============================================================

    var messageText by remember {
        mutableStateOf("")
    }

    var messages by remember {
        mutableStateOf<List<MessageDto>>(
            emptyList()
        )
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var isSending by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }


    // ============================================================
    // LOAD MESSAGES FUNCTION
    // ============================================================

    suspend fun loadMessages() {

        try {

            val response =
                RetrofitClient.api
                    .getMessages(
                        conversationId
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

                    messages =
                        body.messages

                } else {

                    errorMessage =
                        "Unable to load conversation."
                }

            } else {

                errorMessage =
                    "Failed to load messages: " +
                            "${response.code()} - " +
                            "${response.errorBody()?.string()}"
            }

        } catch (e: Exception) {

            e.printStackTrace()

            errorMessage =
                e.message
                    ?: "Unable to connect to server."

        } finally {

            isLoading =
                false
        }
    }


    // ============================================================
    // INITIAL LOAD
    // ============================================================

    LaunchedEffect(
        conversationId
    ) {

        isLoading =
            true

        errorMessage =
            null

        loadMessages()
    }


    // ============================================================
    // TEMPORARY LIVE REFRESH
    //
    // Refresh every 2 seconds.
    //
    // Later:
    // replace this with Socket.IO new_message event.
    // ============================================================

    LaunchedEffect(
        conversationId
    ) {

        while (true) {

            delay(
                2000
            )

            try {

                val response =
                    RetrofitClient.api
                        .getMessages(
                            conversationId
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

                        messages =
                            body.messages
                    }
                }

            } catch (
                _: Exception
            ) {

                // Silent refresh failure.
                // Do not remove existing messages.
            }
        }
    }


    // ============================================================
    // AUTO SCROLL
    // ============================================================

    LaunchedEffect(
        messages.size
    ) {

        if (
            messages.isNotEmpty()
        ) {

            listState.animateScrollToItem(
                messages.lastIndex
            )
        }
    }


    // ============================================================
    // UI
    // ============================================================

    Scaffold(

        containerColor =
            Color(
                0xFFF0E1D8
            ),

        // ========================================================
        // TOP BAR
        // ========================================================

        topBar = {

            TopAppBar(

                navigationIcon = {

                    IconButton(
                        onClick =
                            onBackClick
                    ) {

                        Icon(

                            imageVector =
                                Icons
                                    .AutoMirrored
                                    .Outlined
                                    .ArrowBack,

                            contentDescription =
                                "Back"
                        )
                    }
                },

                title = {

                    Column {

                        Text(

                            text =
                                userName,

                            fontWeight =
                                FontWeight.Bold,

                            fontSize =
                                17.sp
                        )

                        Text(

                            text =
                                "Conversation",

                            fontSize =
                                11.sp,

                            color =
                                Color.Gray
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
        },


        // ========================================================
        // MESSAGE INPUT
        // ========================================================

        bottomBar = {

            Surface(

                color =
                    Color.White,

                shadowElevation =
                    5.dp

            ) {

                Row(

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            8.dp
                        ),

                    verticalAlignment =
                        Alignment.CenterVertically

                ) {


                    // =================================================
                    // ATTACH FILE
                    // =================================================

                    IconButton(

                        onClick = {

                            // TODO NEXT:
                            //
                            // POST /messages/upload
                            //
                            // Support:
                            // - image
                            // - camera
                            // - PDF
                            // - document
                        }

                    ) {

                        Icon(

                            imageVector =
                                Icons
                                    .Outlined
                                    .AttachFile,

                            contentDescription =
                                "Attach file",

                            tint =
                                Color(
                                    0xFFF15A24
                                )
                        )
                    }


                    // =================================================
                    // MESSAGE INPUT
                    // =================================================

                    OutlinedTextField(

                        value =
                            messageText,

                        onValueChange = {

                            messageText =
                                it
                        },

                        modifier =
                            Modifier.weight(
                                1f
                            ),

                        placeholder = {

                            Text(
                                text =
                                    "Type a message..."
                            )
                        },

                        shape =
                            RoundedCornerShape(
                                22.dp
                            ),

                        maxLines =
                            4,

                        enabled =
                            !isSending
                    )


                    Spacer(

                        modifier =
                            Modifier.width(
                                6.dp
                            )
                    )


                    // =================================================
                    // SEND BUTTON
                    // =================================================

                    IconButton(

                        enabled =
                            messageText
                                .isNotBlank() &&
                                    !isSending,

                        onClick = {

                            val textToSend =
                                messageText
                                    .trim()

                            if (
                                textToSend
                                    .isBlank()
                            ) {

                                return@IconButton
                            }


                            scope.launch {

                                try {

                                    isSending =
                                        true

                                    errorMessage =
                                        null


                                    // =================================
                                    // POST /messages
                                    // =================================

                                    val response =
                                        RetrofitClient.api
                                            .sendMessage(

                                                SendMessageRequest(

                                                    conversationId =
                                                        conversationId,

                                                    message =
                                                        textToSend
                                                )
                                            )


                                    if (
                                        response
                                            .isSuccessful
                                    ) {

                                        // Clear field
                                        messageText =
                                            ""


                                        // Reload immediately
                                        loadMessages()

                                    } else {

                                        errorMessage =
                                            "Failed to send message: " +
                                                    "${response.code()} - " +
                                                    "${response.errorBody()?.string()}"
                                    }

                                } catch (
                                    e: Exception
                                ) {

                                    e.printStackTrace()

                                    errorMessage =
                                        e.message
                                            ?: "Unable to send message."

                                } finally {

                                    isSending =
                                        false
                                }
                            }
                        }

                    ) {

                        Box(

                            modifier = Modifier
                                .size(
                                    44.dp
                                )
                                .background(

                                    color =
                                        if (
                                            messageText
                                                .isNotBlank() &&
                                            !isSending
                                        ) {

                                            Color(
                                                0xFFF15A24
                                            )

                                        } else {

                                            Color(
                                                0xFFBDBDBD
                                            )
                                        },

                                    shape =
                                        CircleShape
                                ),

                            contentAlignment =
                                Alignment.Center

                        ) {

                            if (
                                isSending
                            ) {

                                CircularProgressIndicator(

                                    modifier =
                                        Modifier.size(
                                            20.dp
                                        ),

                                    color =
                                        Color.White,

                                    strokeWidth =
                                        2.dp
                                )

                            } else {

                                Icon(

                                    imageVector =
                                        Icons
                                            .Outlined
                                            .Send,

                                    contentDescription =
                                        "Send",

                                    tint =
                                        Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

    ) { padding ->


        // ============================================================
        // LOADING
        // ============================================================

        if (
            isLoading
        ) {

            Box(

                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        padding
                    ),

                contentAlignment =
                    Alignment.Center

            ) {

                CircularProgressIndicator(

                    color =
                        Color(
                            0xFFF15A24
                        )
                )
            }

            return@Scaffold
        }


        // ============================================================
        // CONTENT
        // ============================================================

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(
                    padding
                )

        ) {


            // ========================================================
            // ERROR
            // ========================================================

            if (
                errorMessage != null
            ) {

                Text(

                    text =
                        errorMessage
                            ?: "",

                    color =
                        Color(
                            0xFFD32F2F
                        ),

                    fontSize =
                        11.sp,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                Color.White
                            )
                            .padding(
                                8.dp
                            )
                )
            }


            // ========================================================
            // EMPTY CONVERSATION
            // ========================================================

            if (
                messages.isEmpty()
            ) {

                Box(

                    modifier =
                        Modifier.fillMaxSize(),

                    contentAlignment =
                        Alignment.Center

                ) {

                    Column(

                        horizontalAlignment =
                            Alignment
                                .CenterHorizontally

                    ) {

                        Text(

                            text =
                                "No messages yet.",

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                Color.Gray
                        )

                        Spacer(

                            modifier =
                                Modifier.height(
                                    4.dp
                                )
                        )

                        Text(

                            text =
                                "Send a message to start the conversation.",

                            fontSize =
                                11.sp,

                            color =
                                Color.Gray
                        )
                    }
                }

                return@Column
            }


            // ========================================================
            // REAL MESSAGES
            // ========================================================

            LazyColumn(

                state =
                    listState,

                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal =
                            14.dp
                    ),

                contentPadding =
                    PaddingValues(
                        vertical =
                            16.dp
                    ),

                verticalArrangement =
                    Arrangement
                        .spacedBy(
                            10.dp
                        )

            ) {

                items(

                    items =
                        messages,

                    key = {
                        it.id
                    }

                ) { message ->


                    // ================================================
                    // The backend should return:
                    //
                    // is_mine = true / false
                    //
                    // so Android knows which side to display.
                    // ================================================

                    val isMine =
                        message.isMine


                    Row(

                        modifier =
                            Modifier
                                .fillMaxWidth(),

                        horizontalArrangement =
                            if (
                                isMine
                            ) {

                                Arrangement.End

                            } else {

                                Arrangement.Start
                            }

                    ) {

                        Column(

                            horizontalAlignment =
                                if (
                                    isMine
                                ) {

                                    Alignment.End

                                } else {

                                    Alignment.Start
                                }

                        ) {

                            Surface(

                                color =
                                    if (
                                        isMine
                                    ) {

                                        Color(
                                            0xFFF15A24
                                        )

                                    } else {

                                        Color.White
                                    },

                                shape =
                                    RoundedCornerShape(
                                        15.dp
                                    )

                            ) {

                                Text(

                                    text =
                                        message.messageText
                                            ?: "",

                                    color =
                                        if (
                                            isMine
                                        ) {

                                            Color.White

                                        } else {

                                            Color.Black
                                        },

                                    fontSize =
                                        14.sp,

                                    modifier =
                                        Modifier.padding(
                                            horizontal =
                                                14.dp,
                                            vertical =
                                                10.dp
                                        )
                                )
                            }


                            Spacer(

                                modifier =
                                    Modifier.height(
                                        3.dp
                                    )
                            )


                            Text(

                                text =
                                    formatChatTime(
                                        message.createdAt
                                    ),

                                fontSize =
                                    9.sp,

                                color =
                                    Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}


// ============================================================
// FORMAT CHAT TIME
// ============================================================

private fun formatChatTime(
    date: String?
): String {

    if (
        date.isNullOrBlank()
    ) {

        return ""
    }

    return try {

        if (
            date.contains("T")
        ) {

            date
                .substringAfter("T")
                .take(5)

        } else {

            date
                .takeLast(8)
                .take(5)
        }

    } catch (
        _: Exception
    ) {

        ""
    }
}