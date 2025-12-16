package com.example.dropzone.instance

import com.example.dropzone.interfaces.AIMatchApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AIApiClient {
    private const val BASE_URL = "http://192.168.63.242:8000/"


    val api: AIMatchApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AIMatchApi::class.java)
    }
}