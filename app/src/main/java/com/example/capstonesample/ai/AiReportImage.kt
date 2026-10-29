package com.example.capstonesample.ai

import android.content.Context
import android.net.Uri
import com.example.capstonesample.data.api.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import retrofit2.Response
import java.io.File

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
        suspend fun snapshot(context: Context, taskId: String, source: Uri): AiReportImage =
            withContext(Dispatchers.IO) {
                val mimeType = context.contentResolver.getType(source) ?: "image/jpeg"
                val file = File.createTempFile(
                    "ai_report_", if (mimeType == "image/png") ".png" else ".jpg", context.cacheDir
                )
                try {
                    val input = context.contentResolver.openInputStream(source)
                        ?: throw IllegalStateException("Unable to read the selected report image.")
                    input.use { stream -> file.outputStream().use { stream.copyTo(it) } }
                    check(file.length() > 0) { "The selected report image is empty." }
                    AiReportImage(taskId, file, mimeType)
                } catch (e: Exception) {
                    file.delete()
                    throw e
                }
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
