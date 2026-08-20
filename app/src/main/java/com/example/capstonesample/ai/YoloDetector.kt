package com.example.capstonesample.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.util.Log

import org.tensorflow.lite.Interpreter

import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min


data class Detection(
    val label: String,
    val confidence: Float,
    val box: RectF
)


class YoloDetector(

    private val appContext: Context,

    modelAssetPath: String = "yolo11n.tflite",

    labelsAssetPath: String = "labels.txt",

    private val inputSize: Int = 640,

    private val confThreshold: Float = 0.25f,

    private val iouThreshold: Float = 0.45f

) {

    companion object {

        private const val TAG =
            "YOLO_TEST"
    }


    private val interpreter: Interpreter

    private val labels: List<String>


    // ============================================================
    // INITIALIZE YOLO
    // ============================================================

    init {

        Log.d(
            TAG,
            "Initializing YOLO..."
        )


        // --------------------------------------------------------
        // LOAD LABELS
        // --------------------------------------------------------

        labels =
            appContext.assets
                .open(labelsAssetPath)
                .bufferedReader()
                .useLines {

                        lines ->

                    lines
                        .map {
                            it.trim()
                        }
                        .filter {
                            it.isNotBlank()
                        }
                        .toList()
                }


        Log.d(
            TAG,
            "Labels loaded: ${labels.size}"
        )


        // --------------------------------------------------------
        // LOAD TFLITE MODEL
        // --------------------------------------------------------

        val modelBuffer =
            loadModelFile(
                modelAssetPath
            )


        val options =
            Interpreter.Options().apply {

                setNumThreads(4)
            }


        interpreter =
            Interpreter(
                modelBuffer,
                options
            )


        Log.d(
            TAG,
            "YOLO model loaded successfully."
        )


        // --------------------------------------------------------
        // PRINT MODEL INFORMATION
        // --------------------------------------------------------

        try {

            val inputTensor =
                interpreter.getInputTensor(0)

            val outputTensor =
                interpreter.getOutputTensor(0)


            Log.d(
                TAG,
                "Input shape: ${
                    inputTensor.shape()
                        .contentToString()
                }"
            )


            Log.d(
                TAG,
                "Output shape: ${
                    outputTensor.shape()
                        .contentToString()
                }"
            )


            Log.d(
                TAG,
                "Input type: ${inputTensor.dataType()}"
            )


            Log.d(
                TAG,
                "Output type: ${outputTensor.dataType()}"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Unable to read model tensor info.",
                e
            )
        }
    }


    // ============================================================
    // DETECT FROM URI
    // ============================================================

    fun detect(
        uri: Uri
    ): List<Detection> {

        Log.d(
            TAG,
            "Detection requested from URI."
        )


        val bitmap =
            appContext
                .contentResolver
                .openInputStream(uri)
                ?.use {

                    BitmapFactory.decodeStream(it)
                }


        if (bitmap == null) {

            Log.e(
                TAG,
                "Unable to decode image URI."
            )

            return emptyList()
        }


        return detect(
            bitmap
        )
    }


    // ============================================================
    // DETECT FROM BITMAP
    // ============================================================

    fun detect(
        bitmap: Bitmap
    ): List<Detection> {

        try {

            Log.d(
                TAG,
                "YOLO detection started."
            )


            Log.d(
                TAG,
                "Original bitmap: ${bitmap.width}x${bitmap.height}"
            )


            // ----------------------------------------------------
            // RESIZE IMAGE
            // ----------------------------------------------------

            val resizedBitmap =
                Bitmap.createScaledBitmap(
                    bitmap,
                    inputSize,
                    inputSize,
                    true
                )


            // ----------------------------------------------------
            // CONVERT IMAGE TO FLOAT BUFFER
            // ----------------------------------------------------

            val inputBuffer =
                convertBitmapToByteBuffer(
                    resizedBitmap
                )


            // ----------------------------------------------------
            // READ OUTPUT SHAPE
            // ----------------------------------------------------

            val outputTensor =
                interpreter.getOutputTensor(0)

            val outputShape =
                outputTensor.shape()


            Log.d(
                TAG,
                "Output tensor shape: ${
                    outputShape.contentToString()
                }"
            )


            if (
                outputShape.size != 3
            ) {

                Log.e(
                    TAG,
                    "Unsupported YOLO output shape."
                )

                return emptyList()
            }


            // Usually:
            //
            // [1, 84, 8400]
            //
            // 4 box values +
            // 80 class confidence values
            // ----------------------------------------------------

            val dimension1 =
                outputShape[1]

            val dimension2 =
                outputShape[2]


            val detections =

                if (
                    dimension1 <
                    dimension2
                ) {

                    // Example:
                    // [1, 84, 8400]

                    val output =
                        Array(1) {

                            Array(
                                dimension1
                            ) {

                                FloatArray(
                                    dimension2
                                )
                            }
                        }


                    interpreter.run(
                        inputBuffer,
                        output
                    )


                    parseChannelsFirstOutput(
                        output[0],
                        bitmap.width,
                        bitmap.height
                    )

                } else {

                    // Possible:
                    // [1, 8400, 84]

                    val output =
                        Array(1) {

                            Array(
                                dimension1
                            ) {

                                FloatArray(
                                    dimension2
                                )
                            }
                        }


                    interpreter.run(
                        inputBuffer,
                        output
                    )


                    parsePredictionsFirstOutput(
                        output[0],
                        bitmap.width,
                        bitmap.height
                    )
                }


            val finalDetections =
                nonMaximumSuppression(
                    detections
                )


            Log.d(
                TAG,
                "Final detections: ${finalDetections.size}"
            )


            finalDetections.forEach {

                Log.d(
                    TAG,
                    "Detected: ${it.label} " +
                            "confidence=${it.confidence}"
                )
            }


            return finalDetections

        } catch (e: Exception) {

            Log.e(
                TAG,
                "YOLO detection failed.",
                e
            )

            return emptyList()
        }
    }


    // ============================================================
    // PARSE [1, CHANNELS, PREDICTIONS]
    //
    // Example:
    // [1, 84, 8400]
    // ============================================================

    private fun parseChannelsFirstOutput(

        output: Array<FloatArray>,

        originalWidth: Int,

        originalHeight: Int

    ): List<Detection> {

        val detections =
            mutableListOf<Detection>()


        val channels =
            output.size

        if (
            channels < 5
        ) {

            return detections
        }


        val predictions =
            output[0].size


        val classCount =
            min(
                labels.size,
                channels - 4
            )


        for (
        index in 0 until predictions
        ) {

            val centerX =
                output[0][index]

            val centerY =
                output[1][index]

            val width =
                output[2][index]

            val height =
                output[3][index]


            var bestScore =
                0f

            var bestClass =
                -1


            for (
            classIndex in 0 until classCount
            ) {

                val score =
                    output[
                        classIndex + 4
                    ][index]


                if (
                    score > bestScore
                ) {

                    bestScore =
                        score

                    bestClass =
                        classIndex
                }
            }


            if (
                bestScore <
                confThreshold ||
                bestClass < 0
            ) {

                continue
            }


            addDetection(

                detections =
                    detections,

                centerX =
                    centerX,

                centerY =
                    centerY,

                width =
                    width,

                height =
                    height,

                classIndex =
                    bestClass,

                confidence =
                    bestScore,

                originalWidth =
                    originalWidth,

                originalHeight =
                    originalHeight
            )
        }


        return detections
    }


    // ============================================================
    // PARSE [1, PREDICTIONS, CHANNELS]
    //
    // Example:
    // [1, 8400, 84]
    // ============================================================

    private fun parsePredictionsFirstOutput(

        output: Array<FloatArray>,

        originalWidth: Int,

        originalHeight: Int

    ): List<Detection> {

        val detections =
            mutableListOf<Detection>()


        for (
        prediction in output
        ) {

            if (
                prediction.size < 5
            ) {

                continue
            }


            val centerX =
                prediction[0]

            val centerY =
                prediction[1]

            val width =
                prediction[2]

            val height =
                prediction[3]


            val classCount =
                min(
                    labels.size,
                    prediction.size - 4
                )


            var bestScore =
                0f

            var bestClass =
                -1


            for (
            classIndex in 0 until classCount
            ) {

                val score =
                    prediction[
                        classIndex + 4
                    ]


                if (
                    score > bestScore
                ) {

                    bestScore =
                        score

                    bestClass =
                        classIndex
                }
            }


            if (
                bestScore <
                confThreshold ||
                bestClass < 0
            ) {

                continue
            }


            addDetection(

                detections =
                    detections,

                centerX =
                    centerX,

                centerY =
                    centerY,

                width =
                    width,

                height =
                    height,

                classIndex =
                    bestClass,

                confidence =
                    bestScore,

                originalWidth =
                    originalWidth,

                originalHeight =
                    originalHeight
            )
        }


        return detections
    }


    // ============================================================
    // ADD DETECTION
    // ============================================================

    private fun addDetection(

        detections:
        MutableList<Detection>,

        centerX: Float,

        centerY: Float,

        width: Float,

        height: Float,

        classIndex: Int,

        confidence: Float,

        originalWidth: Int,

        originalHeight: Int

    ) {

        // Most Ultralytics exported models return
        // normalized xywh values in this stage.
        //
        // Handle both normalized and pixel-style values.
        // --------------------------------------------------------

        val normalized =
            centerX <= 1.5f &&
                    centerY <= 1.5f &&
                    width <= 1.5f &&
                    height <= 1.5f


        val scaleX =
            if (normalized) {

                originalWidth.toFloat()

            } else {

                originalWidth.toFloat() /
                        inputSize.toFloat()
            }


        val scaleY =
            if (normalized) {

                originalHeight.toFloat()

            } else {

                originalHeight.toFloat() /
                        inputSize.toFloat()
            }


        val left =
            (centerX - width / 2f) *
                    scaleX

        val top =
            (centerY - height / 2f) *
                    scaleY

        val right =
            (centerX + width / 2f) *
                    scaleX

        val bottom =
            (centerY + height / 2f) *
                    scaleY


        val safeLeft =
            left.coerceIn(
                0f,
                originalWidth.toFloat()
            )

        val safeTop =
            top.coerceIn(
                0f,
                originalHeight.toFloat()
            )

        val safeRight =
            right.coerceIn(
                0f,
                originalWidth.toFloat()
            )

        val safeBottom =
            bottom.coerceIn(
                0f,
                originalHeight.toFloat()
            )


        if (
            safeRight <= safeLeft ||
            safeBottom <= safeTop
        ) {

            return
        }


        detections.add(

            Detection(

                label =
                    labels.getOrElse(
                        classIndex
                    ) {
                        "class_$classIndex"
                    },

                confidence =
                    confidence,

                box =
                    RectF(
                        safeLeft,
                        safeTop,
                        safeRight,
                        safeBottom
                    )
            )
        )
    }


    // ============================================================
    // PREPROCESS BITMAP
    // ============================================================

    private fun convertBitmapToByteBuffer(
        bitmap: Bitmap
    ): ByteBuffer {

        /*
         * MODEL INPUT:
         *
         * [1, 3, 640, 640]
         *
         * This is NCHW format:
         *
         * N = batch
         * C = channels
         * H = height
         * W = width
         *
         * Therefore the buffer must contain:
         *
         * ALL RED values
         * then
         * ALL GREEN values
         * then
         * ALL BLUE values
         */

        val pixelCount =
            inputSize * inputSize


        val buffer =
            ByteBuffer.allocateDirect(
                4 *          // FLOAT32
                        3 *          // RGB
                        pixelCount
            )


        buffer.order(
            ByteOrder.nativeOrder()
        )


        val pixels =
            IntArray(
                pixelCount
            )


        bitmap.getPixels(
            pixels,
            0,
            inputSize,
            0,
            0,
            inputSize,
            inputSize
        )


        // ============================================================
        // RED CHANNEL
        // ============================================================

        for (pixel in pixels) {

            val red =
                ((pixel shr 16) and 0xFF) /
                        255.0f


            buffer.putFloat(
                red
            )
        }


        // ============================================================
        // GREEN CHANNEL
        // ============================================================

        for (pixel in pixels) {

            val green =
                ((pixel shr 8) and 0xFF) /
                        255.0f


            buffer.putFloat(
                green
            )
        }


        // ============================================================
        // BLUE CHANNEL
        // ============================================================

        for (pixel in pixels) {

            val blue =
                (pixel and 0xFF) /
                        255.0f


            buffer.putFloat(
                blue
            )
        }


        buffer.rewind()


        Log.d(
            TAG,
            "Input buffer prepared in NCHW format."
        )


        return buffer
    }


    // ============================================================
    // NON MAXIMUM SUPPRESSION
    // ============================================================

    private fun nonMaximumSuppression(

        detections:
        List<Detection>

    ): List<Detection> {

        if (
            detections.isEmpty()
        ) {

            return emptyList()
        }


        val sorted =
            detections
                .sortedByDescending {
                    it.confidence
                }
                .toMutableList()


        val selected =
            mutableListOf<Detection>()


        while (
            sorted.isNotEmpty()
        ) {

            val best =
                sorted.removeAt(0)


            selected.add(
                best
            )


            val iterator =
                sorted.iterator()


            while (
                iterator.hasNext()
            ) {

                val candidate =
                    iterator.next()


                if (
                    candidate.label ==
                    best.label
                ) {

                    val overlap =
                        calculateIoU(
                            best.box,
                            candidate.box
                        )


                    if (
                        overlap >
                        iouThreshold
                    ) {

                        iterator.remove()
                    }
                }
            }
        }


        return selected
    }


    // ============================================================
    // IOU
    // ============================================================

    private fun calculateIoU(

        first: RectF,

        second: RectF

    ): Float {

        val intersectionLeft =
            max(
                first.left,
                second.left
            )


        val intersectionTop =
            max(
                first.top,
                second.top
            )


        val intersectionRight =
            min(
                first.right,
                second.right
            )


        val intersectionBottom =
            min(
                first.bottom,
                second.bottom
            )


        val intersectionWidth =
            max(
                0f,
                intersectionRight -
                        intersectionLeft
            )


        val intersectionHeight =
            max(
                0f,
                intersectionBottom -
                        intersectionTop
            )


        val intersectionArea =
            intersectionWidth *
                    intersectionHeight


        val firstArea =
            first.width() *
                    first.height()


        val secondArea =
            second.width() *
                    second.height()


        val unionArea =
            firstArea +
                    secondArea -
                    intersectionArea


        if (
            unionArea <= 0f
        ) {

            return 0f
        }


        return intersectionArea /
                unionArea
    }


    // ============================================================
    // LOAD MODEL
    // ============================================================

    private fun loadModelFile(

        assetName: String

    ): ByteBuffer {

        val fileDescriptor =
            appContext.assets
                .openFd(
                    assetName
                )


        FileInputStream(
            fileDescriptor.fileDescriptor
        ).use {

                inputStream ->


            val fileChannel =
                inputStream.channel


            return fileChannel.map(

                FileChannel.MapMode.READ_ONLY,

                fileDescriptor.startOffset,

                fileDescriptor.declaredLength
            )
        }
    }


    // ============================================================
    // CLOSE
    // ============================================================

    fun close() {

        try {

            interpreter.close()


            Log.d(
                TAG,
                "YOLO detector closed."
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Error closing YOLO.",
                e
            )
        }
    }
}