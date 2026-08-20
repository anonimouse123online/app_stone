package com.example.capstonesample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstonesample.data.api.RetrofitClient

private val DetailsBackground = Color(0xFFF0E1D8)
private val DetailsOrange = Color(0xFFF15A24)
private val DetailsGray = Color(0xFF777777)

data class ProjectDocumentItem(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val category: String = "",
    val uploadedAt: String = ""
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
                            uploadedAt = document.uploadedAt ?: ""
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
                            document = document
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

@Composable
private fun DocumentListItem(
    document: ProjectDocumentItem
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
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Document",
                    tint = DetailsOrange
                )
            }

            Spacer(modifier = Modifier.width(13.dp))

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

                Text(
                    text = buildString {

                        if (document.type.isNotBlank()) {
                            append(document.type)
                        }

                        if (document.category.isNotBlank()) {

                            if (isNotEmpty()) {
                                append(" • ")
                            }

                            append(document.category)
                        }
                    },
                    fontSize = 11.sp,
                    color = DetailsGray
                )

                if (document.uploadedAt.isNotBlank()) {

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Uploaded: ${document.uploadedAt}",
                        fontSize = 10.sp,
                        color = DetailsGray
                    )
                }
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
        )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {

        Text(
            text = "$label:",
            modifier = Modifier.width(100.dp),
            fontSize = 12.sp,
            color = DetailsGray
        )

        Text(
            text = value,
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
    }
}

@Composable
private fun StatusBadge(
    status: String
) {
    val isGoodStatus =
        status.equals("On track", ignoreCase = true) ||
                status.equals("Active", ignoreCase = true) ||
                status.equals("Ongoing", ignoreCase = true) ||
                status.equals("Planning", ignoreCase = true)

    val background =
        if (isGoodStatus) {
            Color(0xFFDDF4E1)
        } else {
            Color(0xFFFFDEDE)
        }

    val textColor =
        if (isGoodStatus) {
            Color(0xFF199642)
        } else {
            Color.Red
        }

    Box(
        modifier = Modifier
            .background(
                color = background,
                shape = RoundedCornerShape(9.dp)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 6.dp
            )
    ) {

        Text(
            text = status,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}