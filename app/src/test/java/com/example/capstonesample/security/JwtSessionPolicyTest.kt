package com.example.capstonesample.security

import okio.ByteString.Companion.encodeUtf8
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JwtSessionPolicyTest {
    private fun token(payload: String) = "header.${payload.encodeUtf8().base64Url().trimEnd('=')}.signature"

    @Test
    fun expiryBoundaryRequiresPasswordLogin() {
        assertTrue(JwtSessionPolicy.isUsable(token("{\"exp\":1001}"), 1000))
        assertFalse(JwtSessionPolicy.isUsable(token("{\"exp\":1000}"), 1000))
        assertFalse(JwtSessionPolicy.isUsable(token("{\"exp\":999}"), 1000))
    }

    @Test
    fun offlineMissingAndMalformedTokensCannotUnlockBackendSession() {
        for (value in listOf("", "LOCAL_123", "a.b", "a.@@@.b", token("not json"),
            token("{}"), token("{\"exp\":\"9999\"}"), token("{\"exp\":null}"),
            token("{\"exp\":-1}"), token("{\"exp\":1001.5}"))) {
            assertFalse(value, JwtSessionPolicy.isUsable(value, 1000))
        }
    }

    @Test
    fun notYetValidTokensAreRejected() {
        assertFalse(JwtSessionPolicy.isUsable(token("{\"exp\":2000,\"nbf\":1001}"), 1000))
        assertTrue(JwtSessionPolicy.isUsable(token("{\"exp\":2000,\"nbf\":1000}"), 1000))
        assertFalse(JwtSessionPolicy.isUsable(token("{\"exp\":2000,\"nbf\":\"invalid\"}"), 1000))
    }
}
