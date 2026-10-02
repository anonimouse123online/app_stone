package com.example.capstonesample

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.capstonesample.data.api.RetrofitClient
import com.example.capstonesample.data.model.MessageDto
import com.example.capstonesample.data.model.SendMessageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

// ============================================================
// MESSAGE DETAIL SCREEN
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailScreen(
    conversationId: Int,
    userName: String,
    isGroup: Boolean = false,
    onBackClick: () -> Unit
) {
    BackHandler {
        onBackClick()
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // ============================================================
    // STATE
    // ============================================================

    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<MessageDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ============================================================
    // LOAD MESSAGES FUNCTION
    // ============================================================

    suspend fun loadMessages() {
        try {
            val response = RetrofitClient.api.getMessages(conversationId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    messages = body.messages
                } else {
                    errorMessage = "Unable to load conversation."
                }
            } else {
                errorMessage = "Failed to load messages: ${response.code()}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = e.message ?: "Unable to connect to server."
        } finally {
            isLoading = false
        }
    }

    // ============================================================
    // FILE PICKER LAUNCHER FOR ATTACHMENTS
    // ============================================================

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    isSending = true
                    errorMessage = null
                    val part = uriToMultipartPart(context, uri, "file")
                    if (part != null) {
                        val convIdPart = conversationId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                        val textPart = if (messageText.isNotBlank()) {
                            messageText.trim().toRequestBody("text/plain".toMediaTypeOrNull())
                        } else null

                        val response = RetrofitClient.api.sendAttachment(convIdPart, textPart, part)
                        if (response.isSuccessful && response.body()?.success == true) {
                            messageText = ""
                            loadMessages()
                        } else {
                            errorMessage = "Failed to upload file: ${response.code()}"
                        }
                    } else {
                        errorMessage = "Could not read selected file."
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    errorMessage = e.message ?: "Failed to upload file."
                } finally {
                    isSending = false
                }
            }
        }
    }

    // ============================================================
    // INITIAL LOAD
    // ============================================================

    LaunchedEffect(conversationId) {
        isLoading = true
        errorMessage = null
        loadMessages()
    }

    // ============================================================
    // PERIODIC LIVE REFRESH
    // ============================================================

    LaunchedEffect(conversationId) {
        while (true) {
            delay(2000)
            try {
                val response = RetrofitClient.api.getMessages(conversationId)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        messages = body.messages
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // ============================================================
    // AUTO SCROLL
    // ============================================================

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    // ============================================================
    // UI
    // ============================================================

    Scaffold(
        containerColor = Color(0xFFF6F8FA),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isGroup) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFFFEAD6), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Group,
                                    contentDescription = null,
                                    tint = Color(0xFFF15A24),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Column {
                            Text(
                                text = userName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isGroup) "Group Chat" else "Direct Message",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 5.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ATTACH FILE BUTTON
                    IconButton(
                        enabled = !isSending,
                        onClick = { filePickerLauncher.launch("*/*") }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AttachFile,
                            contentDescription = "Attach file",
                            tint = Color(0xFFF15A24)
                        )
                    }

                    // MESSAGE INPUT
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...", fontSize = 13.sp) },
                        shape = RoundedCornerShape(22.dp),
                        maxLines = 4,
                        enabled = !isSending
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // SEND BUTTON
                    IconButton(
                        enabled = messageText.isNotBlank() && !isSending,
                        onClick = {
                            val textToSend = messageText.trim()
                            if (textToSend.isBlank()) return@IconButton

                            scope.launch {
                                try {
                                    isSending = true
                                    errorMessage = null
                                    val response = RetrofitClient.api.sendMessage(
                                        SendMessageRequest(
                                            conversationId = conversationId,
                                            message = textToSend
                                        )
                                    )
                                    if (response.isSuccessful) {
                                        messageText = ""
                                        loadMessages()
                                    } else {
                                        errorMessage = "Failed to send: ${response.code()}"
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    errorMessage = e.message ?: "Unable to send message."
                                } finally {
                                    isSending = false
                                }
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = if (messageText.isNotBlank() && !isSending) Color(0xFFF15A24) else Color(0xFFBDBDBD),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Send,
                                    contentDescription = "Send",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFF15A24))
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFFD32F2F),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(8.dp)
                )
            }

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "No messages yet.", fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Send a message or attachment to start.", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                return@Column
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    val isMine = message.isMine

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                    ) {
                        Column(
                            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            // Show sender name for incoming messages (especially in group chats)
                            if (!isMine && !message.senderName.isNullOrBlank()) {
                                Text(
                                    text = message.senderName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF15A24),
                                    modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
                                )
                            }

                            Surface(
                                color = if (isMine) Color(0xFFF15A24) else Color.White,
                                shape = RoundedCornerShape(15.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    // 1. ATTACHMENTS
                                    if (message.attachments.isNotEmpty()) {
                                        for (attachment in message.attachments) {
                                            val isImage = attachment.mimeType?.startsWith("image") == true ||
                                                    listOf(".jpg", ".jpeg", ".png", ".webp", ".gif").any {
                                                        attachment.filePath?.endsWith(it, ignoreCase = true) == true ||
                                                        attachment.originalName?.endsWith(it, ignoreCase = true) == true
                                                    }

                                            val fileUrl = if (attachment.filePath?.startsWith("http") == true) {
                                                attachment.filePath
                                            } else {
                                                "${RetrofitClient.BASE_URL.removeSuffix("/")}${if (attachment.filePath?.startsWith("/") == true) "" else "/"}${attachment.filePath ?: ""}"
                                            }

                                            if (isImage) {
                                                AsyncImage(
                                                    model = fileUrl,
                                                    contentDescription = attachment.originalName ?: "Image attachment",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = 220.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            try {
                                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))
                                                                context.startActivity(intent)
                                                            } catch (_: Exception) {}
                                                        },
                                                    contentScale = ContentScale.Crop
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                            } else {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isMine) Color(0xFFD64D1D) else Color(0xFFF2F2F2),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            try {
                                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))
                                                                context.startActivity(intent)
                                                            } catch (_: Exception) {}
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                                                            contentDescription = null,
                                                            tint = if (isMine) Color.White else Color(0xFFF15A24),
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = attachment.originalName ?: attachment.fileName ?: "Attachment",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = if (isMine) Color.White else Color.Black,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            if (attachment.fileSize != null && attachment.fileSize > 0) {
                                                                Text(
                                                                    text = "${attachment.fileSize / 1024} KB",
                                                                    fontSize = 10.sp,
                                                                    color = if (isMine) Color(0xFFFFD6C8) else Color.Gray
                                                                )
                                                            }
                                                        }
                                                        Icon(
                                                            imageVector = Icons.Outlined.FileDownload,
                                                            contentDescription = "Open",
                                                            tint = if (isMine) Color.White else Color.Gray,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                            }
                                        }
                                    }

                                    // 2. TEXT MESSAGE
                                    if (!message.messageText.isNullOrBlank()) {
                                        Text(
                                            text = message.messageText,
                                            color = if (isMine) Color.White else Color.Black,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = formatChatTime(message.createdAt),
                                fontSize = 9.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// HELPER: URI TO MULTIPART
// ============================================================

private fun uriToMultipartPart(context: Context, uri: Uri, partName: String = "file"): MultipartBody.Part? {
    return try {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

        var fileName = "attachment_${System.currentTimeMillis()}"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    val displayName = it.getString(nameIndex)
                    if (!displayName.isNullOrBlank()) {
                        fileName = displayName
                    }
                }
            }
        }

        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        MultipartBody.Part.createFormData(partName, fileName, requestBody)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// ============================================================
// FORMAT CHAT TIME
// ============================================================

private fun formatChatTime(date: String?): String {
    if (date.isNullOrBlank()) return ""
    return try {
        if (date.contains("T")) {
            date.substringAfter("T").take(5)
        } else {
            date.takeLast(8).take(5)
        }
    } catch (_: Exception) {
        ""
    }
}