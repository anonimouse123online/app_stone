package com.example.capstonesample.security

import android.content.Context

data class UserSession(
    val token: String,
    val fullName: String,
    val email: String,
    val role: String
)

object TokenManager {

    private const val PREF_NAME = "sitepulse_auth"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_FULL_NAME = "logged_in_full_name"
    private const val KEY_EMAIL = "logged_in_email"
    private const val KEY_ROLE = "logged_in_role"

    // Remember Me preferences
    private const val KEY_REMEMBER_ME = "remember_me"
    private const val KEY_SAVED_EMAIL = "saved_email"
    private const val KEY_SAVED_PASSWORD = "saved_password"

    fun saveToken(
        context: Context,
        token: String
    ) {
        val prefs =
            context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        prefs.edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    fun getToken(
        context: Context
    ): String? {
        val prefs =
            context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        return prefs.getString(
            KEY_TOKEN,
            null
        )
    }

    fun clearToken(
        context: Context
    ) {
        val prefs =
            context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_FULL_NAME)
            .remove(KEY_EMAIL)
            .remove(KEY_ROLE)
            .apply()
    }

    // ============================================================
    // SESSION PERSISTENCE
    // ============================================================

    fun saveSession(
        context: Context,
        token: String,
        fullName: String,
        email: String,
        role: String
    ) {
        val prefs =
            context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_FULL_NAME, fullName)
            .putString(KEY_EMAIL, email)
            .putString(KEY_ROLE, role)
            .apply()
    }

    fun getUserSession(
        context: Context
    ): UserSession? {
        val prefs =
            context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        if (token.isBlank()) return null

        val fullName = prefs.getString(KEY_FULL_NAME, "") ?: ""
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val role = prefs.getString(KEY_ROLE, "") ?: ""

        return UserSession(
            token = token,
            fullName = fullName,
            email = email,
            role = role
        )
    }

    fun clearSession(
        context: Context
    ) {
        val prefs =
            context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        // Clear active auth token and active user details
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_FULL_NAME)
            .remove(KEY_EMAIL)
            .remove(KEY_ROLE)
            .apply()
    }

    // ============================================================
    // REMEMBER ME CREDENTIALS
    // ============================================================

    fun saveRememberMe(
        context: Context,
        rememberMe: Boolean,
        email: String = "",
        password: String = ""
    ) {
        val prefs =
            context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        if (rememberMe) {
            prefs.edit()
                .putBoolean(KEY_REMEMBER_ME, true)
                .putString(KEY_SAVED_EMAIL, email)
                .putString(KEY_SAVED_PASSWORD, password)
                .apply()
        } else {
            prefs.edit()
                .putBoolean(KEY_REMEMBER_ME, false)
                .remove(KEY_SAVED_EMAIL)
                .remove(KEY_SAVED_PASSWORD)
                .apply()
        }
    }

    fun isRememberMe(
        context: Context
    ): Boolean {
        val prefs =
            context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        return prefs.getBoolean(
            KEY_REMEMBER_ME,
            false
        )
    }

    fun getSavedEmail(
        context: Context
    ): String {
        val prefs =
            context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        return prefs.getString(
            KEY_SAVED_EMAIL,
            ""
        ) ?: ""
    }

    fun getSavedPassword(
        context: Context
    ): String {
        val prefs =
            context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )

        return prefs.getString(
            KEY_SAVED_PASSWORD,
            ""
        ) ?: ""
    }
}