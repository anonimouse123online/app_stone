package com.example.capstonesample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstonesample.data.api.RetrofitClient

import androidx.compose.foundation.BorderStroke
import com.example.capstonesample.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

private val DetailsBackground = Color(0xFFF6F8FA)
private val DetailsOrange = Color(0xFFF15A24)
private val DetailsGray = Color(0xFF64748B)

private fun formatProjectBudget(rawBudget: String): String {
    val clean = rawBudget.replace("₱", "").replace(",", "").trim()
    val num = clean.toDoubleOrNull() ?: return if (rawBudget.isNotBlank()) rawBudget else "—"
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return "₱${formatter.format(num)}"
}

private fun formatDisplayDate(raw: String): String {
    if (raw.isBlank()) return "—"
    val trimmed = if (raw.contains("T")) raw.substringBefore("T") else raw
    val parts = trimmed.split("-")
    if (parts.size == 3) {
        val y = parts[0]
        val m = parts[1].toIntOrNull() ?: return trimmed
        val d = parts[2].toIntOrNull() ?: return trimmed
        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        if (m in 1..12) {
            return "${months[m - 1]} $d, $y"
        }
    }
    return trimmed
}

data class ProjectDocumentItem(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val category: String = "",
    val uploadedAt: String = "",
    val filePath: String = ""
)

data class ProjectIssueItem(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val priority: String = "",
    val status: String = "",
    val description: String = ""
)

