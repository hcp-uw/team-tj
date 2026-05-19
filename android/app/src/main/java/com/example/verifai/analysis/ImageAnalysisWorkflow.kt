package com.example.verifai.analysis

import android.content.Context
import com.example.verifai.data.AnalysisRepository
import com.example.verifai.network.VerifAiApiClient
import com.example.verifai.notifications.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageAnalysisWorkflow {

    private val repository = AnalysisRepository()

    suspend fun processImage(context: Context, localFile: File): String? = withContext(Dispatchers.IO) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            NotificationHelper.showSignInRequiredNotification(context)
            return@withContext null
        }

        val analysisId = UUID.randomUUID().toString()

        try {
            repository.createPendingRecord(userId, analysisId, localFile.name)
            NotificationHelper.showAnalyzingNotification(context)

            repository.uploadImage(userId, analysisId, localFile)
            val result = VerifAiApiClient.analyze(localFile)
            repository.saveAnalysisResult(userId, analysisId, result)

            NotificationHelper.showAnalysisCompleteNotification(
                context = context,
                verdict = formatVerdict(result.label),
            )
            analysisId
        } catch (e: Exception) {
            runCatching {
                repository.markFailed(userId, analysisId, e.message)
            }
            NotificationHelper.showAnalysisFailedNotification(context, e.message)
            null
        }
    }

    fun formatVerdict(label: String): String = when (label.uppercase()) {
        "AI_GENERATED" -> "AI-Generated Detected"
        "REAL" -> "Likely Authentic"
        "UNCERTAIN" -> "Uncertain"
        else -> label
    }
}
