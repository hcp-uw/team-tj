package com.example.verifai.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class AnalysisRecord(
    val id: String,
    val imageUrl: String?,
    val imagePath: String?,
    val fileName: String,
    val label: String?,
    val confidence: Double?,
    val explanation: String?,
    val status: AnalysisStatus,
    val errorMessage: String?,
    val createdAt: Timestamp?,
) {
    val displayVerdict: String
        get() = when (label?.uppercase()) {
            "AI_GENERATED" -> "Likely AI-generated"
            "REAL" -> "Likely real"
            "UNCERTAIN" -> "Uncertain"
            else -> label ?: "Pending"
        }
}

enum class AnalysisStatus {
    PENDING,
    UPLOADING,
    ANALYZING,
    COMPLETE,
    FAILED,
    ;

    companion object {
        fun fromRaw(value: String?): AnalysisStatus =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: PENDING
    }
}

fun DocumentSnapshot.toAnalysisRecord(): AnalysisRecord? {
    val id = id
    return AnalysisRecord(
        id = id,
        imageUrl = getString("imageUrl"),
        imagePath = getString("imagePath"),
        fileName = getString("fileName") ?: "capture.png",
        label = getString("label"),
        confidence = getDouble("confidence"),
        explanation = getString("explanation"),
        status = AnalysisStatus.fromRaw(getString("status")),
        errorMessage = getString("errorMessage"),
        createdAt = getTimestamp("createdAt"),
    )
}