data class ProjectReportItem(
    val id: String = "",
    val title: String = "",
    val reportType: String = "",
    val status: String = "",
    val reportDate: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailsScreen(
    project: SiteProject,
    token: String,
    onBackClick: () -> Unit,
    issues: List<ProjectIssueItem> = emptyList(),
    reports: List<ProjectReportItem> = emptyList()
) {
    val context = LocalContext.current

    var documents by remember {
        mutableStateOf<List<ProjectDocumentItem>>(emptyList())
    }

    var isLoadingDocuments by remember {
        mutableStateOf(false)
    }

    var documentsError by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(project.code, token) {

        if (token.isBlank() || token.startsWith("LOCAL_")) {
            documentsError = "You must be signed in online to view documents."
            return@LaunchedEffect
        }

        isLoadingDocuments = true
        documentsError = null

        try {

            println("====================================")
            println("📥 GET PROJECT DOCUMENTS")
            println("PROJECT = ${project.code}")
            println("====================================")

            val response =
                RetrofitClient.api.getProjectDocuments(
                    code = project.code
                )

            println("DOCUMENT HTTP = ${response.code()}")

            if (response.isSuccessful) {

                val result = response.body()

                println("DOCUMENT RESPONSE = $result")

                if (result?.success == true) {

                    documents = result.data.map { document ->

                        ProjectDocumentItem(
                            id = document.id ?: "",
                            name = document.name ?: "Unnamed document",
                            type = document.type ?: "",
                            category = document.category ?: "",
                            uploadedAt = document.uploadedAt ?: "",
                            filePath = document.filePath ?: ""
                        )
                    }

                    println("✅ DOCUMENTS RECEIVED = ${documents.size}")

                    documents.forEach {
                        println("📄 ${it.name} | ${it.type} | ${it.category}")
                    }

                } else {
                    documents = emptyList()
                    documentsError = "Unable to load documents."
                }

            } else {

                val errorBody =
                    try {
                        response.errorBody()?.string()
                    } catch (e: Exception) {
                        null
                    }

                println("❌ DOCUMENT REQUEST FAILED")
                println("HTTP = ${response.code()}")
                println("ERROR = $errorBody")

                documents = emptyList()

                documentsError =
                    when (response.code()) {
                        401 -> "Login session expired."
                        403 -> "You do not have access to these documents."
                        404 -> "Project not found."
                        else -> "Unable to load documents."
                    }
            }

        } catch (e: Exception) {

            e.printStackTrace()

            println("====================================")
            println("❌ DOCUMENT LOAD ERROR")
            println("TYPE = ${e.javaClass.name}")
            println("MESSAGE = ${e.message}")
            println("CAUSE = ${e.cause}")
            println("====================================")

            documents = emptyList()

            documentsError =
                "${e.javaClass.simpleName}: ${e.message}"
        } finally {
            isLoadingDocuments = false
        }
    }

    Scaffold(
        containerColor = DetailsBackground,
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
                            fontSize = 11.sp,
                            color = DetailsGray
                        )
                    }
                },

                navigationIcon = {
                    IconButton(
                        onClick = {
                            println("⬅ BACK FROM PROJECT: ${project.code}")
                            onBackClick()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {

            ProjectHeaderCard(project = project)

            Spacer(modifier = Modifier.height(22.dp))

            // ============================================================
            // DOCUMENTS
            // ============================================================

            SectionHeader(
                title = "Documents",
                count = documents.size
            )

            Spacer(modifier = Modifier.height(10.dp))

            when {

                isLoadingDocuments -> {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = DetailsOrange
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "Loading documents...",
                                fontSize = 12.sp,
                                color = DetailsGray
                            )
                        }
                    }
                }

                documentsError != null -> {

                    EmptySection(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = DetailsGray
                            )
                        },
                        title = "Unable to load documents",
                        message = documentsError ?: "Unable to load documents."
                    )
                }

                documents.isEmpty() -> {

                    EmptySection(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = DetailsGray
                            )
                        },
                        title = "No documents",
                        message = "No documents have been sent to this project."
                    )
                }

                else -> {

                    documents.forEach { document ->

                        DocumentListItem(
                            document = document,
                            onDownloadClick = {
                                downloadProjectDocument(context, document)
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ============================================================
            // ISSUES
            // ============================================================

            SectionHeader(
                title = "Issues",
                count = issues.size
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (issues.isEmpty()) {

                EmptySection(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.ReportProblem,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = DetailsGray
                        )
                    },
                    title = "No issues",
                    message = "No issues have been reported for this project."
                )

            } else {

                issues.forEach { issue ->

                    IssueListItem(
                        issue = issue
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ============================================================
            // REPORTS
            // ============================================================

            SectionHeader(
                title = "Reports",
                count = reports.size
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (reports.isEmpty()) {

                EmptySection(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = DetailsGray
                        )
                    },
                    title = "No reports",
                    message = "No reports are available for this project."
                )

            } else {

                reports.forEach { report ->

                    ReportListItem(
                        report = report
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .background(
                    color = Color(0xFFFFE7DD),
                    shape = RoundedCornerShape(50.dp)
                )
                .padding(
                    horizontal = 9.dp,
                    vertical = 3.dp
                )
        ) {
            Text(
                text = count.toString(),
                color = DetailsOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptySection(
    icon: @Composable () -> Unit,
    title: String,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            icon()

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = message,
                fontSize = 11.sp,
                color = DetailsGray
            )
        }
    }
}

private fun downloadProjectDocument(context: Context, document: ProjectDocumentItem) {
    if (document.filePath.isBlank()) {
        Toast.makeText(context, "No attached file found for this document", Toast.LENGTH_SHORT).show()
        return
    }

    val fullUrl = if (document.filePath.startsWith("http://") || document.filePath.startsWith("https://")) {
        document.filePath
    } else {
        val base = RetrofitClient.BASE_URL.trimEnd('/')
        val path = document.filePath.trimStart('/')
        "$base/$path"
    }

    val extension = when {
        document.filePath.contains(".") -> "." + document.filePath.substringAfterLast(".")
        document.type.isNotBlank() -> "." + document.type.lowercase()
        else -> ""
    }

    val safeFileName = if (document.name.contains(".")) {
        document.name.replace("/", "_")
    } else {
        "${document.name.replace("/", "_")}$extension"
    }

    val mimeType = when (extension.lowercase()) {
        ".pdf" -> "application/pdf"
        ".xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ".xls" -> "application/vnd.ms-excel"
        ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        ".doc" -> "application/msword"
        ".dwg" -> "image/vnd.dwg"
        else -> "application/octet-stream"
    }

    println("====================================")
    println("📥 STARTING DOCUMENT DOWNLOAD")
    println("NAME = ${document.name}")
    println("FILE_PATH = ${document.filePath}")
    println("URL = $fullUrl")
    println("SAFE_FILE_NAME = $safeFileName")
    println("MIME_TYPE = $mimeType")
    println("====================================")

    Toast.makeText(context, "Downloading $safeFileName...", Toast.LENGTH_SHORT).show()

    // Download in background using OkHttp and save directly to Downloads
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val request = Request.Builder().url(fullUrl).build()
            val response = RetrofitClient.okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download failed: Server returned ${response.code}", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            val body = response.body
            if (body == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "File is empty", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            var savedUri: Uri? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, safeFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SitePulse")
                }
                val resolver = context.contentResolver
                savedUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (savedUri != null) {
                    resolver.openOutputStream(savedUri)?.use { out ->
                        body.byteStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "SitePulse")
                if (!dir.exists()) dir.mkdirs()
                val destFile = File(dir, safeFileName)
                FileOutputStream(destFile).use { out ->
                    body.byteStream().use { input ->
                        input.copyTo(out)
                    }
                }
                savedUri = Uri.fromFile(destFile)
            }

            // Also notify system download manager if possible
            try {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                val req = DownloadManager.Request(Uri.parse(fullUrl)).apply {
                    setTitle(safeFileName)
                    setDescription("Downloaded from SitePulse")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "SitePulse/$safeFileName")
                    setAllowedOverMetered(true)
                    setAllowedOverRoaming(true)
                }
                dm?.enqueue(req)
            } catch (e: Exception) {
                // Ignore background notification failure
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "✅ Downloaded: $safeFileName", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    Toast.makeText(context, "Opening document in browser...", Toast.LENGTH_SHORT).show()
                } catch (e2: Exception) {
                    Toast.makeText(context, "Could not download document: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

@Composable
private fun DocumentListItem(
    document: ProjectDocumentItem,
    onDownloadClick: () -> Unit
) {
    val (typeBg, typeColor) = when (document.type.uppercase()) {
        "PDF" -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
        "DWG" -> Color(0xFFEEF2FF) to Color(0xFF4F46E5)
        "XLS", "XLSX", "CSV" -> Color(0xFFECFDF5) to Color(0xFF059669)
        "DOC", "DOCX" -> Color(0xFFEFF6FF) to Color(0xFF2563EB)
        else -> Color(0xFFFFE7DD) to DetailsOrange
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDownloadClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, CardBorderStroke),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = typeBg,
                        shape = RoundedCornerShape(11.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Document",
                    tint = typeColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = document.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (document.type.isNotBlank()) {
                        Surface(
                            color = typeBg,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = document.type.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = typeColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (document.category.isNotBlank()) {
                        Text(
                            text = document.category,
                            fontSize = 11.sp,
                            color = DetailsGray
                        )
                    }
                }

                if (document.uploadedAt.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Uploaded: ${formatDisplayDate(document.uploadedAt)}",
                        fontSize = 10.sp,
                        color = DetailsGray
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            FilledTonalIconButton(
                onClick = onDownloadClick,
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (document.filePath.isNotBlank()) Color(0xFFFFE7DD) else Color(0xFFF1F5F9),
                    contentColor = if (document.filePath.isNotBlank()) DetailsOrange else Color(0xFF94A3B8)
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.FileDownload,
                    contentDescription = "Download document",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun IssueListItem(
    issue: ProjectIssueItem
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.Top
        ) {

            Box(
                modifier = Modifier
                    .size(45.dp)
                    .background(
                        color = Color(0xFFFFE7DD),
                        shape = RoundedCornerShape(11.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Outlined.ReportProblem,
                    contentDescription = "Issue",
                    tint = DetailsOrange
                )
            }

            Spacer(modifier = Modifier.width(13.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = issue.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row {

                    if (issue.priority.isNotBlank()) {

                        Text(
                            text = issue.priority,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color =
                                when (issue.priority.lowercase()) {
                                    "high", "critical" -> Color.Red
                                    "medium" -> Color(0xFFE58B00)
                                    else -> Color(0xFF199642)
                                }
                        )
                    }

                    if (
                        issue.priority.isNotBlank() &&
                        issue.status.isNotBlank()
                    ) {
                        Text(
                            text = " • ",
                            fontSize = 11.sp,
                            color = DetailsGray
                        )
                    }

                    Text(
                        text = issue.status,
                        fontSize = 11.sp,
                        color = DetailsGray
                    )
                }

                if (issue.description.isNotBlank()) {

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = issue.description,
                        fontSize = 11.sp,
                        color = DetailsGray
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportListItem(
    report: ProjectReportItem
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(45.dp)
                    .background(
                        color = Color(0xFFFFE7DD),
                        shape = RoundedCornerShape(11.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Outlined.Assignment,
                    contentDescription = "Report",
                    tint = DetailsOrange
                )
            }

            Spacer(modifier = Modifier.width(13.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = report.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = buildString {

                        if (report.reportType.isNotBlank()) {
                            append(report.reportType)
                        }

                        if (report.status.isNotBlank()) {

                            if (isNotEmpty()) {
                                append(" • ")
                            }

                            append(report.status)
                        }
                    },
                    fontSize = 11.sp,
                    color = DetailsGray
                )

                if (report.reportDate.isNotBlank()) {

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = report.reportDate,
                        fontSize = 10.sp,
                        color = DetailsGray
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectHeaderCard(
    project: SiteProject
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, CardBorderStroke),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = project.phase,
                        fontSize = 12.sp,
                        color = DetailsGray
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = project.name,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = project.code,
                        fontSize = 12.sp,
                        color = DetailsGray
                    )
                }

                StatusBadge(
                    status = project.status
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            DetailRow("Location", project.location)
            DetailRow("Client", project.client)
            DetailRow("Manager", project.manager)
            DetailRow("Scope", project.scope)
            DetailRow("Budget", project.budget)
            DetailRow("Start Date", project.startDate)
            DetailRow("Due Date", project.dueDate)

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Overall Progress",
                    fontSize = 12.sp,
                    color = DetailsGray
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "${project.progress}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DetailsOrange
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = {
                    project.progress.coerceIn(0, 100) / 100f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp),
                color = DetailsOrange,
                trackColor = Color(0xFFEAE4E1)
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    val displayValue = when (label.lowercase()) {
        "budget" -> formatProjectBudget(value)
        "start date", "due date" -> formatDisplayDate(value)
        else -> value.ifBlank { "Not assigned" }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {

        Text(
            text = "$label:",
            modifier = Modifier.width(105.dp),
            fontSize = 12.sp,
            color = DetailsGray
        )

        Text(
            text = displayValue,
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextSlate900
        )
    }
}

@Composable
private fun StatusBadge(
    status: String
) {
    val statusLower = status.trim().lowercase()
    val (statusBg, statusTextColor) = when {
        statusLower.contains("track") || statusLower.contains("active") || statusLower.contains("ongoing") || statusLower.contains("completed") ->
            Pair(EmeraldGreenBg, EmeraldGreenText)
        statusLower.contains("delayed") || statusLower.contains("risk") || statusLower.contains("halt") || statusLower.contains("behind") ->
            Pair(RoseRedBg, RoseRedText)
        else ->
            Pair(AmberWarningBg, AmberWarningText)
    }

    Box(
        modifier = Modifier
            .background(
                color = statusBg,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 5.dp
            )
    ) {

        Text(
            text = status.ifBlank { "ACTIVE" }.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = statusTextColor
        )
    }
}