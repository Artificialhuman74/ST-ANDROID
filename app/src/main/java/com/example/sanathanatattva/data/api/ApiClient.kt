package com.example.sanathanatattva.data.api

import com.example.sanathanatattva.BuildConfig
import com.example.sanathanatattva.data.auth.TokenStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

object ApiClient {

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun create(tokenStore: TokenStore): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }

        val builder = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = runBlocking { tokenStore.current() }
                val request = if (token != null) {
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else chain.request()
                chain.proceed(request)
            }
            .addInterceptor(logging)

        trustDevCertIfConfigured(builder)

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL + "/")
            .client(builder.build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    /** Client for Coil image loads; shares the dev-cert trust config with the API client. */
    fun imageHttpClient(): OkHttpClient =
        OkHttpClient.Builder().also(::trustDevCertIfConfigured).build()

    // Debug builds talk to the dev backend on 10.0.2.2 which uses a
    // self-signed certificate. Never enabled in release builds.
    private fun trustDevCertIfConfigured(builder: OkHttpClient.Builder) {
        if (!BuildConfig.TRUST_ALL_CERTS) return
        val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustAll), SecureRandom())
        }
        builder.sslSocketFactory(sslContext.socketFactory, trustAll)
        builder.hostnameVerifier { _, _ -> true }
    }
}
