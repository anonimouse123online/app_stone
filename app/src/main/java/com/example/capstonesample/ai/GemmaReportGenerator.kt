package com.example.capstonesample.ai

import android.content.Context
import android.util.Log

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext

import java.io.Closeable
import java.io.File


class GemmaReportGenerator(
    private val context: Context,
    private val modelPath: String
) : Closeable {

    companion object {

        private const val TAG =
            "GEMMA_AI"
    }


    // ============================================================
    // ENGINE
    // ============================================================

    private var engine: Engine? =
        null


    // ============================================================
    // INITIALIZE MODEL
    // ============================================================

    suspend fun initialize() = withContext(
        Dispatchers.Default
    ) {

        if (engine != null) {

            Log.d(
                TAG,
                "Gemma engine already initialized."
            )

            return@withContext
        }


        val modelFile =
            File(
                modelPath
            )


        if (!modelFile.exists()) {

            throw IllegalStateException(
                "Gemma model not found at: $modelPath"
            )
        }


        if (!modelFile.isFile) {

            throw IllegalStateException(
                "Gemma model path is not a file."
            )
        }


        if (!modelFile.canRead()) {

            throw IllegalStateException(
                "Gemma model cannot be read."
            )
        }


        if (modelFile.length() <= 0L) {

            throw IllegalStateException(
                "Gemma model file is empty."
            )
        }


        Log.d(
            TAG,
            "========================================"
        )

        Log.d(
            TAG,
            "INITIALIZING GEMMA"
        )

        Log.d(
            TAG,
            "Model path: $modelPath"
        )

        Log.d(
            TAG,
            "Model size: ${modelFile.length()} bytes"
        )

        Log.d(
            TAG,
            "Model size MB: ${modelFile.length() / 1_048_576L}"
        )

        Log.d(
            TAG,
            "========================================"
        )


        val config =
            EngineConfig(

                modelPath =
                    modelPath,

                backend =
                    Backend.CPU(),

                cacheDir =
                    context.cacheDir.absolutePath
            )


        val newEngine =
            Engine(
                config
            )


        try {

            newEngine.initialize()

            engine =
                newEngine


            Log.d(
                TAG,
                "Gemma initialized successfully."
            )


        } catch (e: Throwable) {

            Log.e(
                TAG,
                "Gemma initialization failed.",
                e
            )


            try {

                newEngine.close()

            } catch (_: Throwable) {
            }


            throw e
        }
    }


    // ============================================================
    // GENERATE REPORT
    // ============================================================

    suspend fun generateReport(
        imageDescription: String,
        engineerAnnotations: String =
            "No engineer annotations were added to the image.",
        date: String = "",
        projectName: String = "",
        location: String = "",
        manpowerInfo: String = "",
        workProgressInfo: String = "",
        ongoingScopeInfo: String = ""
    ): String = withContext(
        Dispatchers.Default
    ) {

        val currentEngine =
            engine
                ?: throw IllegalStateException(
                    "Gemma has not been initialized."
                )

        Log.d(
            TAG,
            "Generating SitePulse client-formatted Daily Site Report..."
        )

        val cleanDate = date.ifBlank {
            java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.ENGLISH).format(java.util.Date())
        }
        val cleanProject = projectName.ifBlank { "Construction Project" }
        val cleanLocation = location.ifBlank { "Project Site" }
        val cleanManpower = manpowerInfo.trim()
        val hasManpower = cleanManpower.isNotBlank()
        val cleanWorkProgress = workProgressInfo.ifBlank {
            "Site Inspection & Preparation: 100% Completed"
        }
        val cleanOngoingScope = ongoingScopeInfo.ifBlank {
            "General Finishing & Quality Testing"
        }

        // ========================================================
        // PROMPT
        // ========================================================

        val prompt = buildString {
            appendLine("You are SitePulse AI, a professional construction site report generator.")
            appendLine()
            appendLine("Generate an official Daily Site Report matching the exact format below, based on the project and field information provided.")
            appendLine()
            appendLine("PROJECT DATA:")
            appendLine("Date: $cleanDate")
            appendLine("Project Name: $cleanProject")
            appendLine("Location: $cleanLocation")
            if (hasManpower) {
                appendLine("Manpower:")
                appendLine(cleanManpower)
            }
            appendLine()
            appendLine("Work Progress:")
            appendLine(cleanWorkProgress)
            appendLine()
            appendLine("Ongoing Scope of works:")
            appendLine(cleanOngoingScope)
            appendLine()
            appendLine("Supporting Field Annotations:")
            appendLine(engineerAnnotations)
            appendLine()
            appendLine("Visual Detections:")
            appendLine(imageDescription)
            appendLine()
            appendLine("IMPORTANT FORMAT RULES:")
            appendLine("1. Output MUST start with the exact title:")
            appendLine("Daily Site Report")
            appendLine("2. Followed by Date, Project Name, and Location.")
            if (hasManpower) {
                appendLine("3. Next section MUST be titled:")
                appendLine("Manpower")
                appendLine("Followed by the Total and breakdown.")
            } else {
                appendLine("3. Do NOT include a Manpower section.")
            }
            appendLine("4. Next section MUST be titled:")
            appendLine("Work Progress")
            appendLine("List completed work items line by line in this format:")
            appendLine("[Item]: 100% Completed")
            appendLine("5. Next section MUST be titled:")
            appendLine("Ongoing Scope of works")
            appendLine("List remaining or ongoing work items line by line.")
            appendLine("6. Do NOT use markdown symbols such as ** or #.")
            appendLine("7. Do NOT include greetings, intro remarks, conversational text, or disclaimers.")
            appendLine("8. Every section MUST be separated by a blank line.")
            appendLine()
            appendLine("OUTPUT EXACTLY THIS STRUCTURE:")
            appendLine()
            appendLine("Daily Site Report")
            appendLine()
            appendLine("Date: $cleanDate")
            appendLine()
            appendLine("Project Name: $cleanProject")
            appendLine("Location: $cleanLocation")
            if (hasManpower) {
                appendLine()
                appendLine()
                appendLine("Manpower")
                appendLine()
                appendLine(cleanManpower)
            }
            appendLine()
            appendLine()
            appendLine("Work Progress")
            appendLine()
            appendLine(cleanWorkProgress)
            appendLine()
            appendLine()
            appendLine("Ongoing Scope of works")
            appendLine()
            appendLine(cleanOngoingScope)
            appendLine()
            appendLine("Generate the report now.")
        }

        // ========================================================
        // RESULT BUILDER
        // ========================================================

        val result =
            StringBuilder()


        // ========================================================
        // CREATE CONVERSATION
        // ========================================================

        currentEngine
            .createConversation()
            .use { conversation ->

                conversation
                    .sendMessageAsync(
                        prompt
                    )
                    .catch { error ->
                        Log.e(
                            TAG,
                            "Gemma generation stream failed.",
                            error
                        )
                        throw error
                    }
                    .collect { message ->
                        val chunk = message.toString()
                        if (chunk.isNotEmpty()) {
                            result.append(chunk)
                        }
                    }
            }


        // ========================================================
        // FINAL RESULT & POST-PROCESSING
        // ========================================================

        var finalReport =
            result
                .toString()
                .trim()

        finalReport =
            finalReport
                .replace("**", "")
                .replace("* ", "- ")
                .replace(Regex("(?i)^#+\\s*"), "")
                .replace(Regex("\n{3,}"), "\n\n")
                .trim()

        val badResponse =
            finalReport.startsWith("Okay", ignoreCase = true) ||
            finalReport.startsWith("Sure", ignoreCase = true) ||
            finalReport.contains("please provide", ignoreCase = true) ||
            finalReport.contains("let's proceed", ignoreCase = true) ||
            !finalReport.contains("Daily Site Report", ignoreCase = true) ||
            !finalReport.contains("Work Progress", ignoreCase = true)

        if (badResponse || finalReport.isBlank()) {
            Log.w(
                TAG,
                "Gemma returned conversational or incomplete output. Using formatted structured report."
            )
            finalReport = formatDailyReport(
                date = cleanDate,
                projectName = cleanProject,
                location = cleanLocation,
                manpowerInfo = cleanManpower,
                workProgressInfo = cleanWorkProgress,
                ongoingScopeInfo = cleanOngoingScope
            )
        }

        Log.d(
            TAG,
            "========================================"
        )
        Log.d(
            TAG,
            "CLIENT DAILY SITE REPORT GENERATED"
        )
        Log.d(
            TAG,
            finalReport
        )
        Log.d(
            TAG,
            "========================================"
        )

        finalReport
    }


    // ============================================================
    // STANDALONE FORMATTER
    // ============================================================

    fun formatDailyReport(
        date: String,
        projectName: String,
        location: String,
        manpowerInfo: String,
        workProgressInfo: String,
        ongoingScopeInfo: String
    ): String {
        val cleanDate = date.ifBlank {
            java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.ENGLISH).format(java.util.Date())
        }
        val cleanProject = projectName.ifBlank { "Construction Project" }
        val cleanLocation = location.ifBlank { "Project Site" }
        val cleanManpower = manpowerInfo.trim()
        val cleanWorkProgress = workProgressInfo.ifBlank {
            "Site Inspection & Preparation: 100% Completed"
        }
        val cleanOngoingScope = ongoingScopeInfo.ifBlank {
            "General Finishing & Quality Testing"
        }

        return buildString {
            appendLine("Daily Site Report")
            appendLine()
            appendLine("Date: $cleanDate")
            appendLine()
            appendLine("Project Name: $cleanProject")
            appendLine("Location: $cleanLocation")
            if (cleanManpower.isNotBlank()) {
                appendLine()
                appendLine()
                appendLine("Manpower")
                appendLine()
                appendLine(cleanManpower)
            }
            appendLine()
            appendLine()
            appendLine("Work Progress")
            appendLine()
            appendLine(cleanWorkProgress.trim())
            appendLine()
            appendLine()
            appendLine("Ongoing Scope of works")
            appendLine()
            appendLine(cleanOngoingScope.trim())
        }.trim()
    }


    // ============================================================
    // CHECK INITIALIZED
    // ============================================================

    fun isInitialized(): Boolean {

        return engine != null
    }


    // ============================================================
    // CLOSE
    // ============================================================

    override fun close() {

        val currentEngine =
            engine


        engine =
            null


        if (currentEngine == null) {

            return
        }


        try {

            currentEngine.close()


            Log.d(
                TAG,
                "Gemma engine closed."
            )


        } catch (e: Throwable) {

            Log.e(
                TAG,
                "Error closing Gemma.",
                e
            )
        }
    }
}
