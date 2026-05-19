package com.example.verifai.network

import com.google.gson.annotations.SerializedName

data class AnalysisResult(
    @SerializedName("label") val label: String,
    @SerializedName("confidence") val confidence: Double?,
    @SerializedName("explanation") val explanation: String
)

data class AnalyzeResponse(
    @SerializedName("status") val status: String,
    @SerializedName("result") val result: AnalysisResult?,
    @SerializedName("error") val error: String?
)

data class HealthResponse(
    @SerializedName("status") val status: String,
    @SerializedName("model_loaded") val modelLoaded: Boolean,
    @SerializedName("device") val device: String,
    @SerializedName("vllm_enabled") val vllmEnabled: Boolean
)

/** Same shape as [AnalysisResult]; used by Firestore/repository layer. */
typealias ApiAnalysisResult = AnalysisResult
