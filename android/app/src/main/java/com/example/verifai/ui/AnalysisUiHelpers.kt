package com.example.verifai.ui

import com.example.verifai.data.AnalysisRecord
import com.example.verifai.data.AnalysisStatus
import com.google.firebase.Timestamp
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

fun AnalysisRecord.isAiGenerated(): Boolean =
    label?.uppercase() == "AI_GENERATED"

fun AnalysisRecord.isAuthentic(): Boolean =
    label?.uppercase() == "REAL"

fun AnalysisRecord.confidencePercent(): Int? =
    confidence?.let { (it * 100).toInt().coerceIn(0, 100) }

fun AnalysisRecord.resultHeadline(): String = when (label?.uppercase()) {
    "AI_GENERATED" -> "AI-Generated Detected"
    "REAL" -> "Likely Authentic"
    "UNCERTAIN" -> "Uncertain"
    else -> when (status) {
        AnalysisStatus.ANALYZING, AnalysisStatus.UPLOADING -> "Analyzing…"
        AnalysisStatus.FAILED -> "Analysis Failed"
        else -> "Awaiting Result"
    }
}

fun AnalysisRecord.relativeTimestamp(): String {
    val created = createdAt ?: return ""
    val millis = created.seconds * 1000
    val diff = System.currentTimeMillis() - millis
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} min ago"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} hr ago"
        diff < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
        else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
    }
}
