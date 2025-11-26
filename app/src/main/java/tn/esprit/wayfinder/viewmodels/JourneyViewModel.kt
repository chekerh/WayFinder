package tn.esprit.wayfinder.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import tn.esprit.wayfinder.models.Journey
import tn.esprit.wayfinder.models.JourneyComment
import tn.esprit.wayfinder.models.JourneyLikeResponse
import tn.esprit.wayfinder.models.UpdateJourneyRequest
import tn.esprit.wayfinder.models.CanShareJourneyResponse
import tn.esprit.wayfinder.presentation.journey.JourneyRepository
import tn.esprit.wayfinder.utils.ImageCompressor
import java.io.File

sealed class JourneyUiState {
    object Idle : JourneyUiState()
    object Loading : JourneyUiState()
    data class Success(val journeys: List<Journey>) : JourneyUiState()
    data class Error(val message: String) : JourneyUiState()
}

sealed class JourneyDetailUiState {
    object Idle : JourneyDetailUiState()
    object Loading : JourneyDetailUiState()
    data class Success(val journey: Journey) : JourneyDetailUiState()
    data class Error(val message: String) : JourneyDetailUiState()
}

sealed class JourneyUploadUiState {
    object Idle : JourneyUploadUiState()
    data class Compressing(val progress: Int, val total: Int) : JourneyUploadUiState() // New: compression progress
    data class Uploading(val progress: Int, val total: Int) : JourneyUploadUiState() // Updated: upload progress
    data class Success(val journey: Journey) : JourneyUploadUiState()
    data class Error(val message: String) : JourneyUploadUiState()
}

class JourneyViewModel(private val journeyRepository: JourneyRepository) : ViewModel() {
    
    private val _uiState = MutableStateFlow<JourneyUiState>(JourneyUiState.Idle)
    val uiState: StateFlow<JourneyUiState> = _uiState.asStateFlow()
    
    private val _detailUiState = MutableStateFlow<JourneyDetailUiState>(JourneyDetailUiState.Idle)
    val detailUiState: StateFlow<JourneyDetailUiState> = _detailUiState.asStateFlow()
    
    private val _uploadUiState = MutableStateFlow<JourneyUploadUiState>(JourneyUploadUiState.Idle)
    val uploadUiState: StateFlow<JourneyUploadUiState> = _uploadUiState.asStateFlow()
    
    private val _comments = MutableStateFlow<List<JourneyComment>>(emptyList())
    val comments: StateFlow<List<JourneyComment>> = _comments.asStateFlow()
    
    private val _canShareState = MutableStateFlow<CanShareJourneyResponse?>(null)
    val canShareState: StateFlow<CanShareJourneyResponse?> = _canShareState.asStateFlow()
    
    fun checkCanShareJourney() {
        viewModelScope.launch {
            try {
                val response = journeyRepository.canShareJourney()
                _canShareState.value = response
                android.util.Log.d("JourneyViewModel", "Can share journey: ${response.canShare}, count: ${response.confirmedBookingsCount}")
            } catch (e: Exception) {
                // Log the error and show the actual error message
                val errorMessage = parseError(e)
                android.util.Log.e("JourneyViewModel", "Error checking can share journey: $errorMessage", e)
                // Translate common error messages to French
                val translatedMessage = when {
                    errorMessage.contains("401") || errorMessage.contains("Unauthorized") -> 
                        "Vous devez être connecté pour vérifier vos réservations"
                    errorMessage.contains("403") || errorMessage.contains("Forbidden") -> 
                        "Accès refusé. Vérifiez vos permissions."
                    errorMessage.contains("Network") || errorMessage.contains("connecter") || errorMessage.contains("UnknownHost") -> 
                        "Impossible de se connecter au serveur. Vérifiez votre connexion Internet."
                    else -> {
                        // If we get a response from the backend, use it; otherwise show generic message
                        if (errorMessage.contains("Unable to verify")) {
                            "Impossible de vérifier le statut de vos réservations. Réessayez plus tard."
                        } else {
                            "Erreur: $errorMessage"
                        }
                    }
                }
                _canShareState.value = CanShareJourneyResponse(false, 0, translatedMessage)
            }
        }
    }
    
