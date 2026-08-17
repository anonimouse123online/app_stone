package com.example.capstonesample.ai

sealed class DownloadProgress {
    data class Progress(
        val filename: String,
        val downloaded: Long,
        val total: Long,
        val downloadedMB: Long,
        val totalMB: Long
    ) : DownloadProgress()

    data class FileDone(val filename: String) : DownloadProgress()

    data class Error(val filename: String, val message: String) : DownloadProgress()
}