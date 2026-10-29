package com.example.capstonesample.ai

import com.example.capstonesample.data.model.UploadTaskReportRequest
import com.example.capstonesample.data.model.UploadTaskImageResponse
import com.example.capstonesample.data.model.TaskImagesResponse
import com.example.capstonesample.data.api.ApiService
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.MultipartBody
import okio.Buffer
import retrofit2.Response
import java.io.File
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class AiReportImageTest {
    private val task = "f2352812-023e-49a1-927a-d7655382115e"
    private val date = "2026-10-07"
    private val filename = "ai_report_123456.jpg"
    private val reference = "/uploads/tasks/$task/$date/1770000000000_$filename"

    @Test
    fun resolvesOnlyAnalyzedImageAmongOldAndConcurrentUploads() {
        val paths = listOf(
            "C:\\backend\\uploads\\tasks\\$task\\$date\\1769999999999_old.jpg",
            "C:\\backend" + reference.replace('/', '\\'),
            "/backend/uploads/tasks/$task/$date/1770000000001_other.jpg"
        )
        assertEquals(reference, AiReportImage.findReference(paths, task, date, filename))
    }

    @Test
    fun rejectsAnotherTaskOrDateAndSimilarFilename() {
        val paths = listOf(
            reference.replace(task, "another-task"),
            reference.replace(date, "2026-10-06"),
            reference.replace(filename, "other_$filename")
        )
        assertThrows(IllegalStateException::class.java) {
            AiReportImage.findReference(paths, task, date, filename)
        }
    }

    @Test
    fun rejectsMissingOrAmbiguousImageInsteadOfChoosingLatest() {
        assertThrows(IllegalStateException::class.java) {
            AiReportImage.findReference(emptyList(), task, date, filename)
        }
        assertThrows(IllegalStateException::class.java) {
            AiReportImage.findReference(
                listOf(reference, reference.replace("1770000000000", "1770000000001")),
                task, date, filename
            )
        }
    }

    @Test
    fun reportJsonIncludesOnlyItsImageAndUploadDateWithExistingFields() {
        val json = JsonParser.parseString(Gson().toJson(UploadTaskReportRequest(
            taskId = task,
            projectCode = "PROJECT-1",
            title = "AI Field Report",
            reportText = "Generated report",
            images = listOf(reference),
            reportDate = date
        ))).asJsonObject
        assertEquals(task, json["task_id"].asString)
        assertEquals("PROJECT-1", json["project_code"].asString)
        assertEquals("Generated report", json["report_text"].asString)
        assertEquals("AI Field Report", json["report_type"].asString)
        assertEquals(date, json["report_date"].asString)
        assertEquals(1, json["images"].asJsonArray.size())
        assertEquals(reference, json["images"].asJsonArray[0].asString)
        assertFalse(json.has("reportDate"))
    }

    @Test
    fun retriesReferenceLookupWithoutUploadingImageAgain() = runBlocking {
        val file = File.createTempFile("ai_report_test_", ".jpg")
        try {
            val originalBytes = byteArrayOf(1, 2, 3, 4, 5)
            file.writeBytes(originalBytes)
            val constructor = AiReportImage::class.java.getDeclaredConstructor(
                String::class.java, File::class.java, String::class.java
            ).apply { isAccessible = true }
            val image = constructor.newInstance(task, file, "image/jpeg")
            val storedReference = "/uploads/tasks/$task/$date/1770000000000_${file.name}"
            var uploads = 0
            var lookups = 0
            val api = Proxy.newProxyInstance(
                ApiService::class.java.classLoader, arrayOf(ApiService::class.java)
            ) { _, method, args ->
                when (method.name) {
                    "uploadTaskImage" -> {
                        uploads++
                        assertEquals(task, args[0])
                        val part = args[1] as MultipartBody.Part
                        assertEquals("image/jpeg", part.body.contentType().toString())
                        assertEquals("form-data; name=\"images\"; filename=\"${file.name}\"",
                            part.headers?.get("Content-Disposition"))
                        val buffer = Buffer()
                        part.body.writeTo(buffer)
                        org.junit.Assert.assertArrayEquals(originalBytes, buffer.readByteArray())
                        Response.success(UploadTaskImageResponse(success = true, date = date))
                    }
                    "getTaskImages" -> {
                        lookups++
                        assertEquals(task, args[0])
                        assertEquals(date, args[1])
                        Response.success(TaskImagesResponse(
                            success = true,
                            images = if (lookups == 1) emptyList() else listOf(storedReference)
                        ))
                    }
                    else -> error("Unexpected API call: ${method.name}")
                }
            } as ApiService
            var lookupFailed = false
            try {
                image.uploadReference(api)
            } catch (_: IllegalStateException) {
                lookupFailed = true
            }
            org.junit.Assert.assertTrue(lookupFailed)
            assertEquals(date, image.uploadedDate)
            assertEquals(storedReference, image.uploadReference(api))
            assertEquals(storedReference, image.uploadReference(api))
            assertEquals(1, uploads)
            assertEquals(2, lookups)
        } finally {
            file.delete()
        }
    }
}
