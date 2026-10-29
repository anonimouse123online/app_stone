package com.example.capstonesample.security

import com.example.capstonesample.data.api.ApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.ByteString.Companion.encodeUtf8
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class BiometricSessionValidatorTest {
    private val session = UserSession(
        token = "header.${"{\"exp\":2000}".encodeUtf8().base64Url().trimEnd('=')}.signature",
        fullName = "Test Engineer",
        email = "engineer@example.test",
        role = "engineer"
    )

    private fun validator(
        status: Int = 200,
        body: String = "{\"success\":true,\"data\":[]}",
        clock: () -> Long = { 1000 },
        onRequest: (Request) -> Unit = {}
    ): BiometricSessionValidator {
        // Exercise the real Retrofit ApiService/JSON converter without a network server.
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            onRequest(request)
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                .code(status).message("Test response")
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
        }.build()
        val api = Retrofit.Builder().baseUrl("http://127.0.0.1/")
            .client(client).addConverterFactory(GsonConverterFactory.create())
            .build().create(ApiService::class.java)
        return BiometricSessionValidator(api, clock)
    }

    @Test
    fun acceptedSessionUsesProtectedProjectsRouteAndExactBearerToken() = runBlocking {
        var requestCount = 0
        val result = validator(onRequest = { request ->
            requestCount++
            assertEquals("GET", request.method)
            assertEquals("/projects", request.url.encodedPath)
            assertEquals("Bearer ${session.token}", request.header("Authorization"))
        }).validate(session)
        assertEquals(1, requestCount)
        assertTrue(result is BiometricSessionValidator.Result.Accepted)
        assertSame(session, (result as BiometricSessionValidator.Result.Accepted).session)
    }

    @Test
    fun rejectedJwtRequiresNormalLogin() = runBlocking {
        for (status in listOf(401, 403)) {
            assertEquals(BiometricSessionValidator.Result.ReauthenticationRequired,
                validator(status, "{\"success\":false}").validate(session))
        }
    }

    @Test
    fun absentRouteAndServerFailureNeverRestoreSession() = runBlocking {
        for (status in listOf(404, 500, 503)) {
            assertEquals(BiometricSessionValidator.Result.Unavailable(status),
                validator(status, "{\"success\":false}").validate(session))
        }
    }

    @Test
    fun successfulHttpResponseStillRequiresBackendSuccess() = runBlocking {
        for (body in listOf("{\"success\":false,\"data\":[]}", "{\"data\":[]}", "null")) {
            assertEquals(BiometricSessionValidator.Result.Unavailable(200),
                validator(body = body).validate(session))
        }
        assertTrue(validator(status = 204, body = "").validate(session) is
            BiometricSessionValidator.Result.Unavailable)
        assertTrue(validator(body = "not JSON").validate(session) is
            BiometricSessionValidator.Result.Unavailable)
    }

    @Test
    fun expiredAndOfflineSessionsDoNotCallTheBackend() = runBlocking {
        var requestCount = 0
        val validator = validator(clock = { 2000 }, onRequest = { requestCount++ })
        assertEquals(BiometricSessionValidator.Result.ReauthenticationRequired, validator.validate(session))
        assertEquals(BiometricSessionValidator.Result.ReauthenticationRequired,
            validator.validate(session.copy(token = "LOCAL_123")))
        assertEquals(0, requestCount)
    }

    @Test
    fun expiryDuringBackendRequestCannotRestoreSession() = runBlocking {
        var now = 1000L
        val result = validator(clock = { now }, onRequest = { now = 2000L }).validate(session)
        assertEquals(BiometricSessionValidator.Result.ReauthenticationRequired, result)
    }

    @Test
    fun networkFailureLeavesSessionUnavailable() = runBlocking {
        val result = validator(onRequest = { throw IOException("Test connection failure") }).validate(session)
        assertEquals(BiometricSessionValidator.Result.Unavailable(), result)
    }

    @Test
    fun cancellationPropagatesWithoutRestoringSession() = runBlocking {
        var clockCalls = 0
        val validator = validator(clock = {
            // Cancel after the backend responds, before navigation can be authorized.
            if (++clockCalls == 1) 1000L else throw CancellationException("Test canceled")
        })
        try {
            validator.validate(session)
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            // Expected: the coroutine cannot restore the account after leaving login.
        }
    }
}
