package com.example.dropzone.services

import com.example.dropzone.models.GeminiRequest
import com.example.dropzone.models.GeminiResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface GeminiApiService {

    @Headers("Content-Type: application/json")
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    fun generateText(
        @Body request: GeminiRequest
    ): Call<GeminiResponse>
}
