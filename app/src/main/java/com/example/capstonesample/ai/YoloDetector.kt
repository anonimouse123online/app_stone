package com.example.capstonesample.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

/**
 * Single detected object.
 * box coordinates are scaled to the ORIGINAL bitmap size (not the 640x640 model input).
 */
data class Detection(
    val label: String,
    val confidence: Float,
    val box: RectF
)

/**
 * Wraps the trained YOLO11n .tflite model (4 classes: helmet, vest, without_helmet, without_vest).
 *
 * Confirmed real output shape from the LiteRT export log: [1, 8, 8400]
 * (4 box coords + 4 classes = 8 attributes, 8400 candidate boxes). Single output tensor -
 * NOT three separate boxes/scores/classes tensors.
 */
class YoloDetector(
    private val appContext: Context,
    modelAssetPath: String = "yolo11n.tflite",
    labelsAssetPath: String = "labels.txt",
    private val inputSize: Int = 640,
    private val confThreshold: Float = 0.25f,
    private val iouThreshold: Float = 0.45f
) {

    private val interpreter: Interpreter
    private val labels: List<String>

    init {
        val model = loadModelFile(appContext, modelAssetPath)
        val options = Interpreter.Options().apply {
            numThreads = 4
        }
        interpreter = Interpreter(model, options)
        labels = appContext.assets.open(labelsAssetPath).bufferedReader().readLines()
            .filter { it.isNotBlank() }
    }

    private fun loadModelFile(context: Context, assetPath: String): ByteBuffer {
        val bytes = context.assets.open(assetPath).readBytes()
        return ByteBuffer.allocateDirect(bytes.size).apply {
            order(ByteOrder.nativeOrder())
            put(bytes)
            rewind()
        }
    }

    /** Convenience overload: decode a Bitmap from a content Uri, then run detection.
     * Always call from a background coroutine (Dispatchers.Default/IO) - never the main thread. */
    fun detect(uri: Uri): List<Detection> {
        val bitmap = uriToBitmap(appContext, uri)
        return detect(bitmap)
    }

    private fun uriToBitmap(context: Context, uri: Uri): Bitmap {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            context.contentResolver.openInputStream(uri).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }

        // Camera photos frequently carry an EXIF rotation tag rather than storing pixels
        // already right-side-up. Without correcting for it, the model sees a sideways or
        // upside-down image, which can tank detection confidence to near zero.
        val rotationDegrees = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }

        return if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    fun detect(bitmap: Bitmap): List<Detection> {
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val inputBuffer = bitmapToInputBuffer(resized)

        // Real shape confirmed from export log: [1, 8, 8400] -> read dynamically so this
        // keeps working even if you retrain with a different number of classes later.
        val outputShape = interpreter.getOutputTensor(0).shape() // [1, numAttrs, numBoxes]
        val numAttrs = outputShape[1]
        val numBoxes = outputShape[2]
        val numClasses = numAttrs - 4

        val output = Array(1) { Array(numAttrs) { FloatArray(numBoxes) } }

        // Single output tensor -> plain run(), NOT runForMultipleInputsOutputs
        interpreter.run(inputBuffer, output)

        val scaleX = bitmap.width.toFloat() / inputSize
        val scaleY = bitmap.height.toFloat() / inputSize

        val candidates = mutableListOf<Detection>()
        var maxScoreSeen = 0f

        for (i in 0 until numBoxes) {
            var bestClass = -1
            var bestScore = 0f
            for (c in 0 until numClasses) {
                val score = output[0][4 + c][i]
                if (score > bestScore) {
                    bestScore = score
                    bestClass = c
                }
            }
            if (bestScore > maxScoreSeen) maxScoreSeen = bestScore
            if (bestScore < confThreshold) continue

            val cx = output[0][0][i] * inputSize
            val cy = output[0][1][i] * inputSize
            val w = output[0][2][i] * inputSize
            val h = output[0][3][i] * inputSize

            val left = (cx - w / 2f) * scaleX
            val top = (cy - h / 2f) * scaleY
            val right = (cx + w / 2f) * scaleX
            val bottom = (cy + h / 2f) * scaleY

            candidates.add(
                Detection(
                    label = labels.getOrElse(bestClass) { "class_$bestClass" },
                    confidence = bestScore,
                    box = RectF(left, top, right, bottom)
                )
            )
        }

        android.util.Log.d("YoloDetector", "Max confidence seen across all boxes: $maxScoreSeen (threshold: $confThreshold)")

        return nonMaxSuppression(candidates, iouThreshold)
    }

    private fun nonMaxSuppression(detections: List<Detection>, iouThresh: Float): List<Detection> {
        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val result = mutableListOf<Detection>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            result.add(best)
            sorted.removeAll { iou(best.box, it.box) > iouThresh && it.label == best.label }
        }
        return result
    }

    private fun iou(a: RectF, b: RectF): Float {
        val interLeft = max(a.left, b.left)
        val interTop = max(a.top, b.top)
        val interRight = min(a.right, b.right)
        val interBottom = min(a.bottom, b.bottom)

        val interArea = max(0f, interRight - interLeft) * max(0f, interBottom - interTop)
        val aArea = (a.right - a.left) * (a.bottom - a.top)
        val bArea = (b.right - b.left) * (b.bottom - b.top)

        return if (aArea + bArea - interArea <= 0f) 0f else interArea / (aArea + bArea - interArea)
    }

    private fun bitmapToInputBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF) / 255f) // R
            buffer.putFloat(((pixel shr 8) and 0xFF) / 255f)  // G
            buffer.putFloat((pixel and 0xFF) / 255f)          // B
        }
        buffer.rewind()
        return buffer
    }

    fun close() {
        interpreter.close()
    }
}