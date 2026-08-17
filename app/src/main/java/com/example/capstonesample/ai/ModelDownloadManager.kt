package com.example.capstonesample.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class ModelDownloadManager(private val context: Context) {

    private val modelDir: File = File(context.filesDir, "models").apply {
        if (!exists() && !mkdirs()) {
            Log.e("ModelDownloadManager", "Failed to create models directory")
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)  // IMPORTANT: Follow redirects
        .followSslRedirects(true)
        .build()

    private var currentCall: okhttp3.Call? = null

    fun downloadFile(url: String, filename: String): Flow<DownloadProgress> = flow {
        try {
            Log.d("ModelDownloadManager", "Downloading: $url")

            val file = File(modelDir, filename)
            val tempFile = File(modelDir, "$filename.download")
            val downloadedBytes = if (tempFile.exists()) tempFile.length() else 0L

            // Build request with proper headers for HuggingFace
            val requestBuilder = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) CapstoneApp/1.0")
                .addHeader("Accept", "application/octet-stream,*/*")

            if (downloadedBytes > 0) {
                requestBuilder.header("Range", "bytes=$downloadedBytes-")
            }

            currentCall = client.newCall(requestBuilder.build())
            val response = currentCall!!.execute()

            Log.d("ModelDownloadManager", "Response: ${response.code} ${response.message}")
            Log.d("ModelDownloadManager", "Headers: ${response.headers}")

            // Handle redirects and check response
            if (!response.isSuccessful && response.code != 206) {
                val errorBody = response.body?.string()?.take(500) ?: "No body"
                Log.e("ModelDownloadManager", "HTTP Error ${response.code}: $errorBody")
                throw IOException("HTTP ${response.code}: ${response.message}")
            }

            val body = response.body ?: throw IOException("Empty response body")

            // Get content length - try multiple header sources
            val contentLength = response.header("Content-Length")?.toLongOrNull()
                ?: response.header("content-length")?.toLongOrNull()
                ?: 0L
            val totalBytes = contentLength + downloadedBytes

            Log.d("ModelDownloadManager", "Content-Length: $contentLength, total: $totalBytes")

            tempFile.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var currentDownloaded = downloadedBytes
                    var bytesRead: Int
                    var lastEmitTime = System.currentTimeMillis()

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        currentDownloaded += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastEmitTime > 500) {
                            lastEmitTime = now
                            val totalForCalc = if (totalBytes > 0) totalBytes else currentDownloaded
                            val percent = if (totalBytes > 0) (currentDownloaded * 100 / totalBytes).toInt() else 0
                            emit(
                                DownloadProgress.Progress(
                                    filename = filename,
                                    downloaded = currentDownloaded,
                                    total = totalForCalc,
                                    downloadedMB = currentDownloaded / 1_048_576,
                                    totalMB = totalForCalc / 1_048_576
                                )
                            )
                        }
                    }
                }
            }

            if (tempFile.exists() && tempFile.length() > 1000) {
                if (tempFile.renameTo(file)) {
                    Log.d("ModelDownloadManager", "Download complete: $filename (${file.length()} bytes)")
                    // FIXED: Added extra logging to verify file location
                    Log.d("ModelDownloadManager", "Final file path: ${file.absolutePath}")
                    Log.d("ModelDownloadManager", "Final file exists: ${file.exists()}")
                    emit(DownloadProgress.FileDone(filename))
                } else {
                    throw IOException("Failed to rename temp file")
                }
            } else {
                throw IOException("Downloaded file is empty or missing")
            }

        } catch (e: Exception) {
            if (e is java.util.concurrent.CancellationException) throw e
            Log.e("ModelDownloadManager", "Error: ${e.javaClass.simpleName}: ${e.message}")
            emit(DownloadProgress.Error(filename, "${e.javaClass.simpleName}: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    fun cancelDownload() {
        currentCall?.cancel()
    }

    fun deleteAllModels() {
        modelDir.listFiles()?.forEach { it.delete() }
    }
}