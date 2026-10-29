package com.example.capstonesample.documents

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.capstonesample.ProjectDocumentItem
import com.example.capstonesample.data.api.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

internal fun projectDocumentUrl(path: String, baseUrl: String): HttpUrl {
    val value = path.trim()
    require(value.isNotEmpty()) { "No attached file found for this document." }
    val url = if (value.startsWith("http://", true) || value.startsWith("https://", true)) {
        value.toHttpUrlOrNull()
    } else {
        require(!value.contains(':') && !value.startsWith("//") && !value.contains('\\')) {
            "Invalid document URL."
        }
        "${baseUrl.trimEnd('/')}/${value.trimStart('/')}".toHttpUrlOrNull()
    }
    return requireNotNull(url) { "Invalid document URL." }
}

internal fun projectDocumentMimeType(path: String, type: String, mimeType: String = ""): String {
    mimeType.toMediaTypeOrNull()?.let { return "${it.type}/${it.subtype}" }
    val extension = path.substringBefore('?').substringBefore('#')
        .substringAfterLast('.', "").ifBlank { type }.lowercase()
    return when (extension) {
        "pdf" -> "application/pdf"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "txt" -> "text/plain"
        "csv" -> "text/csv"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "xls" -> "application/vnd.ms-excel"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "doc" -> "application/msword"
        "dwg" -> "image/vnd.dwg"
        else -> "application/octet-stream"
    }
}

internal suspend fun cacheProjectDocument(
    directory: File,
    url: HttpUrl,
    client: OkHttpClient
): File = withContext(Dispatchers.IO) {
    client.newCall(Request.Builder().url(url).build()).execute().use { response ->
        check(response.isSuccessful) {
            when (response.code) {
                401 -> "Your login session has expired. Please sign in again."
                403 -> "You do not have access to this document."
                404 -> "Document is unavailable."
                else -> "Unable to open document: server returned ${response.code}."
            }
        }
        val body = checkNotNull(response.body) { "Document is empty." }
        val extension = url.pathSegments.lastOrNull()?.substringAfterLast('.', "")
            ?.takeIf { it.isNotEmpty() && it.length <= 10 && it.all(Char::isLetterOrDigit) }
        val file = File.createTempFile("project_document_", extension?.let { ".$it" } ?: ".tmp", directory)
        try {
            body.byteStream().use { input -> file.outputStream().use { input.copyTo(it) } }
            check(file.length() > 0) { "Document is empty." }
            file
        } catch (e: Exception) {
            file.delete()
            throw e
        }
    }
}

@Composable
internal fun ProjectDocumentViewer(document: ProjectDocumentItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var file by remember(document) { mutableStateOf<File?>(null) }
    var error by remember(document) { mutableStateOf<String?>(null) }
    var textContent by remember(document) { mutableStateOf<String?>(null) }
    val mimeType = projectDocumentMimeType(document.filePath, document.type, document.mimeType)
    val isImage = mimeType in setOf("image/jpeg", "image/png", "image/webp", "image/gif")
    val isText = mimeType.startsWith("text/")

    LaunchedEffect(document) {
        try {
            val url = projectDocumentUrl(document.filePath, RetrofitClient.BASE_URL)
            val cached = cacheProjectDocument(context.cacheDir, url, RetrofitClient.okHttpClient)
            if (isText) {
                textContent = withContext(Dispatchers.IO) {
                    cached.readText(document.mimeType.toMediaTypeOrNull()?.charset() ?: Charsets.UTF_8)
                }
            }
            file = cached
            if (!isImage && !isText) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", cached)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    clipData = ClipData.newRawUri(document.name, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
                onDismiss()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: ActivityNotFoundException) {
            error = "No installed app can view this document type."
        } catch (e: Exception) {
            error = e.message ?: "Unable to open document."
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(document.name) },
        text = {
            Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                val cached = file
                when {
                    error != null -> Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    cached == null -> CircularProgressIndicator()
                    isText -> Text(
                        textContent.orEmpty(),
                        modifier = Modifier.fillMaxWidth().height(240.dp)
                            .verticalScroll(rememberScrollState())
                    )
                    isImage -> AsyncImage(
                        model = cached,
                        contentDescription = document.name,
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        contentScale = ContentScale.Fit,
                        onError = { error = "Unable to display this image." }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
