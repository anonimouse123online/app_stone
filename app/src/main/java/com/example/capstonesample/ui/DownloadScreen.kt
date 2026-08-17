package com.example.capstonesample.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstonesample.R
import com.example.capstonesample.ai.ModelDownloadManager
import com.example.capstonesample.ai.ModelStatusChecker
import kotlinx.coroutines.launch
import com.example.capstonesample.ai.DownloadProgress

@Composable
fun DownloadScreen(
    onDownloadComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val statusChecker = remember { ModelStatusChecker(context) }
    val downloadManager = remember { ModelDownloadManager(context) }

    // State declarations - ORDER MATTERS
    var isAlreadyDownloaded by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var progressText by remember { mutableStateOf("0% - 0MB / 0MB") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // FIXED: Recheck model status every time screen becomes visible
    LaunchedEffect(Unit) {
        isAlreadyDownloaded = statusChecker.isReady()
    }

    // FIXED: Derive status and description from state
    // Now isDownloading is declared above, so these work
    val status = when {
        isDownloading -> "Downloading AI Model..."
        isAlreadyDownloaded -> "AI Model Ready"
        else -> "AI Model Not Installed"
    }

    val description = when {
        isDownloading -> "Downloading Qwen2.5. Please keep the app open."
        isAlreadyDownloaded -> "Qwen2.5 is downloaded and ready.\n\nYou can now generate reports."
        else -> "Download Qwen2.5 AI model (~1GB) to generate inspection reports."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val bitmap = remember {
            BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        }
        if (bitmap != null) {
            Image(
                painter = BitmapPainter(bitmap.asImageBitmap()),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = status,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = description,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isDownloading) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = progressText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    downloadManager.cancelDownload()
                    isDownloading = false
                }
            ) {
                Text("Cancel")
            }
        } else {
            if (isAlreadyDownloaded) {
                Column {
                    Button(
                        onClick = { onDownloadComplete() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("Start Using AI", fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            downloadManager.deleteAllModels()
                            isAlreadyDownloaded = false
                        }
                    ) {
                        Text("Delete & Re-download", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                Button(
                    onClick = {
                        if (!isWifiConnected(context)) {
                            showError = true
                            errorMessage = "Please connect to WiFi. The AI model is ~1GB."
                            return@Button
                        }

                        isDownloading = true

                        scope.launch {
                            downloadModel(context, downloadManager,
                                onProgress = { p, text ->
                                    progress = p
                                    progressText = text
                                },
                                onComplete = {
                                    isDownloading = false
                                    isAlreadyDownloaded = true
                                    onDownloadComplete()
                                },
                                onError = { msg ->
                                    isDownloading = false
                                    isAlreadyDownloaded = statusChecker.isReady()
                                    showError = true
                                    errorMessage = msg
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Download AI Now", fontSize = 16.sp)
                }
            }
        }
    }

    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = { Text("Download Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showError = false }) {
                    Text("OK")
                }
            }
        )
    }
}

private suspend fun downloadModel(
    context: Context,
    downloadManager: ModelDownloadManager,
    onProgress: (Int, String) -> Unit,
    onComplete: () -> Unit,
    onError: (String) -> Unit
) {
    val url = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"
    val filename = "qwen2.5-1.5b-instruct-q4_k_m.gguf"

    val totalSize = 1_120_000_000L
    var currentDownloaded = 0L

    try {
        downloadManager.downloadFile(url, filename).collect { progress ->
            when (progress) {
                is DownloadProgress.Progress -> {
                    currentDownloaded = progress.downloaded
                    val percent = if (progress.total > 0) {
                        (currentDownloaded * 100 / progress.total).toInt()
                    } else {
                        (currentDownloaded * 100 / totalSize).toInt()
                    }
                    val text = "${progress.downloadedMB}MB / ${progress.totalMB}MB (${percent}%)"
                    onProgress(percent, text)
                }
                is DownloadProgress.FileDone -> {
                    onComplete()
                }
                is DownloadProgress.Error -> {
                    onError(progress.message)
                }
            }
        }
    } catch (e: Exception) {
        onError("${e.javaClass.simpleName}: ${e.message ?: "Unknown error"}")
    }
}

private fun isWifiConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}