    fun loadJourneys(limit: Int = 20, skip: Int = 0, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Don't reload if we already have data and not forcing refresh
            val currentState = _uiState.value
            if (!forceRefresh && currentState is JourneyUiState.Success && currentState.journeys.isNotEmpty()) {
                return@launch
            }
            
            try {
                if (currentState !is JourneyUiState.Success) {
                    _uiState.value = JourneyUiState.Loading
                }
                val journeys = journeyRepository.getJourneys(limit, skip)
                _uiState.value = JourneyUiState.Success(journeys)
            } catch (e: Exception) {
                val errorMessage = parseError(e)
                // Don't show JSON deserialization errors for image_urls - backend will be fixed soon
                if (errorMessage.contains("image_urls") || errorMessage.contains("Unexpected JSON token")) {
                    android.util.Log.w("JourneyViewModel", "JSON deserialization error (will be fixed after backend deploy): $errorMessage")
                    // Keep current state if it's success, otherwise show empty list
                    val currentState = _uiState.value
                    if (currentState is JourneyUiState.Success) {
                        // Keep the current list - don't show error
                        return@launch
                    } else {
                        // Show empty list instead of error
                        _uiState.value = JourneyUiState.Success(emptyList())
                    }
                } else {
                    _uiState.value = JourneyUiState.Error(errorMessage)
                }
            }
        }
    }
    
    fun loadMyJourneys(limit: Int = 20, skip: Int = 0) {
        viewModelScope.launch {
            try {
                _uiState.value = JourneyUiState.Loading
                val journeys = journeyRepository.getMyJourneys(limit, skip)
                _uiState.value = JourneyUiState.Success(journeys)
            } catch (e: Exception) {
                _uiState.value = JourneyUiState.Error(parseError(e))
            }
        }
    }
    
    fun loadJourneyDetail(journeyId: String) {
        viewModelScope.launch {
            try {
                _detailUiState.value = JourneyDetailUiState.Loading
                val journey = journeyRepository.getJourneyById(journeyId)
                _detailUiState.value = JourneyDetailUiState.Success(journey)
                loadComments(journeyId)
            } catch (e: Exception) {
                _detailUiState.value = JourneyDetailUiState.Error(parseError(e))
            }
        }
    }
    
    fun uploadJourney(
        imageUris: List<Uri>,
        bookingId: String? = null,
        destination: String? = null,
        description: String? = null,
        tags: List<String>? = null,
        isPublic: Boolean = true,
        context: android.content.Context
    ) {
        viewModelScope.launch {
            try {
                val totalImages = imageUris.size
                
                // Step 1: Compress images in parallel (with progress)
                _uploadUiState.value = JourneyUploadUiState.Compressing(0, totalImages)
                
                val compressedFiles = withContext(Dispatchers.IO) {
                    val coroutineScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
                    val compressionJobs = imageUris.mapIndexed { index, uri ->
                        coroutineScope.async {
                            val file = ImageCompressor.compressImage(
                                context,
                                uri,
                                "compressed_${System.currentTimeMillis()}_$index.jpg"
                            )
                            // Update compression progress on main thread
                            withContext(Dispatchers.Main) {
                                val currentProgress = when (val state = _uploadUiState.value) {
                                    is JourneyUploadUiState.Compressing -> state.progress + 1
                                    else -> index + 1
                                }
                                _uploadUiState.value = JourneyUploadUiState.Compressing(
                                    currentProgress,
                                    totalImages
                                )
                            }
                            file
                        }
                    }
                    compressionJobs.map { it.await() }
                }
                
                // Step 2: Convert compressed files to MultipartBody.Part
                val imageParts = compressedFiles.map { file ->
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("images", file.name, requestFile)
                }
                
                // Step 3: Upload to backend
                _uploadUiState.value = JourneyUploadUiState.Uploading(1, totalImages)
                
                val journey = withContext(Dispatchers.IO) {
                    journeyRepository.createJourney(
                        images = imageParts,
                        bookingId = bookingId,
                        destination = destination,
                        description = description,
                        tags = tags,
                        isPublic = isPublic
                    )
                }
                
                // Mark upload as complete
                withContext(Dispatchers.Main) {
                    _uploadUiState.value = JourneyUploadUiState.Uploading(totalImages, totalImages)
                }
                
                // Clean up compressed files and temporary files (async, don't block)
                viewModelScope.launch(Dispatchers.IO) {
                    compressedFiles.forEach { file ->
                        try {
                            if (file.exists()) {
                                file.delete()
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("JourneyViewModel", "Failed to delete compressed file: ${e.message}")
                        }
                    }
                    
                    // Also clean up any temporary files from cache (file:// URIs)
                    imageUris.forEach { uri ->
                        if (uri.scheme == "file") {
                            try {
                                val file = File(uri.path ?: "")
                                if (file.exists() && file.parentFile?.name == "cache") {
                                    file.delete()
                                    android.util.Log.d("JourneyViewModel", "Deleted temp file: ${file.absolutePath}")
                                }
                            } catch (e: Exception) {
                                android.util.Log.w("JourneyViewModel", "Failed to delete temp file: ${e.message}")
                            }
                        }
                    }
                }
                
                _uploadUiState.value = JourneyUploadUiState.Success(journey)
            } catch (e: Exception) {
                android.util.Log.e("JourneyViewModel", "Upload error: ${e.message}", e)
                _uploadUiState.value = JourneyUploadUiState.Error(parseError(e))
            }
        }
    }
    
    fun likeJourney(journeyId: String) {
        viewModelScope.launch {
            try {
                journeyRepository.likeJourney(journeyId)
                // Reload journey detail to get updated like status
                loadJourneyDetail(journeyId)
            } catch (e: Exception) {
                // Handle error silently or show snackbar
            }
        }
    }
    
    fun addComment(journeyId: String, content: String, parentCommentId: String? = null) {
        viewModelScope.launch {
            try {
                journeyRepository.addComment(journeyId, content, parentCommentId)
                loadComments(journeyId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun loadComments(journeyId: String, limit: Int = 50, skip: Int = 0) {
        viewModelScope.launch {
            try {
                val comments = journeyRepository.getComments(journeyId, limit, skip)
                _comments.value = comments
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun deleteJourney(journeyId: String) {
        viewModelScope.launch {
            try {
                journeyRepository.deleteJourney(journeyId)
                // Remove the deleted journey from the current list - don't reload to avoid JSON errors
                val currentState = _uiState.value
                if (currentState is JourneyUiState.Success) {
                    val updatedJourneys = currentState.journeys.filter { it.id != journeyId }
                    _uiState.value = JourneyUiState.Success(updatedJourneys)
                }
                // Don't reload - just remove from list to avoid JSON deserialization errors
            } catch (e: Exception) {
                android.util.Log.e("JourneyViewModel", "Error deleting journey: ${e.message}", e)
                val errorMessage = parseError(e)
                // If deletion failed with 404, journey might already be deleted - remove from list anyway
                if (errorMessage.contains("404") || errorMessage.contains("not found")) {
                    val currentState = _uiState.value
                    if (currentState is JourneyUiState.Success) {
                        val updatedJourneys = currentState.journeys.filter { it.id != journeyId }
                        _uiState.value = JourneyUiState.Success(updatedJourneys)
                    }
                } else {
                    // Only show error for real deletion failures (not JSON errors)
                    if (!errorMessage.contains("image_urls") && !errorMessage.contains("Unexpected JSON token")) {
                        _uiState.value = JourneyUiState.Error(errorMessage)
                    } else {
                        // JSON error during deletion - just remove from list silently
                        val currentState = _uiState.value
                        if (currentState is JourneyUiState.Success) {
                            val updatedJourneys = currentState.journeys.filter { it.id != journeyId }
                            _uiState.value = JourneyUiState.Success(updatedJourneys)
                        }
                    }
                }
            }
        }
    }
    
    fun regenerateVideo(journeyId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("JourneyViewModel", "Starting video regeneration for journey: $journeyId")
                journeyRepository.regenerateVideo(journeyId)
                android.util.Log.d("JourneyViewModel", "Video regeneration request sent successfully")
                // Reload journeys to show updated video status (processing)
                loadJourneys()
                // Note: Video generation is asynchronous, status will be updated via polling
                // The backend will process the video using the configured service (Cloudinary, Replicate, Kaggle, etc.)
            } catch (e: Exception) {
                android.util.Log.e("JourneyViewModel", "Error regenerating video: ${e.message}", e)
                // Error is logged, user will see status update when they refresh
                // The backend handles all video generation services transparently
            }
        }
    }
    
    private fun parseError(throwable: Throwable): String {
        return when (throwable) {
            is HttpException -> {
                val errorBody = throwable.response()?.errorBody()?.string()
                if (!errorBody.isNullOrBlank()) {
                    val parsedMessage = try {
                        val element = Json.parseToJsonElement(errorBody)
                        element.jsonObject["message"]?.jsonPrimitive?.content
                            ?: element.jsonObject["error"]?.jsonPrimitive?.content
                            ?: element.jsonObject.toString()
                    } catch (_: Exception) {
                        errorBody
                    }
                    parsedMessage ?: throwable.message()
                } else {
                    "HTTP ${throwable.code()}: ${throwable.message()}"
                }
            }
            is java.net.UnknownHostException -> "Impossible de se connecter au serveur. Vérifiez votre connexion Internet."
            is java.net.SocketTimeoutException -> "Le serveur met trop de temps à répondre. Veuillez réessayer."
            is java.util.concurrent.TimeoutException -> "L'upload a pris trop de temps. Veuillez réessayer."
            is java.net.ConnectException -> "Impossible de se connecter au serveur. Vérifiez votre connexion Internet."
            else -> {
                throwable.message ?: "Une erreur inattendue est survenue"
            }
        }
    }
}

