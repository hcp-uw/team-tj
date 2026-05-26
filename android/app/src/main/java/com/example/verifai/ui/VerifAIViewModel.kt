package com.example.verifai.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.verifai.analysis.ImageAnalysisWorkflow
import com.example.verifai.data.AnalysisRecord
import com.example.verifai.data.AnalysisRepository
import com.example.verifai.data.AnalysisStatus
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UploadUiState(
    val previewUri: Uri? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class VerifAIViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AnalysisRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _analyses = MutableStateFlow<List<AnalysisRecord>>(emptyList())
    val analyses: StateFlow<List<AnalysisRecord>> = _analyses.asStateFlow()

    private val _selectedAnalysisId = MutableStateFlow<String?>(null)
    val selectedAnalysisId: StateFlow<String?> = _selectedAnalysisId.asStateFlow()

    /** Latest upload/capture result when Firestore has not synced yet (or rules block writes). */
    private val _latestProcessRecord = MutableStateFlow<AnalysisRecord?>(null)

    val selectedAnalysis: StateFlow<AnalysisRecord?> = combine(
        _analyses,
        _selectedAnalysisId,
        _latestProcessRecord,
    ) { records, id, latest ->
        when {
            id != null -> records.find { it.id == id } ?: latest?.takeIf { it.id == id }
            else -> records.firstOrNull { it.status == AnalysisStatus.COMPLETE } ?: latest
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uploadState = MutableStateFlow(UploadUiState())
    val uploadState: StateFlow<UploadUiState> = _uploadState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _navigateToResult = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToResult: SharedFlow<Unit> = _navigateToResult.asSharedFlow()

    private var observeJob: Job? = null

    init {
        auth.addAuthStateListener { subscribeToAnalyses() }
        subscribeToAnalyses()
    }

    fun onImagePicked(uri: Uri) {
        _latestProcessRecord.value = null
        _uploadState.update { it.copy(previewUri = uri, error = null) }
    }

    fun analyzeSelectedImage() {
        val uri = _uploadState.value.previewUri ?: return
        viewModelScope.launch {
            _uploadState.update { it.copy(isLoading = true, error = null) }
            try {
                val file = copyUriToCache(uri)
                val record = ImageAnalysisWorkflow.processImage(getApplication(), file)
                if (record != null) {
                    _latestProcessRecord.value = record
                    _selectedAnalysisId.value = record.id
                    _navigateToResult.emit(Unit)
                } else {
                    _uploadState.update {
                        it.copy(error = "Could not start analysis. Sign in and try again.")
                    }
                }
            } catch (e: Exception) {
                _uploadState.update {
                    it.copy(
                        error = e.message
                            ?: "Analysis failed. Start the backend (python server.py) and set api.base.url in android/local.properties if needed.",
                    )
                }
            } finally {
                _uploadState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectAnalysis(record: AnalysisRecord) {
        _latestProcessRecord.value = null
        _selectedAnalysisId.value = record.id
        viewModelScope.launch { _navigateToResult.emit(Unit) }
    }

    fun subscribeToAnalyses() {
        observeJob?.cancel()
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _analyses.value = emptyList()
            _errorMessage.value = null
            return
        }

        observeJob = viewModelScope.launch {
            repository.observeAnalyses(userId)
                .catch { error ->
                    _errorMessage.value = error.message
                    emit(emptyList())
                }
                .collect { records ->
                    _errorMessage.value = null
                    _analyses.value = records
                }
        }
    }

    private fun copyUriToCache(uri: Uri): File {
        val context = getApplication<Application>()
        val extension = context.contentResolver.getType(uri)?.substringAfter("/") ?: "jpg"
        val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not read selected image")
        return file
    }
}
