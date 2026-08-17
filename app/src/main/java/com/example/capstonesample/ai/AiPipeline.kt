package com.example.capstonesample.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiPipeline(context: Context) {

    private val qwen = QwenReportGenerator(context)

    suspend fun generateReport(prompt: String): String = withContext(Dispatchers.Default) {
        qwen.generateReport(prompt)
    }

    fun release() {
        qwen.release()
    }
}