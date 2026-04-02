package com.example.dropzone.services

import com.example.dropzone.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val updatedUrl = originalRequest.url().newBuilder()
                .addQueryParameter("key", BuildConfig.GEMINI_API_KEY)
                .build()
            val request = originalRequest.newBuilder()
                .url(updatedUrl)
                .build()
            chain.proceed(request)
        }
        .build()

    val instance: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }
}
