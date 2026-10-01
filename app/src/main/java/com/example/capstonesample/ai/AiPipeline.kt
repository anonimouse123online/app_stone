package com.example.capstonesample.ai

import android.content.Context
import android.net.Uri
import android.util.Log

import com.example.capstonesample.data.model.FieldAnnotation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class AiPipeline(
    context: Context
) {

    companion object {

        private const val TAG =
            "AI_PIPELINE"
    }


    // ============================================================
    // APP CONTEXT
    // ============================================================

    private val appContext =
        context.applicationContext


    // ============================================================
    // MODEL STATUS
    // ============================================================

    private val modelStatusChecker =
        ModelStatusChecker(
            appContext
        )


    // ============================================================
    // YOLO
    // ============================================================

    // YOLO is kept in the pipeline so your existing
    // image processing code does not need to be removed.
    //
    // IMPORTANT:
    // YOLO is now SECONDARY evidence only.
    private val yolo =
        YoloDetector(
            appContext =
                appContext
        )


    // ============================================================
    // GEMMA
    // ============================================================

    private val gemma =
        GemmaReportGenerator(

            context =
                appContext,

            modelPath =
                modelStatusChecker.getGemmaPath()
        )


    // ============================================================
    // INITIALIZE AI
    // ============================================================

    suspend fun initialize() =
        withContext(
            Dispatchers.IO
        ) {

            Log.d(
                TAG,
                "Preparing bundled Gemma model..."
            )


            val ready =
                modelStatusChecker
                    .ensureGemmaInstalled()


            if (!ready) {

                throw IllegalStateException(
                    "Bundled Gemma model could not be prepared."
                )
            }


            Log.d(
                TAG,
                "Gemma model ready."
            )


            gemma.initialize()


            Log.d(
                TAG,
                "Gemma initialized."
            )
        }


    // ============================================================
    // IMAGE + ENGINEER LABELS → GEMMA
    // ============================================================

    suspend fun analyzeImage(

        imageUri: Uri,

        annotations:
        List<FieldAnnotation> =
            emptyList()

    ): AiAnalysisResult =
        withContext(
            Dispatchers.Default
        ) {


            Log.d(
                TAG,
                "Starting AI field analysis."
            )


            // ========================================================
            // YOLO
            // ========================================================

            // YOLO still runs so you do not need to remove your
            // existing detector/model.
            //
            // However, its result is NOT allowed to control the
            // construction progress report.

            val detections =

                try {

                    yolo.detect(
                        imageUri
                    )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "YOLO detection failed.",
                        e
                    )

                    emptyList()
                }


            Log.d(
                TAG,
                "YOLO detections: ${detections.size}"
            )


            // ========================================================
            // YOLO DETECTION SUMMARY
            // ========================================================

            val detectionSummary =

                if (
                    detections.isEmpty()
                ) {

                    "No supporting visual objects were detected."

                } else {

                    detections
                        .sortedByDescending {
                            it.confidence
                        }
                        .joinToString(
                            separator =
                                "\n"
                        ) { detection ->


                            val confidence =
                                (
                                        detection.confidence *
                                                100f
                                        )
                                    .toInt()
                                    .coerceIn(
                                        0,
                                        100
                                    )


                            "- ${detection.label}: $confidence%"
                        }
                }


            Log.d(
                TAG,
                "Secondary YOLO observations:\n$detectionSummary"
            )


            // ========================================================
            // ENGINEER ANNOTATIONS
            // ========================================================

            val hasEngineerAnnotations =
                annotations.isNotEmpty()


            val engineerAnnotationSummary =

                if (annotations.isEmpty()) {

                    "NO_ENGINEER_ANNOTATIONS"

                } else {

                    annotations
                        .mapIndexed { index, annotation ->

                            buildString {

                                appendLine("AREA_${index + 1}")
                                appendLine("WORK_TYPE=${annotation.label}")
                                appendLine("STAGE=${annotation.stage}")

                                if (annotation.progress != null) {
                                    appendLine("PROGRESS_PERCENT=${annotation.progress}")
                                } else {
                                    appendLine("PROGRESS_PERCENT=NOT_PROVIDED")
                                }
                            }
                        }
                        .joinToString(
                            separator = "\n\n"
                        )
                }


            Log.d(
                TAG,
                "Engineer annotations:\n$engineerAnnotationSummary"
            )


            // ========================================================
            // REPORT INPUT
            // ========================================================

            /*
             * IMPORTANT LOGIC
             *
             * Engineer annotations are the authoritative source.
             *
             * If at least one engineer annotation exists:
             * - report is based on engineer annotations
             * - YOLO is only supplemental
             * - YOLO must not override progress
             * - YOLO must not override stage
             * - YOLO confidence must never become progress %
             *
             * If no engineer annotations exist:
             * - AI should return that there is insufficient
             *   engineer-provided evidence for progress reporting
             * - YOLO may still be displayed separately
             */

            val primaryEngineerInput =
                if (hasEngineerAnnotations) {

                    """
                    ENGINEER-PROVIDED FIELD EVIDENCE

                    The following annotations were manually entered
                    by the site engineer and are the PRIMARY and
                    AUTHORITATIVE evidence for this report.

                    $engineerAnnotationSummary

                    REPORTING RULES:

                    1. Base the construction progress report primarily
                       on the engineer-provided work labels above.

                    2. Treat the engineer's work type, stage, and
                       progress percentage as authoritative field data.

                    3. Do NOT change, estimate, or replace the engineer's
                       progress percentage using image detection confidence.

                    4. Do NOT interpret YOLO confidence as construction
                       completion progress.

                    5. Do NOT make random detected objects such as helmets,
                       persons, chairs, bottles, tools, or similar objects
                       the main subject of the report.

                    6. Only mention a YOLO observation when it directly
                       supports the engineer-labeled construction activity.

                    7. If YOLO conflicts with the engineer annotation,
                       keep the engineer annotation as the primary record.

                    8. Do not invent construction activities that were
                       not entered by the engineer.

                    9. Write the report in professional civil/site
                       engineering language.

                    10. Clearly state the current work activity,
                        current stage, and engineer-reported progress.

                    11. Avoid claiming that the exact progress percentage
                        was visually verified unless there is enough
                        evidence to do so.

                    12. The final report should focus on construction
                        progress, not object detection.
                    """.trimIndent()

                } else {

                    """
                    NO ENGINEER-PROVIDED WORK LABELS ARE AVAILABLE.

                    Do not generate a construction progress percentage
                    from YOLO detections.

                    Do not infer work stage from object detection.

                    State that engineer-provided work-area annotations
                    are required before a reliable progress report can
                    be generated.
                    """.trimIndent()
                }


            // ========================================================
            // YOLO SUPPORTING INPUT
            // ========================================================

            val secondaryVisualInput =
                if (hasEngineerAnnotations) {

                    """
                    SECONDARY VISUAL OBSERVATIONS ONLY

                    These are automated YOLO detections.

                    They are NOT authoritative construction progress data.
                    They must NOT override engineer labels.

                    $detectionSummary
                    """.trimIndent()

                } else {

                    """
                    Automated YOLO observations:

                    $detectionSummary

                    These detections must not be converted into
                    construction progress or completion percentage.
                    """.trimIndent()
                }


            // ========================================================
            // GEMMA REPORT
            // ========================================================

            val report =
                gemma.generateReport(

                    /*
                     * We intentionally put the engineer-driven
                     * instructions into the main description so
                     * Gemma cannot treat YOLO as the primary source.
                     */

                    imageDescription =
                        primaryEngineerInput,

                    /*
                     * YOLO is placed in the secondary parameter.
                     *
                     * This prevents random object detections from
                     * becoming the center of the report.
                     */

                    engineerAnnotations =
                        secondaryVisualInput
                )


            Log.d(
                TAG,
                "Gemma engineer-guided report generated."
            )


            // ========================================================
            // RESULT
            // ========================================================

            AiAnalysisResult(

                detections =
                    detections,

                detectionSummary =
                    detectionSummary,

                report =
                    report
            )
        }


    // ============================================================
    // RELEASE
    // ============================================================

    fun release() {

        try {

            yolo.close()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Error closing YOLO.",
                e
            )
        }


        try {

            gemma.close()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Error closing Gemma.",
                e
            )
        }


        Log.d(
            TAG,
            "AI pipeline released."
        )
    }
}


// ============================================================
// AI RESULT
// ============================================================

data class AiAnalysisResult(

    val detections:
    List<Detection>,

    val detectionSummary:
    String,

    val report:
    String
)