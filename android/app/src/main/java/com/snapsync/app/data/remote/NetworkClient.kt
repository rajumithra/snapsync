package com.snapsync.app.data.remote

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {
    private const val PREF_NAME = "snapsync_prefs"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_SERVER_URL = "server_url"

    // Default development server URL (10.0.2.2 for Android emulator, or custom Wi-Fi host IP)
    var defaultBaseUrl = "http://10.0.2.2:8000/api/v1/"

    private var retrofit: Retrofit? = null
    private var api: SnapSyncApi? = null

    fun getApi(context: Context): SnapSyncApi {
        if (api == null) {
            val client = buildOkHttpClient(context)
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val baseUrl = prefs.getString(KEY_SERVER_URL, defaultBaseUrl) ?: defaultBaseUrl

            retrofit = Retrofit.Builder()
                .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            api = retrofit!!.create(SnapSyncApi::class.java)
        }
        return api!!
    }

    private fun buildOkHttpClient(context: Context): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val token = prefs.getString(KEY_TOKEN, null)

            val requestBuilder = chain.request().newBuilder()
            if (!token.isNullOrEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun saveToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TOKEN, token).apply()
        api = null // Reset so new token is attached
    }

    fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TOKEN, null)
    }

    fun clearToken(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_TOKEN).apply()
        api = null
    }

    fun setServerUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SERVER_URL, url).apply()
        api = null
    }
}

