package com.example.capstonesample

import com.example.capstonesample.data.api.RetrofitClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class ChatImageUrlTest {
    private val base = "http://172.24.36.52:5001/"

    @Test
    fun relativeAndLegacyUploadUrlsUseConfiguredBackend() {
        val expected = "${base}uploads/messages/photo.jpg"
        assertEquals(expected, chatAttachmentUrl("/uploads/messages/photo.jpg", base))
        assertEquals(expected, chatAttachmentUrl("uploads/messages/photo.jpg", base))
        assertEquals(expected, chatAttachmentUrl("http://localhost:5001/uploads/messages/photo.jpg", base))
        assertEquals(expected, chatAttachmentUrl("http://192.168.1.4:5001/uploads/messages/photo.jpg", base))
    }

    @Test
    fun invalidUrlsFailGracefullyAndExternalImagesRemainExternal() {
        assertNull(chatAttachmentUrl(null, base))
        assertNull(chatAttachmentUrl(" ", base))
        assertNull(chatAttachmentUrl("file:///private/image.jpg", base))
        assertNull(chatAttachmentUrl("//external.example/image.jpg", base))
        assertEquals("https://external.example/image.jpg", chatAttachmentUrl("https://external.example/image.jpg", base))
    }

    @Test
    fun authorizationRequiresExactBackendOrigin() {
        val backend = RetrofitClient.BASE_URL.toHttpUrl()
        assertTrue(RetrofitClient.isBackendOrigin(backend.resolve("/uploads/messages/photo.jpg")!!))
        assertFalse(RetrofitClient.isBackendOrigin(backend.newBuilder().host("external.example").build()))
        assertFalse(RetrofitClient.isBackendOrigin(backend.newBuilder().port(backend.port + 1).build()))
        assertFalse(RetrofitClient.isBackendOrigin(backend.newBuilder().scheme("https").build()))
    }
}
