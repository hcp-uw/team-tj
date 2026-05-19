package com.example.verifai.data

import android.net.Uri
import com.example.verifai.network.ApiAnalysisResult
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AnalysisRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
) {

    fun currentUserId(): String? = auth.currentUser?.uid

    fun observeAnalyses(userId: String): Flow<List<AnalysisRecord>> = callbackFlow {
        val registration = analysesCollection(userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val records = snapshot?.documents
                    ?.mapNotNull { it.toAnalysisRecord() }
                    ?: emptyList()
                trySend(records)
            }
        awaitClose { registration.remove() }
    }

    suspend fun createPendingRecord(userId: String, analysisId: String, fileName: String) {
        analysesCollection(userId).document(analysisId).set(
            mapOf(
                "fileName" to fileName,
                "status" to AnalysisStatus.PENDING.name,
                "createdAt" to Timestamp.now(),
            ),
        ).await()
    }

    suspend fun uploadImage(userId: String, analysisId: String, localFile: File): UploadResult {
        analysesCollection(userId).document(analysisId).update(
            mapOf("status" to AnalysisStatus.UPLOADING.name),
        ).await()

        val storagePath = "users/$userId/images/$analysisId.png"
        val ref = storage.reference.child(storagePath)
        ref.putFile(Uri.fromFile(localFile)).await()
        val downloadUrl = ref.downloadUrl.await().toString()

        analysesCollection(userId).document(analysisId).update(
            mapOf(
                "imagePath" to storagePath,
                "imageUrl" to downloadUrl,
                "status" to AnalysisStatus.ANALYZING.name,
            ),
        ).await()

        return UploadResult(storagePath = storagePath, downloadUrl = downloadUrl)
    }

    suspend fun saveAnalysisResult(
        userId: String,
        analysisId: String,
        result: ApiAnalysisResult,
    ) {
        analysesCollection(userId).document(analysisId).update(
            mapOf(
                "label" to result.label,
                "confidence" to result.confidence,
                "explanation" to result.explanation,
                "status" to AnalysisStatus.COMPLETE.name,
                "errorMessage" to null,
            ),
        ).await()
    }

    suspend fun markFailed(userId: String, analysisId: String, message: String?) {
        analysesCollection(userId).document(analysisId).update(
            mapOf(
                "status" to AnalysisStatus.FAILED.name,
                "errorMessage" to (message ?: "Unknown error"),
            ),
        ).await()
    }

    private fun analysesCollection(userId: String) =
        firestore.collection("users").document(userId).collection("analyses")

    data class UploadResult(
        val storagePath: String,
        val downloadUrl: String,
    )
}
