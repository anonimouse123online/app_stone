# Optional biometric login

The existing email/password login, registration, offline login, Remember Me checkbox,
saved email, and saved password storage are retained. Biometric login stores no password
or biometric data. No backend API was added or changed.

## Files

Modified:

- `app/build.gradle.kts`: `androidx.biometric:biometric:1.1.0`.
- `app/src/main/AndroidManifest.xml`: biometric permission.
- `app/src/main/java/MainActivity.kt`: FragmentActivity hosts BiometricPrompt;
  a biometric-enabled account starts at login instead of restoring its active token
  automatically. Notification navigation checks current authentication state.
- `app/src/main/java/com/example/capstonesample/LoginScreen.kt`: conditional biometric
  section and invalidation of its saved session after a successful password reset.
- `app/src/main/java/com/example/capstonesample/ProfileScreen.kt`: toggle in the
  existing Settings/Security sheet, plus invalidation after password changes.
- `app/src/main/java/com/example/capstonesample/data/security/TokenManager.kt`:
  records the last signed-in account. Remember Me methods are unchanged.
- `app/src/main/java/com/example/capstonesample/data/api/RetrofitClient.kt`:
  respects explicit Authorization headers for session validation and uses BASIC logging
  to avoid logging JWT response bodies and login passwords.
- `app/src/main/res/xml/backup_rules.xml` and `data_extraction_rules.xml`: exclude
  device-specific biometric preferences and ciphertext from backup/device transfer.

Created:

- `security/BiometricAuthManager.kt`: availability/enrollment checks, the AndroidX prompt,
  retryable failures, cancellation and errors.
- `security/BiometricSessionStore.kt`: per-account preferences and Keystore-protected session.
- `security/BiometricControls.kt`: reusable Security toggle and biometric login UI.
- `security/JwtSessionPolicy.kt`: fail-closed JWT expiry/not-before checks.
- `security/BiometricSessionValidator.kt`: backend JWT validation using the Dashboard's
  existing protected projects endpoint.
- `app/src/test/java/com/example/capstonesample/security/JwtSessionPolicyTest.kt`.
- `app/src/test/java/com/example/capstonesample/security/BiometricSessionValidatorTest.kt`.
- This document. The main security files are under
  `app/src/main/java/com/example/capstonesample/security/`.

## Preference and session storage

`sitepulse_biometrics` is a private SharedPreferences file. Each account has a boolean
`biometric_enabled_<SHA-256 of normalized email>`, defaulting to false, and a separate
encrypted session record. The last signed-in account identifies which option to show
when the login email is empty; a typed email selects that account's record instead.

The saved JWT and profile are encrypted using AES-GCM. Each account has its own
Android Keystore key that requires a strong biometric for every encryption/decryption
operation. BiometricPrompt's CryptoObject authorizes the actual operation. New biometric
enrollment invalidates the key. Unsupported/weak-only biometrics are unavailable for
this secure session unlock. Fingerprints/templates are handled entirely by Android.

## Toggle and login behavior

Turning ON checks device capability and the current online JWT, then displays the
biometric prompt. Only a successful biometric-authorized encryption saves the session
and enables the preference. Cancellation or a non-matching fingerprint never enables it.
Turning OFF immediately disables the preference and deletes only its biometric session
and key. Account data and normal login are unaffected.

Logout still clears the active JWT/profile through TokenManager. The biometric preference
and encrypted copy remain. On a fresh launch, an opted-in account goes to login, including
when Remember Me is checked; the saved email/password behavior itself is unchanged.

The biometric login button is shown only for an opted-in account with a nonexpired saved
session and available strong biometrics. After a successful prompt, the session is
decrypted and its JWT is sent to the existing authenticated `GET /projects` endpoint.
This endpoint rejects missing/invalid tokens; `/dashboard` is not implemented by this
backend. Only an HTTP success with `success: true` restores the original encrypted
account/profile session through the existing MainActivity login callback and navigates
to Dashboard. No active token
is installed while this validation is pending. Biometric login requires server access.

Expired or rejected sessions, changed biometric enrollment, or password resets require
email/password login. Opt-in is retained; after a new normal login, choose **Refresh
biometric login** in Security and verify again to protect the new session. Transient
network errors allow retry without deleting a usable encrypted session.

## Real-phone checks

1. Enable a screen lock and enroll a fingerprint or strong supported biometric in
   Android Settings. Install/run the debug app using Android Studio. Ensure the phone
   can reach the existing SitePulse server configured in `RetrofitClient.BASE_URL`.
2. Log in normally with an account that has never opted in. Logout: only email/password
   should appear. Registration should never display a biometric prompt.
3. Sign in online. Open **Profile → Settings → Security → Enable Biometrics**.
   Cancel the prompt: the switch must remain OFF. Try a nonmatching finger, then
   authenticate correctly: the switch should become ON only after success.
4. Logout. Confirm the OR separator and **Login with Biometrics** appear. Authenticate:
   the server must accept the saved JWT before Dashboard opens. Canceling or failing
   must leave you at login; normal login remains usable.
5. Turn the switch OFF, then logout: the biometric option must disappear, and normal
   login must still work. Confirm Remember Me still saves/autofills as before.
6. With biometrics enabled and Remember Me checked, close/reopen the app: it should
   open login, and biometric login or email/password should be available.
7. Test an expired or backend-rejected JWT: Dashboard must not open through biometrics.
   Log in with email/password, then refresh biometric login in Security.
8. Disable network/server access: biometric verification alone must not open Dashboard.
   Restore access and retry. The existing normal/offline login behavior is retained.
9. Test a phone without supported hardware, or remove enrollment: show an availability
   message and keep the ON action unavailable; an already-enabled setting can still
   be switched OFF. After enrollment changes, normal login and fresh verification
   restore biometric access.
10. Use two accounts: opting in/out for one must not change the other's preference.
    Entering a different account's email must never unlock the previous account.

Automated verification: `gradlew.bat :app:testDebugUnitTest :app:assembleDebug`.
The JWT tests cover the expiry boundary, malformed/missing/offline tokens, and future
not-before claims. Real biometric prompts require interactive phone testing.
Validator tests exercise Retrofit's actual `/projects` request and Bearer header,
including successful empty project lists, 401/403, server failures, invalid response
payloads, network failures, cancellation, and tokens expiring during validation.

## Verification of the session-validation fix (2026-10-03)

- Live backend: `/projects` returns 401 without a token and 403 for an invalid token.
- Debug build, all 12 unit tests, and lint completed successfully (lint has warnings).
- Installed the corrected debug APK as an update on the connected Android phone,
  preserving application data and existing Remember Me settings.
- Interactive biometric login: the validation request to `/projects` returned 200 OK
  and the UI moved from Login to the authenticated Dashboard. The new process did
  not request the missing `/dashboard` endpoint.
