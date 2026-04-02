package com.example.dropzone.services

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object CloudinaryRetrofitClient {
    private const val BASE_URL = "https://api.cloudinary.com/v1_1/"

    val instance: CloudinaryApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudinaryApiService::class.java)
    }
}
