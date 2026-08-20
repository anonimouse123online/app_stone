package com.example.capstonesample.ai

import android.content.Context
import android.net.Uri
import android.util.Log

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

    suspend fun initialize() = withContext(
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
    // IMAGE → YOLO → GEMMA
    // ============================================================

    suspend fun analyzeImage(
        imageUri: Uri
    ): AiAnalysisResult = withContext(
        Dispatchers.Default
    ) {

        Log.d(
            TAG,
            "Starting AI field analysis."
        )


        // ========================================================
        // YOLO
        // ========================================================

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
        // DETECTION SUMMARY
        // ========================================================

        val detectionSummary =

            if (
                detections.isEmpty()
            ) {

                "No recognizable objects were detected."

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
            "Detection summary:\n$detectionSummary"
        )


        // ========================================================
        // GEMMA REPORT
        // ========================================================

        val report =
            gemma.generateReport(
                detectionSummary
            )


        Log.d(
            TAG,
            "Gemma report generated."
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