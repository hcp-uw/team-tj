package com.example.verifai.ui.analysis

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.verifai.network.ApiAnalysisResult
import com.example.verifai.network.VerifAiApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class AnalysisUiState(
    val selectedImageUri: Uri? = null,
    val isAnalyzing: Boolean = false,
    val result: ApiAnalysisResult? = null,
    val errorMessage: String? = null,
)

class AnalysisViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    fun onImageSelected(uri: Uri) {
        _uiState.value = AnalysisUiState(selectedImageUri = uri)
    }

    fun analyzeImage(imageFile: File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true, errorMessage = null)

            try {
                val result = withContext(Dispatchers.IO) {
                    VerifAiApiClient.analyze(imageFile)
                }
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    result = result,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    errorMessage = e.message ?: "Network error. Is the server running?",
                )
            }
        }
    }

    fun reset() {
        _uiState.value = AnalysisUiState()
    }
}
