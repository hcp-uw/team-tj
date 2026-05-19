package com.example.verifai.network

import com.example.verifai.BuildConfig
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject

data class ApiAnalysisResult(
    val label: String,
    val confidence: Double?,
    val explanation: String,
)

object VerifAiApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyze(imageFile: File): ApiAnalysisResult = withContext(Dispatchers.IO) {
        val requestBody = imageFile.asRequestBody("image/png".toMediaType())
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", imageFile.name, requestBody)
            .build()

        val request = Request.Builder()
            .url("${BuildConfig.API_BASE_URL.trimEnd('/')}/analyze")
            .post(multipart)
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = runCatching {
                    JSONObject(body).optString("detail", body)
                }.getOrDefault(body)
                throw IllegalStateException("Analysis failed (${response.code}): $detail")
            }

            val json = JSONObject(body)
            if (json.optString("status") != "success") {
                throw IllegalStateException(json.optString("error", "Analysis failed"))
            }

            val result = json.getJSONObject("result")
            ApiAnalysisResult(
                label = result.getString("label"),
                confidence = if (result.has("confidence") && !result.isNull("confidence")) {
                    result.getDouble("confidence")
                } else {
                    null
                },
                explanation = result.getString("explanation"),
            )
        }
    }
}
