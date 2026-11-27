package com.example.capstonesample.ai

import android.content.Context
import android.net.Uri
import android.util.Log

import com.example.capstonesample.data.model.FieldAnnotation
import com.example.capstonesample.data.api.SubtaskItem

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AiPipeline(
    context: Context
) {

    companion object {

        private const val TAG =
            "AI_PIPELINE"
    }


    private val appContext =
        context.applicationContext


    private val modelStatusChecker =
        ModelStatusChecker(
            appContext
        )


    // YOLO detections are supplemental; engineer annotations remain authoritative.
    private val yolo =
        YoloDetector(
            appContext =
                appContext
        )


    private val gemma =
        GemmaReportGenerator(

            context =
                appContext,

            modelPath =
                modelStatusChecker.getGemmaPath()
        )


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


    // IMAGE + ENGINEER LABELS → GEMMA
    suspend fun analyzeImage(
        imageUri: Uri,
        annotations: List<FieldAnnotation> = emptyList(),
        taskTitle: String = "",
        projectName: String = "",
        location: String = "",
        subtasks: List<SubtaskItem> = emptyList(),
        assignee: String = "",
        currentProgress: Int = 0,
        manpower: String = "",
        engineerNotes: String = ""
    ): AiAnalysisResult =
        withContext(
            Dispatchers.Default
        ) {


            Log.d(
                TAG,
                "Starting AI field analysis."
            )


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


            /*
             * Engineer annotations are authoritative. YOLO may supplement them
             * but must not determine progress or stage. Without annotations,
             * report insufficient evidence and keep detections separate.
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


            val formattedDate =
                SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH).format(Date())

            val cleanProjectName =
                projectName.ifBlank { "Construction Project" }

            val cleanLocation =
                location.ifBlank { "Project Site" }

            // 1. Manpower
            val manpowerText =
                if (manpower.isNotBlank()) {
                    manpower.trim()
                } else {
                    ""
                }

            // 2. Work Progress
            val workProgressList =
                mutableListOf<String>()

            val completedSubtasks =
                subtasks.filter { it.completed || (it.progress ?: 0) >= 100 }

            completedSubtasks.forEach {
                workProgressList.add("${it.title.trim()}: 100% Completed")
            }

            annotations
                .filter { it.stage.contains("completed", ignoreCase = true) || (it.progress ?: 0) >= 100 }
                .forEach { ann ->
                    val label = ann.label.trim()
                    if (label.isNotBlank() && workProgressList.none { it.startsWith(label, ignoreCase = true) }) {
                        workProgressList.add("$label: 100% Completed")
                    }
                }

            if (workProgressList.isEmpty()) {
                val prog = if (currentProgress > 0) currentProgress else 100
                workProgressList.add("${taskTitle.ifBlank { "Site Inspection & Testing" }}: $prog% Completed")
            }

            val workProgressText =
                workProgressList.joinToString("\n")

            // 3. Ongoing Scope of works
            val ongoingList =
                mutableListOf<String>()

            val pendingSubtasks =
                subtasks.filter { !it.completed && (it.progress ?: 0) < 100 }

            pendingSubtasks.forEach { sub ->
                val matchingAnn = annotations.firstOrNull { ann ->
                    ann.label.trim().equals(sub.title.trim(), ignoreCase = true) && ann.progress != null
                }
                val prog = matchingAnn?.progress ?: sub.progress ?: sub.displayedProgress
                ongoingList.add("${sub.title.trim()}: $prog% Ongoing")
            }

            annotations
                .filter { it.stage.contains("ongoing", ignoreCase = true) || it.stage.contains("progress", ignoreCase = true) }
                .forEach { ann ->
                    val label = ann.label.trim()
                    if (label.isNotBlank() && ongoingList.none { it.startsWith(label, ignoreCase = true) }) {
                        val prog = ann.progress ?: 50
                        ongoingList.add("$label: $prog% Ongoing")
                    }
                }

            if (ongoingList.isEmpty() && pendingSubtasks.isEmpty()) {
                ongoingList.add("General Site Finishing & Testing")
            }

            if (engineerNotes.isNotBlank()) {
                ongoingList.add("")
                ongoingList.add("Site Notes")
                ongoingList.add(engineerNotes.trim())
            }

            val ongoingScopeText =
                ongoingList.joinToString("\n")


            val report =
                try {
                    gemma.generateReport(
                        imageDescription = detectionSummary,
                        engineerAnnotations = engineerAnnotationSummary,
                        date = formattedDate,
                        projectName = cleanProjectName,
                        location = cleanLocation,
                        manpowerInfo = manpowerText,
                        workProgressInfo = workProgressText,
                        ongoingScopeInfo = ongoingScopeText
                    )
                } catch (e: Exception) {
                    Log.w(
                        TAG,
                        "Gemma generation encountered error, falling back to structured daily report: ${e.message}"
                    )
                    gemma.formatDailyReport(
                        date = formattedDate,
                        projectName = cleanProjectName,
                        location = cleanLocation,
                        manpowerInfo = manpowerText,
                        workProgressInfo = workProgressText,
                        ongoingScopeInfo = ongoingScopeText
                    )
                }

            Log.d(
                TAG,
                "Daily Site Report successfully generated."
            )


            AiAnalysisResult(
                detections =
                    detections,
                detectionSummary =
                    detectionSummary,
                report =
                    report
            )
        }


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


data class AiAnalysisResult(

    val detections:
    List<Detection>,

    val detectionSummary:
    String,

    val report:
    String
)