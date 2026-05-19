package com.example.verifai.network

import com.example.verifai.BuildConfig
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Calls the FastAPI `/analyze` endpoint using the same stack as [NetworkModule].
 * Base URL comes from `api.base.url` in `android/local.properties` (see [BuildConfig.API_BASE_URL]).
 */
object VerifAiApiClient {

    suspend fun analyze(file: File): ApiAnalysisResult = withContext(Dispatchers.IO) {
        val bytes = file.readBytes()
        if (bytes.isEmpty()) {
            throw IOException("Empty image file")
        }
        val safeName = file.name.ifBlank { "image.jpg" }
        val body = bytes.toRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", safeName, body)

        val response = try {
            NetworkModule.api.analyzeImage(part)
        } catch (e: Exception) {
            throw IOException(
                "Cannot reach backend at ${BuildConfig.API_BASE_URL}. " +
                    "Start the server (python server.py in backend/) and set api.base.url in local.properties if needed.",
                e,
            )
        }

        when {
            response.status == "success" && response.result != null -> response.result!!
            else -> throw IOException(response.error ?: "Analysis failed")
        }
    }
}
