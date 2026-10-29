package com.example.capstonesample.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.Gson
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Device-only, per-account opt-in and encrypted copy of an existing backend session. */
class BiometricSessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun isEnabled(email: String): Boolean = prefs.getBoolean("biometric_enabled_${accountId(email)}", false)

    fun markLastAccount(email: String) {
        prefs.edit().putString("last_account", normalize(email)).apply()
    }

    fun loginAccount(enteredEmail: String): String? {
        val email = normalize(enteredEmail).ifBlank { prefs.getString("last_account", "").orEmpty() }
        return email.takeIf { it.isNotBlank() && isEnabled(it) && hasSavedSession(it) }
    }

    fun hasSavedSession(email: String): Boolean {
        val id = accountId(email)
        val expires = prefs.getLong("expires_$id", 0)
        if (expires <= System.currentTimeMillis() / 1000) {
            // Keep opt-in, but never offer an expired token for login.
            if (prefs.contains("session_$id")) forgetSession(email)
            return false
        }
        return prefs.contains("session_$id") && prefs.contains("iv_$id")
    }

    fun needsRenewal(session: UserSession): Boolean = !hasSavedSession(session.email) ||
        prefs.getString("token_hash_${accountId(session.email)}", null) != hash(session.token)

    fun encryptionCipher(email: String): Cipher {
        val alias = alias(email)
        val store = keyStore()
        val key = if (store.containsAlias(alias)) store.getKey(alias, null) as SecretKey else {
            val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                spec.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            } else {
                @Suppress("DEPRECATION")
                spec.setUserAuthenticationValidityDurationSeconds(-1)
            }
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
                init(spec.build())
                generateKey()
            }
        }
        return Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
    }

    fun enable(session: UserSession, authenticatedCipher: Cipher) {
        require(session.email.isNotBlank() && JwtSessionPolicy.isUsable(session.token))
        val id = accountId(session.email)
        authenticatedCipher.updateAAD(id.toByteArray(Charsets.UTF_8))
        val encrypted = authenticatedCipher.doFinal(gson.toJson(session).toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString("session_$id", encode(encrypted))
            .putString("iv_$id", encode(authenticatedCipher.iv))
            .putLong("expires_$id", JwtSessionPolicy.expiresAt(session.token)!!)
            .putString("token_hash_$id", hash(session.token))
            .putBoolean("biometric_enabled_$id", true)
            .putString("last_account", normalize(session.email))
            .apply()
    }

    fun decryptionCipher(email: String): Cipher {
        check(isEnabled(email) && hasSavedSession(email))
        val iv = Base64.decode(prefs.getString("iv_${accountId(email)}", null), Base64.NO_WRAP)
        val key = keyStore().getKey(alias(email), null) as SecretKey
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        }
    }

    fun unlock(email: String, authenticatedCipher: Cipher): UserSession {
        check(isEnabled(email) && hasSavedSession(email))
        val id = accountId(email)
        authenticatedCipher.updateAAD(id.toByteArray(Charsets.UTF_8))
        val encrypted = Base64.decode(prefs.getString("session_$id", null), Base64.NO_WRAP)
        val session = gson.fromJson(String(authenticatedCipher.doFinal(encrypted), Charsets.UTF_8), UserSession::class.java)
        require(normalize(session.email) == normalize(email) && JwtSessionPolicy.isUsable(session.token))
        return session
    }

    fun disable(email: String) {
        prefs.edit().putBoolean("biometric_enabled_${accountId(email)}", false).apply()
        forgetSession(email)
    }

    /** Called on expiry/revocation/enrollment changes; leaves the user's preference intact. */
    fun forgetSession(email: String) {
        val id = accountId(email)
        prefs.edit().remove("session_$id").remove("iv_$id").remove("expires_$id")
            .remove("token_hash_$id").apply()
        runCatching { keyStore().deleteEntry(alias(email)) }
    }

    private fun alias(email: String) = "sitepulse_biometric_${accountId(email)}"
    private fun accountId(email: String) = hash(normalize(email))
    private fun normalize(email: String) = email.trim().lowercase(Locale.ROOT)
    private fun hash(value: String) = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    private fun encode(value: ByteArray) = Base64.encodeToString(value, Base64.NO_WRAP)
    private fun keyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    companion object {
        const val PREF_NAME = "sitepulse_biometrics"
    }
}
