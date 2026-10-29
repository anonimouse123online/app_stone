package com.example.capstonesample.security

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.capstonesample.data.api.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
private fun rememberBiometricManager(): BiometricAuthManager? {
    val context = LocalContext.current
    val activity = BiometricAuthManager.findActivity(context)
    val manager = remember(activity) { activity?.let { BiometricAuthManager(it) } }
    DisposableEffect(manager) {
        onDispose { manager?.cancel() }
    }
    return manager
}

/** Recheck hardware/enrollment after returning from Android Settings. */
@Composable
private fun biometricAvailability(): String? {
    val context = LocalContext.current
    val activity = BiometricAuthManager.findActivity(context)
    var unavailable by remember { mutableStateOf(BiometricAuthManager.availabilityMessage(context)) }
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) unavailable = BiometricAuthManager.availabilityMessage(context)
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }
    return unavailable
}

@Composable
fun BiometricSettingsRow(email: String, enabled: Boolean = true) {
    val context = LocalContext.current
    val store = remember(context) { BiometricSessionStore(context) }
    val manager = rememberBiometricManager()
    val unavailable = biometricAvailability()
    var optedIn by remember(email) { mutableStateOf(store.isEnabled(email)) }
    var pending by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    val session = TokenManager.getUserSession(context)?.takeIf { it.email.equals(email, ignoreCase = true) }
    val needsRenewal = remember(session, revision) { session?.let(store::needsRenewal) ?: false }

    fun verifyAndSave() {
        if (manager == null || unavailable != null) {
            message = unavailable ?: "Biometric authentication is not available on this device."
            return
        }
        if (session == null || !JwtSessionPolicy.isUsable(session.token)) {
            message = "Sign in online with email and password before enabling biometrics."
            return
        }
        try {
            val cipher = store.encryptionCipher(email)
            pending = true
            message = null
            manager.authenticate("Enable Biometrics", cipher,
                onSuccess = { authenticatedCipher ->
                    try {
                        // Do not enable a session that was changed/logged out during the prompt.
                        check(TokenManager.getUserSession(context) == session)
                        store.enable(session, authenticatedCipher)
                        optedIn = true
                        revision++
                        message = "Biometric login enabled for this account."
                    } catch (e: Exception) {
                        message = "Unable to save biometric login. Sign in online and try again."
                    } finally {
                        pending = false
                    }
                },
                onError = { pending = false; message = it },
                onFailure = { message = "Biometric not recognized. Try again or cancel." }
            )
        } catch (e: Exception) {
            store.forgetSession(email)
            revision++
            message = "Biometric security changed. Try enabling biometrics again."
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Fingerprint, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Enable Biometrics", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Use fingerprint or supported device biometrics for faster and more secure access to SitePulse.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = optedIn,
                enabled = enabled && !pending && (optedIn || (unavailable == null && manager != null)),
                onCheckedChange = { turnOn ->
                    if (turnOn) verifyAndSave() else {
                        manager?.cancel()
                        store.disable(email)
                        optedIn = false
                        revision++
                        message = "Biometric login disabled."
                    }
                }
            )
        }
        unavailable?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (optedIn && needsRenewal) {
            Text("Verify again to save your current session for biometric login.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { verifyAndSave() }, enabled = enabled && !pending && unavailable == null) {
                Text("Refresh biometric login")
            }
        }
        if (pending) LinearProgressIndicator(Modifier.fillMaxWidth())
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
fun BiometricLoginSection(
    enteredEmail: String,
    busy: Boolean,
    onBusyChange: (Boolean) -> Unit,
    onMessage: (String) -> Unit,
    onAuthenticated: (UserSession) -> Unit
) {
    val context = LocalContext.current
    val store = remember(context) { BiometricSessionStore(context) }
    val manager = rememberBiometricManager()
    val unavailable = biometricAvailability()
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    val account = remember(enteredEmail, revision, unavailable) { store.loginAccount(enteredEmail) }
    val currentOnAuthenticated by rememberUpdatedState(onAuthenticated)
    val validator = remember { BiometricSessionValidator(RetrofitClient.api) }

    if (account != null && unavailable == null && manager != null) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f))
            Text("OR", modifier = Modifier.padding(horizontal = 12.dp))
            HorizontalDivider(Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
            onClick = {
                try {
                    val cipher = store.decryptionCipher(account)
                    onBusyChange(true)
                    onMessage("")
                    manager.authenticate("Login with Biometrics", cipher,
                        onSuccess = { authenticatedCipher ->
                            val session = try {
                                store.unlock(account, authenticatedCipher)
                            } catch (e: Exception) {
                                store.forgetSession(account)
                                revision++
                                onBusyChange(false)
                                onMessage("Saved session is unavailable or expired. Sign in with email and password.")
                                null
                            }
                            if (session != null) scope.launch {
                                try {
                                    when (val result = validator.validate(session)) {
                                        BiometricSessionValidator.Result.ReauthenticationRequired -> {
                                            store.forgetSession(account)
                                            revision++
                                            onMessage("Your session has expired or been rejected. Sign in with email and password.")
                                        }
                                        is BiometricSessionValidator.Result.Accepted -> {
                                            // Restore through MainActivity's existing login callback/session manager.
                                            currentOnAuthenticated(result.session)
                                        }
                                        is BiometricSessionValidator.Result.Unavailable -> {
                                            // Status only; never log the saved token or account credentials.
                                            Log.w("BiometricLogin", "Session verification unavailable; HTTP ${result.httpCode}")
                                            onMessage("Unable to verify your session with SitePulse. Check your connection and try again or use email and password.")
                                        }
                                    }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    onMessage("Connect to the SitePulse server to use biometric login, or use email and password.")
                                } finally {
                                    onBusyChange(false)
                                }
                            }
                        },
                        onError = { onBusyChange(false); onMessage(it) },
                        onFailure = { onMessage("Biometric not recognized. Try again or cancel.") }
                    )
                } catch (e: Exception) {
                    store.forgetSession(account)
                    revision++
                    onBusyChange(false)
                    onMessage("Biometric security changed. Sign in with email and password and enable biometrics again.")
                }
            }
        ) {
            Icon(Icons.Outlined.Fingerprint, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Login with Biometrics")
        }
        Text(account, style = MaterialTheme.typography.bodySmall)
    }
}
