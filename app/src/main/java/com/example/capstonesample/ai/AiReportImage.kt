package com.example.capstonesample.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import com.example.capstonesample.data.api.ApiService
import com.example.capstonesample.data.model.FieldAnnotation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import retrofit2.Response
import java.io.File
import kotlin.math.max
import kotlin.math.min

/** A private byte-for-byte copy shared by analysis, preview and report submission. */
class AiReportImage private constructor(
    val taskId: String,
    val file: File,
    private val mimeType: String
) {
    val uri: Uri get() = Uri.fromFile(file)
    var uploadedDate: String? = null
        private set
    private var uploadedReference: String? = null

    suspend fun uploadReference(api: ApiService): String {
        uploadedReference?.let { return it }

        // Remember a successful upload even if reference lookup/report submission fails.
        if (uploadedDate == null) {
            val response = api.uploadTaskImage(
                taskId,
                MultipartBody.Part.createFormData(
                    "images", file.name, file.asRequestBody(mimeType.toMediaType())
                )
            )
            val body = response.body()
            if (!response.isSuccessful || body?.success != true) {
                throw IllegalStateException(serverError(response)
                    ?: body?.message ?: body?.error ?: "Unable to upload report image.")
            }
            uploadedDate = body.date
                ?: throw IllegalStateException("Image upload did not return its date.")
        }

        // This backend returns a count/date on upload; resolve our unique filename
        // from its saved batch, without ever choosing the latest/first image.
        val response = api.getTaskImages(taskId, checkNotNull(uploadedDate))
        val body = response.body()
        if (!response.isSuccessful || body?.success != true) {
            throw IllegalStateException(serverError(response)
                ?: body?.error ?: "Unable to retrieve the uploaded report image. Retry submission.")
        }
        return findReference(body.images, taskId, checkNotNull(uploadedDate), file.name)
            .also { uploadedReference = it }
    }

    companion object {
        suspend fun snapshot(
            context: Context,
            taskId: String,
            source: Uri,
            annotations: List<FieldAnnotation> = emptyList()
        ): AiReportImage =
            withContext(Dispatchers.IO) {
                val mimeType = context.contentResolver.getType(source) ?: "image/jpeg"
                val file = File.createTempFile(
                    "ai_report_", if (mimeType.contains("png", ignoreCase = true)) ".png" else ".jpg", context.cacheDir
                )
                try {
                    if (annotations.isNotEmpty()) {
                        val options = BitmapFactory.Options().apply {
                            inMutable = true
                        }
                        val decoded = context.contentResolver.openInputStream(source)?.use { stream ->
                            BitmapFactory.decodeStream(stream, null, options)
                        } ?: throw IllegalStateException("Unable to read the selected report image.")

                        val annotatedBitmap = drawAnnotationsOnBitmap(decoded, annotations)
                        file.outputStream().use { out ->
                            val format = if (mimeType.contains("png", ignoreCase = true)) {
                                Bitmap.CompressFormat.PNG
                            } else {
                                Bitmap.CompressFormat.JPEG
                            }
                            annotatedBitmap.compress(format, 92, out)
                        }
                        annotatedBitmap.recycle()
                    } else {
                        val input = context.contentResolver.openInputStream(source)
                            ?: throw IllegalStateException("Unable to read the selected report image.")
                        input.use { stream -> file.outputStream().use { stream.copyTo(it) } }
                    }
                    check(file.length() > 0) { "The selected report image is empty." }
                    AiReportImage(taskId, file, mimeType)
                } catch (e: Exception) {
                    file.delete()
                    throw e
                }
            }

        internal fun drawAnnotationsOnBitmap(
            bitmap: Bitmap,
            annotations: List<FieldAnnotation>
        ): Bitmap {
            val mutableBitmap = if (bitmap.isMutable) {
                bitmap
            } else {
                val copy = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                bitmap.recycle()
                copy ?: bitmap
            }
            val canvas = Canvas(mutableBitmap)

            val greenColor = Color.rgb(16, 185, 129) // #10B981 (TaskDetailGreen)
            val whiteColor = Color.WHITE

            val baseWidth = 1080f
            val scale = (mutableBitmap.width.toFloat() / baseWidth).coerceIn(0.5f, 6.0f)

            val strokePaint = Paint().apply {
                color = greenColor
                style = Paint.Style.STROKE
                strokeWidth = 8f * scale
                isAntiAlias = true
            }

            val handlePaint = Paint().apply {
                color = greenColor
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            val pillPaint = Paint().apply {
                color = greenColor
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            val textPaint = Paint().apply {
                color = whiteColor
                textSize = 32f * scale
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }

            val fontMetrics = textPaint.fontMetrics
            val textHeight = fontMetrics.descent - fontMetrics.ascent

            for (annotation in annotations) {
                val l = (annotation.left * mutableBitmap.width).coerceIn(0f, mutableBitmap.width.toFloat())
                val t = (annotation.top * mutableBitmap.height).coerceIn(0f, mutableBitmap.height.toFloat())
                val r = (annotation.right * mutableBitmap.width).coerceIn(0f, mutableBitmap.width.toFloat())
                val b = (annotation.bottom * mutableBitmap.height).coerceIn(0f, mutableBitmap.height.toFloat())

                val boxLeft = min(l, r)
                val boxRight = max(l, r)
                val boxTop = min(t, b)
                val boxBottom = max(t, b)

                // 1. Draw bounding box rectangle
                canvas.drawRect(boxLeft, boxTop, boxRight, boxBottom, strokePaint)

                // 2. Draw 4 corner handles
                val handleRadius = 10f * scale
                canvas.drawCircle(boxLeft, boxTop, handleRadius, handlePaint)
                canvas.drawCircle(boxRight, boxTop, handleRadius, handlePaint)
                canvas.drawCircle(boxLeft, boxBottom, handleRadius, handlePaint)
                canvas.drawCircle(boxRight, boxBottom, handleRadius, handlePaint)

                // 3. Draw label badge (e.g. "tanawa • 35%")
                val labelText = buildString {
                    append(annotation.label.trim())
                    annotation.progress?.let {
                        append(" • $it%")
                    }
                }

                if (labelText.isNotEmpty()) {
                    val textWidth = textPaint.measureText(labelText)
                    val padX = 14f * scale
                    val padY = 8f * scale
                    val pillWidth = textWidth + (padX * 2f)
                    val pillHeight = textHeight + (padY * 2f)
                    val cornerRadius = 8f * scale

                    var pillLeft = boxLeft
                    var pillTop = boxTop - (pillHeight * 0.7f)
                    if (pillTop < 0f) {
                        pillTop = boxTop
                    }
                    if (pillLeft + pillWidth > mutableBitmap.width) {
                        pillLeft = (mutableBitmap.width - pillWidth).coerceAtLeast(0f)
                    }
                    val pillRight = (pillLeft + pillWidth).coerceAtMost(mutableBitmap.width.toFloat())
                    val pillBottom = (pillTop + pillHeight).coerceAtMost(mutableBitmap.height.toFloat())

                    val pillRect = RectF(pillLeft, pillTop, pillRight, pillBottom)
                    canvas.drawRoundRect(pillRect, cornerRadius, cornerRadius, pillPaint)

                    val textX = pillLeft + padX
                    val textY = (pillTop + pillBottom) / 2f - (fontMetrics.descent + fontMetrics.ascent) / 2f
                    canvas.drawText(labelText, textX, textY, textPaint)
                }
            }

            return mutableBitmap
        }

        internal fun findReference(paths: List<String>, taskId: String, date: String, filename: String): String {
            val taskDirectory = "/uploads/tasks/$taskId/$date/"
            val matches = paths.mapNotNull { storedPath ->
                val path = storedPath.replace('\\', '/')
                val start = path.indexOf(taskDirectory)
                if (start < 0) return@mapNotNull null
                val savedName = path.substring(start + taskDirectory.length)
                val prefix = savedName.removeSuffix("_$filename")
                if (!savedName.endsWith("_$filename") || prefix.isEmpty() ||
                    !prefix.all { it in '0'..'9' }) return@mapNotNull null
                taskDirectory + savedName
            }.distinct()
            return matches.singleOrNull()
                ?: throw IllegalStateException("Cannot identify the exact uploaded report image. Retry submission.")
        }

        private fun serverError(response: Response<*>): String? = runCatching {
            val body = JSONObject(response.errorBody()?.string().orEmpty())
            body.optString("message").ifBlank { body.optString("error") }.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
