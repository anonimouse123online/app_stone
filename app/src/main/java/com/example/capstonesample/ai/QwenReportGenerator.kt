package com.example.capstonesample.ai

import android.content.Context
import java.io.File

class QwenReportGenerator(context: Context) {

    private var modelPtr: Long = 0
    private val statusChecker = ModelStatusChecker(context)

    init {
        if (!File(statusChecker.getQwenPath()).exists()) {
            throw IllegalStateException("Qwen model not found")
        }

        modelPtr = nativeLoadModel(statusChecker.getQwenPath(), 2048, 4)
    }

    fun generateReport(imageDescription: String): String {
        val prompt = """<|im_start|>system
You are a professional inspection AI. Generate detailed, structured reports from image descriptions. Use formal language. Be specific and actionable.<|im_end|>
<|im_start|>user
Based on this image analysis, generate a formal inspection report:

IMAGE ANALYSIS:
$imageDescription

Format:
1. OBJECTS IDENTIFIED
2. CONDITION ASSESSMENT
3. NOTABLE DETAILS
4. RECOMMENDATIONS
5. OVERALL SUMMARY<|im_end|>
<|im_start|>assistant
"""

        return nativeGenerate(modelPtr, prompt, 512)
    }

    fun release() {
        if (modelPtr != 0L) {
            nativeUnload(modelPtr)
            modelPtr = 0
        }
    }

    private external fun nativeLoadModel(path: String, nCtx: Int, nThreads: Int): Long
    private external fun nativeGenerate(modelPtr: Long, prompt: String, maxTokens: Int): String
    private external fun nativeUnload(modelPtr: Long)

    companion object {
        init {
            System.loadLibrary("llama")
        }
    }
}