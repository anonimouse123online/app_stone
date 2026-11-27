package com.example.capstonesample.data.api

import android.content.Context
import com.example.capstonesample.security.TokenManager
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    internal fun isBackendOrigin(url: HttpUrl): Boolean {
        val backend = BASE_URL.toHttpUrl()
        return url.scheme == backend.scheme && url.host == backend.host && url.port == backend.port
    }

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }


<<<<<<< HEAD
    const val BASE_URL = "http://192.168.254.110:5001/"
=======
    // ============================================================
    // BACKEND BASE URL
    // ============================================================

    // PHYSICAL PHONE 

    const val BASE_URL = "http://192.168.254.132:5001/"
>>>>>>> 832bdbbdbc8d87fa94e46afc2308761e8d217fec

    //const val BASE_URL = "http://192.168.1.4:5001/"

    /// private const val BASE_URL =
        // "http://10.0.2.2:5001/"


    private val loggingInterceptor =
        HttpLoggingInterceptor().apply {

            level =
                // Do not log JWT response bodies, decrypted sessions, or login passwords.
                HttpLoggingInterceptor.Level.BASIC
        }


    // Automatically adds:
    //
    // Authorization: Bearer <JWT>
    private val authInterceptor = { chain: okhttp3.Interceptor.Chain ->

        val originalRequest =
            chain.request()

        val requestBuilder =
            originalRequest.newBuilder()

        // Only try reading token after RetrofitClient.init()
        if (!isBackendOrigin(originalRequest.url)) {
            requestBuilder.removeHeader("Authorization")
        } else if (::appContext.isInitialized) {

            val token =
                TokenManager.getToken(
                    appContext
                )

            if (!token.isNullOrBlank() && originalRequest.header("Authorization") == null) {

                requestBuilder.header(
                    "Authorization",
                    "Bearer $token"
                )
            }
        }

        chain.proceed(
            requestBuilder.build()
        )
    }


    val okHttpClient: OkHttpClient get() = client

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

            // Also enforce the origin boundary on every redirect/network exchange.
            .addNetworkInterceptor { chain ->
                val request = chain.request()
                chain.proceed(
                    if (isBackendOrigin(request.url)) request
                    else request.newBuilder().removeHeader("Authorization").build()
                )
            }

            .connectTimeout(
                30,
                TimeUnit.SECONDS
            )

            .readTimeout(
                120,
                TimeUnit.SECONDS
            )

            .writeTimeout(
                120,
                TimeUnit.SECONDS
            )

            .build()
    }


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
