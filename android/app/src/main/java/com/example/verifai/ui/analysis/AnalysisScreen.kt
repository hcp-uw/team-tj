package com.example.verifai.ui.analysis

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.verifai.network.ApiAnalysisResult
import java.io.File

@Composable
fun AnalysisScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalysisViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImageSelected(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "VerifAI",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(onClick = onSignOut) {
                Text("Sign out")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Image selection area
        if (state.selectedImageUri == null) {
            // No image selected — show picker prompt
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Select an image to analyze",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { imagePicker.launch("image/*") }) {
                        Text("Choose from Gallery")
                    }
                }
            }
        } else {
            // Image selected — show preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(state.selectedImageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!state.isAnalyzing && state.result == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Change Image")
                    }
                    Button(
                        onClick = {
                            val uri = state.selectedImageUri
                            if (uri != null) {
                                val file = copyUriToCacheFile(context, uri)
                                viewModel.analyzeImage(file)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Analyze")
                    }
                }
            }
        }

        // Loading state
        if (state.isAnalyzing) {
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Analyzing image...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Error
        state.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Result
        state.result?.let { result ->
            Spacer(modifier = Modifier.height(24.dp))
            ResultCard(result = result)

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.reset() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Analyze Another Image")
            }
        }
    }
}

@Composable
private fun ResultCard(result: ApiAnalysisResult) {
    val isFake = result.label == "AI_GENERATED"
    val verdictColor = if (isFake) Color(0xFFE53935) else Color(0xFF43A047)
    val verdictText = when (result.label) {
        "AI_GENERATED" -> "AI-Generated"
        "REAL" -> "Real Photo"
        else -> "Uncertain"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Analysis Result",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Verdict
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Verdict:", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = verdictText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = verdictColor
                )
            }

            // Confidence
            result.confidence?.let { conf ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Confidence:", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${(conf * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Explanation
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Explanation",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = result.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun copyUriToCacheFile(context: android.content.Context, uri: Uri): File {
    val extension = context.contentResolver.getType(uri)?.substringAfter('/') ?: "jpg"
    val file = File(context.cacheDir, "analyze_${System.currentTimeMillis()}.$extension")
    context.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    } ?: error("Could not read selected image")
    return file
}
