package com.example.verifai.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.verifai.R
import com.example.verifai.data.AnalysisRecord
import com.example.verifai.data.AnalysisStatus
import com.example.verifai.ui.theme.VerifAITheme

@Composable
fun VerifAIApp(
    userName: String,
    onSignOut: () -> Unit,
    viewModel: VerifAIViewModel = viewModel(),
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.UPLOAD) }

    LaunchedEffect(Unit) {
        viewModel.navigateToResult.collect {
            currentDestination = AppDestinations.RESULT
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            painter = painterResource(destination.icon),
                            contentDescription = destination.label,
                        )
                    },
                    label = { Text(destination.label) },
                    selected = destination == currentDestination,
                    onClick = { currentDestination = destination },
                )
            }
        },
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.UPLOAD -> UploadScreen(
                    userName = userName,
                    viewModel = viewModel,
                    onSignOut = onSignOut,
                    modifier = Modifier.padding(innerPadding),
                )

                AppDestinations.RESULT -> ResultScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding),
                )

                AppDestinations.HISTORY -> HistoryScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
fun UploadScreen(
    userName: String,
    viewModel: VerifAIViewModel,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) viewModel.onImagePicked(uri)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Verify Image",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Hi, $userName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onSignOut) {
                Text("Sign out")
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clickable(enabled = !uploadState.isLoading) {
                    imagePicker.launch("image/*")
                },
            color = Color.Gray.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, Color.Gray.copy(alpha = 0.5f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_upload),
                        contentDescription = "Upload Icon",
                        modifier = Modifier.size(48.dp),
                        tint = Color.DarkGray,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select an image to scan", color = Color.DarkGray)
                }
            }
        }

        uploadState.error?.let { error ->
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }

        Text(
            text = "Preview",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 2.dp,
        ) {
            if (uploadState.previewUri != null) {
                AsyncImage(
                    model = uploadState.previewUri,
                    contentDescription = "Selected preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No image selected",
                        color = Color.Gray,
                    )
                }
            }
        }

        Button(
            onClick = { viewModel.analyzeSelectedImage() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = uploadState.previewUri != null && !uploadState.isLoading,
        ) {
            if (uploadState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Analyze for Deepfakes", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun ResultScreen(
    viewModel: VerifAIViewModel,
    modifier: Modifier = Modifier,
) {
    val record by viewModel.selectedAnalysis.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val analysis = record

    if (analysis == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No results yet.\nUpload an image or capture from Quick Settings.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(24.dp),
            )
        }
        return
    }

    val isAi = analysis.isAiGenerated()
    val confidence = analysis.confidencePercent()
    val isComplete = analysis.status == AnalysisStatus.COMPLETE

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "Analysis Result",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
        )

        if (analysis.imageUrl != null) {
            AsyncImage(
                model = analysis.imageUrl,
                contentDescription = analysis.fileName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    !isComplete -> Color(0xFFECEFF1)
                    isAi -> Color(0xFFFFEBEE)
                    else -> Color(0xFFE8F5E9)
                },
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isAi) R.drawable.ic_warning else R.drawable.ic_verified,
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = if (isAi) Color.Red else Color(0xFF2E7D32),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = analysis.resultHeadline(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        !isComplete -> Color.DarkGray
                        isAi -> Color(0xFFB71C1C)
                        else -> Color(0xFF1B5E20)
                    },
                )
            }
        }

        if (confidence != null && isComplete) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "AI Confidence",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray,
                )
                Text(
                    text = "$confidence%",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (confidence > 80 && isAi) Color.Red else Color.DarkGray,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color.LightGray),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Why this result?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    text = when {
                        analysis.status == AnalysisStatus.FAILED ->
                            analysis.errorMessage ?: "Analysis could not be completed."

                        !analysis.explanation.isNullOrBlank() -> analysis.explanation

                        analysis.status == AnalysisStatus.ANALYZING ||
                            analysis.status == AnalysisStatus.UPLOADING ->
                            "Your image is being uploaded and analyzed. Check back in a moment."

                        else -> "No explanation available yet."
                    },
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = Color.DarkGray,
                )
            }
        }
    }
}

@Composable
fun HistoryScreen(
    viewModel: VerifAIViewModel,
    modifier: Modifier = Modifier,
) {
    val analyses by viewModel.analyses.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "History",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        when {
            errorMessage != null -> {
                Text(text = errorMessage ?: "Unable to load history.")
            }

            analyses.isEmpty() -> {
                Text(
                    text = "No captures yet.\nUpload an image or use the Quick Settings tile.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(analyses, key = { it.id }) { item ->
                        HistoryCard(
                            item = item,
                            onClick = { viewModel.selectAnalysis(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    item: AnalysisRecord,
    onClick: () -> Unit,
) {
    val isAi = item.isAiGenerated()
    val confidence = item.confidencePercent()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .height(80.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.imageUrl != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = if (isAi) "AI Generated" else item.resultHeadline(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAi) Color(0xFFB71C1C) else Color(0xFF1B5E20),
                )
                Text(
                    text = when {
                        confidence != null -> "Confidence: $confidence%"
                        item.status == AnalysisStatus.FAILED -> "Failed"
                        else -> "In progress…"
                    },
                    fontSize = 14.sp,
                    color = Color.Gray,
                )
            }

            Text(
                text = item.relativeTimestamp(),
                fontSize = 12.sp,
                color = Color.LightGray,
                modifier = Modifier.align(Alignment.Top),
            )
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    UPLOAD("Upload", R.drawable.ic_upload),
    RESULT("Result", R.drawable.ic_result),
    HISTORY("History", R.drawable.ic_history),
}

@PreviewScreenSizes
@Composable
fun VerifAIAppPreview() {
    VerifAITheme {
        VerifAIApp(userName = "User", onSignOut = {})
    }
}
