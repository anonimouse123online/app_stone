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

        // Already initialized
        if (engine != null) {

            Log.d(
                TAG,
                "Gemma engine already initialized."
            )

            return@withContext
        }


        // ========================================================
        // CHECK MODEL FILE
        // ========================================================

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


        // ========================================================
        // ENGINE CONFIGURATION
        // ========================================================
        //
        // Start with CPU while testing.
        //
        // GPU can be enabled later once inference works reliably.
        //
        // ========================================================

        val config =
            EngineConfig(

                modelPath =
                    modelPath,

                backend =
                    Backend.CPU(),

                cacheDir =
                    context.cacheDir.absolutePath
            )


        // ========================================================
        // CREATE ENGINE
        // ========================================================

        val newEngine =
            Engine(
                config
            )


        try {

            /*
             * initialize() can take several seconds.
             *
             * That's why this function runs on
             * Dispatchers.Default rather than the UI thread.
             */
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
        imageDescription: String
    ): String = withContext(
        Dispatchers.Default
    ) {

        val currentEngine =
            engine
                ?: throw IllegalStateException(
                    "Gemma has not been initialized."
                )


        if (imageDescription.isBlank()) {

            throw IllegalArgumentException(
                "Image description is empty."
            )
        }


        Log.d(
            TAG,
            "Generating SitePulse AI report..."
        )


        Log.d(
            TAG,
            "YOLO input:\n$imageDescription"
        )


        // ========================================================
        // PROMPT
        // ========================================================

        val prompt =
            """
You are SitePulse AI, an automated construction field inspection reporting system.

Your ONLY task is to produce a concise professional construction field inspection report from the supplied YOLO detection results.

YOLO DETECTION RESULTS:
$imageDescription

STRICT OUTPUT RULES:

1. Output ONLY the inspection report.
2. Do NOT greet the user.
3. Do NOT say "Okay", "Sure", "Here is", "Initial draft", or similar phrases.
4. Do NOT ask questions at the end.
5. Do NOT offer to refine, expand, or modify the report.
6. Do NOT use Markdown symbols such as **, *, #, ---, or backticks.
7. Do NOT repeat these instructions.
8. Do NOT invent objects, activities, hazards, defects, progress, or PPE.
9. YOLO confidence represents detection confidence ONLY.
10. NEVER interpret detection confidence as:
    - safety compliance percentage
    - PPE effectiveness
    - quality
    - work completion
    - installation completion
11. Do NOT say PPE is adequate, properly worn, correctly fitted, maintained, or compliant unless that information is explicitly provided.
12. Do NOT infer missing PPE unless a class such as without_helmet or without_vest was actually detected.
13. Do NOT infer construction progress from helmet or vest detections.
14. Do NOT estimate a work completion percentage unless one is explicitly supplied.
15. Keep the report concise and factual.
16. Use plain text only.
17. Do not repeat identical detections individually.

DETECTION CONSOLIDATION:

If the same class appears multiple times, consolidate it.

Example input:
helmet: 91%
helmet: 88%
helmet: 86%
vest: 86%
vest: 67%

Write:
Helmet - 4 detections, highest confidence 91%
Safety Vest - 2 detections, highest confidence 86%

Do not list every duplicate detection unless they represent different classes.

PPE INTERPRETATION:

helmet:
State only that safety helmet(s) were detected.

vest:
State only that safety vest(s) were detected.

without_helmet:
State that potential missing helmet PPE was detected and requires verification by site personnel.

without_vest:
State that potential missing safety vest PPE was detected and requires verification by site personnel.

If only PPE is detected, DO NOT attempt to determine construction work progress.

Use EXACTLY this structure:

SITEPULSE AI FIELD INSPECTION REPORT

DETECTED OBJECTS AND PPE:
[Consolidated detection results.]

SITE ACTIVITY ASSESSMENT:
[State only activities directly supported by detected construction objects.]
[If only PPE was detected, write exactly:
"The specific construction activity cannot be reliably determined from the available visual evidence."]

SAFETY AND PPE OBSERVATION:
[Describe only detected PPE or explicitly detected missing-PPE classes.]
[Do not determine overall PPE compliance from helmet/vest detection alone.]

WORK PROGRESS OBSERVATION:
[Describe visible work-related evidence only.]
[If detections contain only PPE, write exactly:
"Work progress cannot be reliably determined from the detected PPE alone."]

FIELD ASSESSMENT:
[Provide a maximum of 2 concise sentences based strictly on the detections.]

RECOMMENDED ACTION:
[Provide 1 or 2 relevant verification actions only.]

OVERALL SUMMARY:
[Provide a maximum of 2 concise sentences.]

AI DISCLAIMER:
"This AI-generated assessment is based solely on computer-vision detections from the submitted image. Final verification of site conditions, safety compliance, and work progress must be performed by authorized site personnel."
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

                        /*
                         * LiteRT-LM Flow returns Message objects.
                         *
                         * Convert the streamed Message into text.
                         */
                        val chunk =
                            message.toString()


                        if (chunk.isNotBlank()) {

                            result.append(
                                chunk
                            )
                        }
                    }
            }


        // ========================================================
        // FINAL RESULT
        // ========================================================

        val finalReport =
            result
                .toString()
                .trim()


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
            "GEMMA REPORT GENERATED"
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