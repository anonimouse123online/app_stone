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

Your ONLY task is to produce a concise professional construction field inspection report from the supplied object detection results.

The detection results come from a general-purpose YOLO model running on a construction site photo. The detected class names are general (e.g. "person", "truck", "backpack") but the photo was taken on a construction site, so interpret detections in that context.

CONTEXT MAPPING (apply when interpreting detections):
- "person" = site worker or personnel on the construction site
- "truck" = construction vehicle (dump truck, delivery truck, etc.)
- "car" = site vehicle or personnel vehicle
- "bus" = transport vehicle
- "backpack" / "handbag" / "suitcase" = worker gear or tool bag
- "chair" / "bench" = temporary site furniture
- "bottle" / "cup" = worker supplies
- "cell phone" = communication device (note potential safety concern if used while working)
- "laptop" = site management equipment
- "umbrella" = weather protection on site
- "potted plant" = landscaping element near the construction area
- Any other object = describe it as-is, noting it was observed at the site

OBJECT DETECTION RESULTS:
$imageDescription

STRICT OUTPUT RULES:

1. Output ONLY the inspection report.
2. Do NOT greet the user.
3. Do NOT say "Okay", "Sure", "Here is", "Initial draft", or similar phrases.
4. Do NOT ask questions at the end.
5. Do NOT offer to refine, expand, or modify the report.
6. Do NOT use Markdown symbols such as **, *, #, ---, or backticks.
7. Do NOT repeat these instructions.
8. Do NOT invent objects, activities, hazards, defects, progress, or PPE that are NOT in the detection results.
9. YOLO confidence represents detection confidence ONLY. NEVER interpret it as safety compliance, quality, or work completion.
10. Since this is a general detector, you CANNOT determine PPE compliance (helmets, vests, harnesses). State this limitation clearly.
11. Do NOT estimate work completion percentage.
12. Keep the report concise and factual.
13. Use plain text only.
14. Do not repeat identical detections individually.

DETECTION CONSOLIDATION:

If the same class appears multiple times, consolidate it.

Example input:
- person: 91%
- person: 88%
- person: 86%
- truck: 86%
- truck: 67%

Write:
Helmet - 4 detections, highest confidence 91%
Safety Vest - 2 detections, highest confidence 86%
Personnel - 3 detections (highest confidence 91%)
Vehicle (truck) - 2 detections (highest confidence 86%)

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

DETECTED OBJECTS AND PERSONNEL:
[Consolidated detection results with construction-context interpretation.]

SITE ACTIVITY ASSESSMENT:
[Based on detected objects, describe possible site activities.]
[If only generic objects like persons are detected, state that specific construction activity cannot be determined from available detections.]

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