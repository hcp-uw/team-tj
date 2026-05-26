package com.example.verifai.network

import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface VerifaiApi {
    @Multipart
    @POST("/analyze")
    suspend fun analyzeImage(
        @Part file: MultipartBody.Part
    ): AnalyzeResponse

    @GET("/health")
    suspend fun healthCheck(): HealthResponse
}
