package com.example.capstonesample.security

import com.google.gson.JsonParser
import okio.ByteString.Companion.decodeBase64

/** Expiry checks only. The backend must still verify the token's signature and authorization. */
object JwtSessionPolicy {
    fun expiresAt(token: String): Long? = runCatching {
        val parts = token.split('.')
        require(parts.size == 3 && parts.all { it.isNotBlank() })
        val payload = parts[1].decodeBase64()?.utf8() ?: return null
        val claims = JsonParser.parseString(payload).asJsonObject
        val expiry = claims.getAsJsonPrimitive("exp") ?: return null
        require(expiry.isNumber)
        expiry.asString.toLongOrNull()?.takeIf { it > 0 }
    }.getOrNull()

    fun isUsable(token: String, nowSeconds: Long = System.currentTimeMillis() / 1000): Boolean {
        val expiry = expiresAt(token) ?: return false
        if (expiry <= nowSeconds) return false
        return runCatching {
            val payload = token.split('.')[1].decodeBase64()!!.utf8()
            val claims = JsonParser.parseString(payload).asJsonObject
            val notBefore = claims.get("nbf")
            notBefore == null || (notBefore.asJsonPrimitive.isNumber &&
                (notBefore.asString.toLongOrNull() ?: return false) <= nowSeconds)
        }.getOrDefault(false)
    }
}
