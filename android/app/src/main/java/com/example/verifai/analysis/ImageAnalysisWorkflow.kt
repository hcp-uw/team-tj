package com.example.verifai.analysis

import android.content.Context
import android.net.Uri
import com.example.verifai.data.AnalysisRecord
import com.example.verifai.data.AnalysisRepository
import com.example.verifai.data.AnalysisStatus
import com.example.verifai.network.VerifAiApiClient
import com.example.verifai.notifications.NotificationHelper
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageAnalysisWorkflow {

    private val repository = AnalysisRepository()

    /**
     * Runs FakeVLM inference first, then tries to persist to Firestore + Storage.
     * If Firebase returns PERMISSION_DENIED (rules not deployed, etc.), the API result is still returned
     * so the in-app Result screen can show the verdict using a local image URL.
     */
    suspend fun processImage(context: Context, localFile: File): AnalysisRecord? = withContext(Dispatchers.IO) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            NotificationHelper.showSignInRequiredNotification(context)
            return@withContext null
        }

        val analysisId = UUID.randomUUID().toString()

        try {
            NotificationHelper.showAnalyzingNotification(context)

            val result = VerifAiApiClient.analyze(localFile)

            var imageUrl: String?
            var cloudSaved = false
            try {
                repository.createPendingRecord(userId, analysisId, localFile.name)
                val upload = repository.uploadImage(userId, analysisId, localFile)
                imageUrl = upload.downloadUrl
                repository.saveAnalysisResult(userId, analysisId, result)
                cloudSaved = true
            } catch (e: Exception) {
                runCatching {
                    repository.markFailed(userId, analysisId, e.message)
                }
                imageUrl = Uri.fromFile(localFile).toString()
            }

            NotificationHelper.showAnalysisCompleteNotification(
                context = context,
                verdict = formatVerdict(result.label),
            )

            AnalysisRecord(
                id = analysisId,
                imageUrl = imageUrl,
                imagePath = if (cloudSaved) "users/$userId/images/$analysisId.png" else null,
                fileName = localFile.name,
                label = result.label,
                confidence = result.confidence,
                explanation = result.explanation,
                status = AnalysisStatus.COMPLETE,
                errorMessage = null,
                createdAt = Timestamp.now(),
            )
        } catch (e: Exception) {
            runCatching {
                repository.markFailed(userId, analysisId, e.message)
            }
            NotificationHelper.showAnalysisFailedNotification(context, e.message)
            throw e
        }
    }

    fun formatVerdict(label: String): String = when (label.uppercase()) {
        "AI_GENERATED" -> "AI-Generated Detected"
        "REAL" -> "Likely Authentic"
        "UNCERTAIN" -> "Uncertain"
        else -> label
    }
}
