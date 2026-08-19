package com.example.capstonesample.data.api

import android.content.Context
import com.example.capstonesample.security.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // ============================================================
    // APP CONTEXT
    // ============================================================

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }


    // ============================================================
    // BACKEND BASE URL
    // ============================================================

    // PHYSICAL PHONE
    private const val BASE_URL =
        "http://192.168.1.4:5001/"

    // ANDROID EMULATOR
    // private const val BASE_URL =
    //     "http://10.0.2.2:5001/"


    // ============================================================
    // HTTP LOGGING
    // ============================================================

    private val loggingInterceptor =
        HttpLoggingInterceptor().apply {

            level =
                HttpLoggingInterceptor.Level.BODY
        }


    // ============================================================
    // AUTH INTERCEPTOR
    // Automatically adds:
    //
    // Authorization: Bearer <JWT>
    // ============================================================

    private val authInterceptor = { chain: okhttp3.Interceptor.Chain ->

        val originalRequest =
            chain.request()

        val requestBuilder =
            originalRequest.newBuilder()

        // Only try reading token after RetrofitClient.init()
        if (::appContext.isInitialized) {

            val token =
                TokenManager.getToken(
                    appContext
                )

            if (!token.isNullOrBlank()) {

                requestBuilder.addHeader(
                    "Authorization",
                    "Bearer $token"
                )
            }
        }

        chain.proceed(
            requestBuilder.build()
        )
    }


    // ============================================================
    // OKHTTP CLIENT
    // ============================================================

    private val client: OkHttpClient by lazy {

        OkHttpClient.Builder()

            // JWT first
            .addInterceptor(
                authInterceptor
            )

            // Logging
            .addInterceptor(
                loggingInterceptor
            )

            .connectTimeout(
                15,
                TimeUnit.SECONDS
            )

            .readTimeout(
                30,
                TimeUnit.SECONDS
            )

            .writeTimeout(
                30,
                TimeUnit.SECONDS
            )

            .build()
    }


    // ============================================================
    // RETROFIT
    // ============================================================

    val api: ApiService by lazy {

        Retrofit.Builder()

            .baseUrl(
                BASE_URL
            )

            .client(
                client
            )

            .addConverterFactory(
                GsonConverterFactory.create()
            )

            .build()

            .create(
                ApiService::class.java
            )
    }
}