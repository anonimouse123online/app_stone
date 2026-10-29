package com.example.capstonesample.security

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher

/** A prompt never authenticates with SitePulse: it authorizes one Keystore operation. */
class BiometricAuthManager(private val activity: FragmentActivity) {
    private var success: ((Cipher) -> Unit)? = null
    private var error: ((String) -> Unit)? = null
    private var failure: (() -> Unit)? = null
    private val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val cipher = result.cryptoObject?.cipher
                val onSuccess = success
                val onError = error
                clearCallbacks()
                if (cipher != null) onSuccess?.invoke(cipher)
                else onError?.invoke("Unable to unlock the saved session. Use email and password.")
            }

            override fun onAuthenticationFailed() {
                // A non-match is retryable; Android keeps the prompt open.
                failure?.invoke()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val onError = error
                clearCallbacks()
                onError?.invoke(errString.toString())
            }
        })

    fun authenticate(
        title: String,
        cipher: Cipher,
        onSuccess: (Cipher) -> Unit,
        onError: (String) -> Unit,
        onFailure: () -> Unit
    ) {
        val unavailable = availabilityMessage(activity)
        if (unavailable != null) {
            onError(unavailable)
            return
        }
        success = onSuccess
        error = onError
        failure = onFailure
        try {
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle("Verify your biometrics for SitePulse")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .setNegativeButtonText("Cancel")
                    .build(),
                BiometricPrompt.CryptoObject(cipher)
            )
        } catch (e: Exception) {
            clearCallbacks()
            onError("Unable to start biometric authentication. Use email and password.")
        }
    }

    fun cancel() {
        clearCallbacks()
        prompt.cancelAuthentication()
    }

    private fun clearCallbacks() {
        success = null
        error = null
        failure = null
    }

    companion object {
        fun availabilityMessage(context: Context): String? = when (
            BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        ) {
            BiometricManager.BIOMETRIC_SUCCESS -> null
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                "Biometric authentication is not available on this device."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                "Biometric hardware is temporarily unavailable. Try again later."
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                "Enroll a fingerprint or supported strong biometric in your phone's Settings first."
            else -> "Supported secure biometrics are not available on this device."
        }

        fun findActivity(context: Context): FragmentActivity? = when (context) {
            is FragmentActivity -> context
            is ContextWrapper -> findActivity(context.baseContext)
            else -> null
        }
    }
}
