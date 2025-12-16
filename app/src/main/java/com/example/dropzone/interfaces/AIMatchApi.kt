package com.example.dropzone.interfaces

import com.example.dropzone.models.AIMatchRequest
import com.example.dropzone.models.AIMatchResponse
import retrofit2.http.Body
import retrofit2.http.POST


interface AIMatchApi {
    @POST("match")
    suspend fun getMatches(
        @Body request: AIMatchRequest
    ): List<AIMatchResponse>
}