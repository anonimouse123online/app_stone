package com.example.capstonesample.ai

import android.content.Context
import java.io.File

class ModelStatusChecker(private val context: Context) {

    companion object {
        const val QWEN_MODEL = "qwen2.5-1.5b-instruct-q4_k_m.gguf"
        const val QWEN_MIN_SIZE = 900_000_000L
    }

    private val modelDir = File(context.filesDir, "models").apply { mkdirs() }
    private val qwenFile = File(modelDir, QWEN_MODEL)

    fun getStatus(): AiStatus {
        val qwenExists = qwenFile.exists() && qwenFile.length() >= QWEN_MIN_SIZE

        android.util.Log.d(
            "ModelStatusChecker",
            "Checking path=${qwenFile.absolutePath} exists=${qwenFile.exists()} length=${qwenFile.length()} minRequired=$QWEN_MIN_SIZE"
        )

        return when {
            !qwenExists -> AiStatus.NotDownloaded
            else -> AiStatus.Ready
        }
    }

    fun isReady(): Boolean = getStatus() is AiStatus.Ready

    fun getQwenPath(): String = qwenFile.absolutePath

    fun getDownloadedSize(): Long = if (qwenFile.exists()) qwenFile.length() else 0L

    sealed class AiStatus {
        object NotDownloaded : AiStatus()
        object Ready : AiStatus()
    }
}