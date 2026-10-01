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
            "No engineer annotations were added to the image."

    ): String = withContext(
        Dispatchers.Default
    ) {

        val currentEngine =
            engine
                ?: throw IllegalStateException(
                    "Gemma has not been initialized."
                )


        if (engineerAnnotations.isBlank()) {

            throw IllegalArgumentException(
                "Engineer annotation input is empty."
            )
        }


        Log.d(
            TAG,
            "Generating SitePulse engineer-guided AI report..."
        )


        Log.d(
            TAG,
            "Supporting visual input:\n$imageDescription"
        )


        Log.d(
            TAG,
            "Primary engineer annotation input:\n$engineerAnnotations"
        )


        // ========================================================
        // PROMPT
        // ========================================================

        val prompt =
            """
You are SitePulse AI, a professional construction field reporting assistant.

Generate a FINAL construction field assessment using the engineer-provided
field information below.

PRIMARY ENGINEER FIELD DATA:
$engineerAnnotations

SECONDARY VISUAL OBSERVATIONS:
$imageDescription

IMPORTANT RULES:

1. Engineer-provided information is the authoritative source.
2. Use the exact WORK_TYPE provided by the engineer.
3. Use the exact STAGE provided by the engineer.
4. If PROGRESS_PERCENT contains a number, use that exact percentage.
5. NEVER state that no percentage was provided when a progress percentage exists.
6. Never calculate progress from YOLO confidence.
7. Automated object detections are secondary evidence only.
8. Do not focus on helmets, persons, PPE, tools, or unrelated objects unless directly relevant.
9. Do not invent construction work, defects, hazards, or progress.
10. Use professional construction terminology.
11. Do not say "Okay", "Sure", "Let's proceed", or ask for more information.
12. Do not greet the user.
13. Do not use Markdown symbols such as ** or #.
14. Every section MUST be separated by a blank line.
15. Generate the final report immediately.

OUTPUT EXACTLY THIS STRUCTURE:

SITEPULSE FIELD INSPECTION REPORT

WORK ACTIVITY:
[Exact engineer-provided work type]

CURRENT STATUS:
[Exact engineer-provided stage]

ENGINEER-RECORDED PROGRESS:
[Exact engineer-provided progress percentage]

FIELD ASSESSMENT:
[Write 2-3 professional sentences describing the current construction activity.]

PROGRESS OBSERVATION:
[Write 1-2 sentences explaining the current progress without changing the engineer percentage.]

FIELD VERIFICATION:
[State that the engineer-recorded progress should be verified through normal site inspection and project documentation procedures.]

RECOMMENDED ACTIONS:
1. [Relevant construction action]
2. [Relevant inspection or verification action]

OVERALL SUMMARY:
[Maximum 2 professional sentences summarizing work activity, stage, and progress.]

AI NOTE:
This report was generated using engineer-provided field annotations. Automated image detections are used only as supporting information and do not determine construction progress.

Generate the report now.
    """.trimIndent()
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

                        val chunk =
                            message.toString()

                        if (chunk.isNotEmpty()) {

                            // IMPORTANT:
                            // Do NOT manually add spaces between chunks.
                            // Gemma may stream fragments of the same word.
                            result.append(chunk)
                        }
                    }
            }


        // ========================================================
        // FINAL RESULT
        // ========================================================

        var finalReport =
            result
                .toString()
                .trim()

        finalReport =
            finalReport
                .replace(
                    "SITEPULSE FIELD INSPECTION REPORT",
                    "SITEPULSE FIELD INSPECTION REPORT\n\n"
                )
                .replace(
                    "WORK ACTIVITY:",
                    "\n\nWORK ACTIVITY:\n"
                )
                .replace(
                    "CURRENT STATUS:",
                    "\n\nCURRENT STATUS:\n"
                )
                .replace(
                    "ENGINEER-RECORDED PROGRESS:",
                    "\n\nENGINEER-RECORDED PROGRESS:\n"
                )
                .replace(
                    "FIELD ASSESSMENT:",
                    "\n\nFIELD ASSESSMENT:\n"
                )
                .replace(
                    "PROGRESS OBSERVATION:",
                    "\n\nPROGRESS OBSERVATION:\n"
                )
                .replace(
                    "FIELD VERIFICATION:",
                    "\n\nFIELD VERIFICATION:\n"
                )
                .replace(
                    "RECOMMENDED ACTIONS:",
                    "\n\nRECOMMENDED ACTIONS:\n"
                )
                .replace(
                    "OVERALL SUMMARY:",
                    "\n\nOVERALL SUMMARY:\n"
                )
                .replace(
                    "AI NOTE:",
                    "\n\nAI NOTE:\n"
                )
                .replace(
                    Regex("\n{3,}"),
                    "\n\n"
                )
                .trim()



        val badResponse =
            finalReport.startsWith(
                "Okay",
                ignoreCase = true
            ) ||
                    finalReport.startsWith(
                        "Sure",
                        ignoreCase = true
                    ) ||
                    finalReport.contains(
                        "please provide",
                        ignoreCase = true
                    ) ||
                    finalReport.contains(
                        "please send",
                        ignoreCase = true
                    ) ||
                    finalReport.contains(
                        "let's proceed",
                        ignoreCase = true
                    )


        if (badResponse) {

            Log.e(
                TAG,
                "Gemma returned conversational output instead of report: $finalReport"
            )

            throw IllegalStateException(
                "AI did not generate the field report correctly. Please generate again."
            )
        }


        if (finalReport.isBlank()) {

            throw IllegalStateException(
                "Gemma returned an empty response."
            )
        }


        Log.d(
            TAG,
            "========================================"
        )


        Log.d(
            TAG,
            "GEMMA ENGINEER-GUIDED REPORT GENERATED"
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
