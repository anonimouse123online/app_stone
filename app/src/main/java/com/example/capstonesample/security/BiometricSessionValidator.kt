package com.example.capstonesample.security

import com.example.capstonesample.data.api.ApiService
import kotlinx.coroutines.CancellationException

/** Validate the decrypted JWT using the same protected endpoint the Dashboard uses. */
class BiometricSessionValidator(
    private val api: ApiService,
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000 }
) {
    sealed interface Result {
        data class Accepted(val session: UserSession) : Result
        data object ReauthenticationRequired : Result
        data class Unavailable(val httpCode: Int? = null) : Result
    }

    suspend fun validate(session: UserSession): Result {
        if (!JwtSessionPolicy.isUsable(session.token, nowSeconds())) {
            return Result.ReauthenticationRequired
        }
        return try {
            // This request does not install a token in TokenManager. The active session
            // is restored by MainActivity only after the server accepts this exact JWT.
            val response = api.getProjects("Bearer ${session.token}")
            when {
                response.code() == 401 || response.code() == 403 ||
                    !JwtSessionPolicy.isUsable(session.token, nowSeconds()) ->
                    Result.ReauthenticationRequired
                response.isSuccessful && response.body()?.success == true ->
                    // Account/profile identity comes from the authenticated AES-GCM record.
                    // A projects response has no user profile and an empty list is valid.
                    Result.Accepted(session)
                else -> Result.Unavailable(response.code())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Unavailable()
        }
    }
}
