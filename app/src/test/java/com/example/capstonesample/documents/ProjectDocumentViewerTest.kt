package com.example.capstonesample.documents

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.nio.file.Files
import com.example.capstonesample.data.api.ProjectDocumentsResponse
import com.google.gson.Gson

class ProjectDocumentViewerTest {
    private val base = "https://backend.example.test/"

    @Test
    fun resolvesUploadPathWithConfiguredBackendAndEncodesSpaces() {
        assertEquals(
            "https://backend.example.test/uploads/documents/Site%20Plan.pdf",
            projectDocumentUrl(" /uploads/documents/Site Plan.pdf ", base).toString()
        )
    }

    @Test
    fun preservesBackendProvidedAbsoluteUrl() {
        val url = "https://files.example.test/plans/a.pdf?version=2"
        assertEquals(url, projectDocumentUrl(url, base).toString())
    }

    @Test
    fun rejectsMissingAndInvalidDocumentUrls() {
        for (path in listOf("", " ", "file:///private/a.pdf", "javascript:alert(1)",
            "http://[invalid", "//other.example.test/file.pdf")) {
            assertThrows(IllegalArgumentException::class.java) { projectDocumentUrl(path, base) }
        }
    }

    @Test
    fun determinesPdfAndImageTypesWithoutTreatingQueryAsExtension() {
        assertEquals("application/pdf", projectDocumentMimeType("/uploads/a.PDF?v=2", "PDF"))
        assertEquals("image/png", projectDocumentMimeType("/uploads/image.png", "Image"))
        assertEquals("application/pdf", projectDocumentMimeType("/uploads/file", "PDF"))
    }

    @Test
    fun backendMimeTypeControlsViewingAndDocumentResponseParsesIt() {
        val response = Gson().fromJson("""
            {"success":true,"data":[{"id":"1","file_path":"/uploads/documents/file",
             "mime_type":"text/plain; charset=utf-8"}]}
        """.trimIndent(), ProjectDocumentsResponse::class.java)
        val document = response.data.single()
        assertEquals("/uploads/documents/file", document.filePath)
        assertEquals("text/plain; charset=utf-8", document.mimeType)
        assertEquals("text/plain", projectDocumentMimeType("/uploads/unknown", "", document.mimeType.orEmpty()))
        assertEquals("application/pdf", projectDocumentMimeType("/uploads/file.bin", "", "application/pdf"))
        assertEquals("image/png", projectDocumentMimeType("/uploads/file.bin", "", "image/png"))
    }

    @Test
    fun fileLoaderPreservesExistingClientAuthenticationAndRejectsAccessErrors() = runBlocking {
        val directory = Files.createTempDirectory("project_document_auth_test_").toFile()
        try {
            for (status in listOf(200, 401, 403)) {
                val client = OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        chain.proceed(chain.request().newBuilder().header("Authorization", "Bearer test-token").build())
                    }
                    .addInterceptor { chain ->
                        assertEquals("Bearer test-token", chain.request().header("Authorization"))
                        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                            .code(status).message("Test").body("Document".toResponseBody()).build()
                    }.build()
                try {
                    val file = cacheProjectDocument(directory, projectDocumentUrl("/uploads/document.txt", base), client)
                    assertEquals(200, status)
                    assertEquals("Document", file.readText())
                    file.delete()
                } catch (e: IllegalStateException) {
                    assertTrue(status == 401 || status == 403)
                    assertTrue(e.message.orEmpty().contains(if (status == 401) "session" else "access"))
                }
            }
        } finally {
            directory.listFiles().orEmpty().forEach { it.delete() }
            directory.delete()
        }
    }

    @Test
    fun fetchesOriginalBytesIntoTemporaryFileReadyForImmediateViewing() = runBlocking {
        val directory = Files.createTempDirectory("project_document_test_").toFile()
        try {
            val bytes = "%PDF-test-document".toByteArray()
            val url = projectDocumentUrl("/uploads/documents/site.pdf", base)
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                assertEquals(url, chain.request().url)
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("OK")
                    .body(bytes.toResponseBody("application/pdf".toMediaType())).build()
            }.build()
            val file = cacheProjectDocument(directory, url, client)
            assertEquals(directory.canonicalFile, checkNotNull(file.parentFile).canonicalFile)
            assertEquals("pdf", file.extension)
            assertArrayEquals(bytes, file.readBytes())
        } finally {
            directory.listFiles().orEmpty().forEach { it.delete() }
            directory.delete()
        }
    }

    @Test
    fun unavailableEmptyOrNetworkFailureLeavesNoOpenableFile() = runBlocking {
        val directory = Files.createTempDirectory("project_document_error_test_").toFile()
        try {
            for (status in listOf(404, 200, -1)) {
                val client = OkHttpClient.Builder().addInterceptor { chain ->
                    if (status == -1) throw IOException("Network unavailable")
                    Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                        .code(status).message("Test").body("".toResponseBody()).build()
                }.build()
                var failed = false
                try {
                    cacheProjectDocument(directory, projectDocumentUrl("/uploads/site.pdf", base), client)
                } catch (e: Exception) {
                    assertTrue(if (status == -1) e is IOException else e is IllegalStateException)
                    failed = true
                }
                assertTrue(failed)
                assertTrue(directory.listFiles().orEmpty().isEmpty())
            }
        } finally {
            directory.listFiles().orEmpty().forEach { it.delete() }
            directory.delete()
        }
    }
}
