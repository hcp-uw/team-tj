package com.example.verifai.analysis

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CaptureAnalysisWorkflow {

    suspend fun processCapture(context: Context, localFile: File) = withContext(Dispatchers.IO) {
        ImageAnalysisWorkflow.processImage(context, localFile)
    }
}
