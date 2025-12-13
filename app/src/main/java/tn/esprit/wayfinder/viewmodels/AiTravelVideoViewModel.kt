package tn.esprit.wayfinder.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import tn.esprit.wayfinder.models.AiVideoGenerateRequest
import tn.esprit.wayfinder.models.AiVideoGenerateWithMediaRequest
import tn.esprit.wayfinder.models.MusicTrack
import tn.esprit.wayfinder.models.TravelPlanSuggestion
import tn.esprit.wayfinder.network.ApiService

/**
 * UI State for AI Travel Video generation
 */
sealed class AiVideoUiState {
    object Idle : AiVideoUiState()
    object Loading : AiVideoUiState()
    object ServiceUnavailable : AiVideoUiState()
    data class Generating(
        val predictionId: String,
        val progress: Int = 0,
        val enhancedPrompt: String = ""
    ) : AiVideoUiState()
    data class Completed(val videoUrl: String, val prompt: String) : AiVideoUiState()
    data class Error(val message: String) : AiVideoUiState()
}

/**
 * ViewModel for AI Travel Video generation
 * Handles text-to-video generation with travel-specific prompts
 */
class AiTravelVideoViewModel(
    private val apiService: ApiService
) : ViewModel() {

    companion object {
        private const val TAG = "AiTravelVideoVM"
        private const val POLL_INTERVAL_MS = 3000L // 3 seconds
        private const val MAX_POLL_ATTEMPTS = 120 // 6 minutes max
    }

    private val _uiState = MutableStateFlow<AiVideoUiState>(AiVideoUiState.Idle)
    val uiState: StateFlow<AiVideoUiState> = _uiState.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val _isServiceAvailable = MutableStateFlow(false)
    val isServiceAvailable: StateFlow<Boolean> = _isServiceAvailable.asStateFlow()

    private val _generatedVideos = MutableStateFlow<List<GeneratedVideoItem>>(emptyList())
    val generatedVideos: StateFlow<List<GeneratedVideoItem>> = _generatedVideos.asStateFlow()

    private val _musicTracks = MutableStateFlow<List<MusicTrack>>(emptyList())
    val musicTracks: StateFlow<List<MusicTrack>> = _musicTracks.asStateFlow()

    private val _travelPlans = MutableStateFlow<List<TravelPlanSuggestion>>(emptyList())
    val travelPlans: StateFlow<List<TravelPlanSuggestion>> = _travelPlans.asStateFlow()

    private val _selectedImages = MutableStateFlow<List<String>>(emptyList())
    val selectedImages: StateFlow<List<String>> = _selectedImages.asStateFlow()

    private val _selectedMusicTrack = MutableStateFlow<MusicTrack?>(null)
    val selectedMusicTrack: StateFlow<MusicTrack?> = _selectedMusicTrack.asStateFlow()

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    init {
        checkServiceStatus()
        loadMusicTracks()
        loadTravelPlans()
    }

    /**
     * Check if AI video generation service is available
     */
    fun checkServiceStatus() {
        viewModelScope.launch {
            try {
                val response = apiService.getAiVideoStatus()
                _isServiceAvailable.value = response.available
                _suggestions.value = response.suggestions
                
                if (!response.available) {
                    _uiState.value = AiVideoUiState.ServiceUnavailable
                }
                
                Log.d(TAG, "AI Video service status: ${response.available}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check service status", e)
                _isServiceAvailable.value = false
                _uiState.value = AiVideoUiState.ServiceUnavailable
            }
        }
    }

    /**
     * Generate a travel video from a text prompt
     */
    fun generateVideo(prompt: String) {
        if (prompt.isBlank()) {
            _uiState.value = AiVideoUiState.Error("Please enter a prompt")
            return
        }

        if (prompt.length < 5) {
            _uiState.value = AiVideoUiState.Error("Prompt must be at least 5 characters")
            return
        }

        viewModelScope.launch {
            _uiState.value = AiVideoUiState.Loading

            try {
                val request = AiVideoGenerateRequest(prompt = prompt.trim())
                val response = apiService.generateAiTravelVideo(request)

                if (response.success && response.data != null) {
                    val predictionId = response.data.predictionId
                    _uiState.value = AiVideoUiState.Generating(
                        predictionId = predictionId,
                        progress = 0,
                        enhancedPrompt = response.data.enhancedPrompt
                    )
                    
                    Log.d(TAG, "Video generation started: $predictionId")
                    
                    // Start polling for status
                    pollForCompletion(predictionId, prompt)
                } else {
                    _uiState.value = AiVideoUiState.Error(
                        response.message ?: "Failed to start video generation"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate video", e)
                _uiState.value = AiVideoUiState.Error(
                    e.message ?: "Failed to generate video"
                )
            }
        }
    }

    /**
     * Poll for video generation completion
     */
    private suspend fun pollForCompletion(predictionId: String, originalPrompt: String) {
        var attempts = 0

        while (attempts < MAX_POLL_ATTEMPTS) {
            try {
                delay(POLL_INTERVAL_MS)
                
                val statusResponse = apiService.checkAiVideoStatus(predictionId)
                val data = statusResponse.data

                if (data != null) {
                    when {
                        data.isComplete && !data.videoUrl.isNullOrBlank() -> {
                            _uiState.value = AiVideoUiState.Completed(
                                videoUrl = data.videoUrl,
                                prompt = originalPrompt
                            )
                            
                            // Add to generated videos list
                            addGeneratedVideo(
                                GeneratedVideoItem(
                                    id = predictionId,
                                    prompt = originalPrompt,
                                    videoUrl = data.videoUrl,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                            
                            Log.d(TAG, "Video generation completed: ${data.videoUrl}")
                            return
                        }
                        data.isFailed -> {
                            _uiState.value = AiVideoUiState.Error(
                                data.error ?: "Video generation failed"
                            )
                            Log.e(TAG, "Video generation failed: ${data.error}")
                            return
                        }
                        else -> {
                            // Still processing, update progress
                            val currentState = _uiState.value
                            if (currentState is AiVideoUiState.Generating) {
                                _uiState.value = currentState.copy(
                                    progress = data.progress ?: ((attempts * 100) / MAX_POLL_ATTEMPTS)
                                )
                            }
                        }
                    }
                }

                attempts++
            } catch (e: Exception) {
                Log.e(TAG, "Error polling status", e)
                attempts++
            }
        }

        // Timeout
        _uiState.value = AiVideoUiState.Error("Video generation timed out. Please try again.")
    }

    /**
     * Cancel current video generation
     */
    fun cancelGeneration() {
        val currentState = _uiState.value
        if (currentState is AiVideoUiState.Generating) {
            viewModelScope.launch {
                try {
                    apiService.cancelAiVideo(currentState.predictionId)
                    _uiState.value = AiVideoUiState.Idle
                    Log.d(TAG, "Video generation cancelled")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to cancel generation", e)
                }
            }
        }
    }

    /**
     * Reset state to idle
     */
    fun resetState() {
        _uiState.value = AiVideoUiState.Idle
    }

    /**
     * Add a generated video to the history
     */
    private fun addGeneratedVideo(video: GeneratedVideoItem) {
        val currentList = _generatedVideos.value.toMutableList()
        currentList.add(0, video) // Add to the beginning
        _generatedVideos.value = currentList.take(10) // Keep last 10
    }

    /**
     * Use a suggestion as the prompt
     */
    fun useSuggestion(suggestion: String): String {
        return suggestion
    }

    /**
     * Load available music tracks
     */
    private fun loadMusicTracks() {
        viewModelScope.launch {
            try {
                val response = apiService.getMusicTracks()
                if (response.success) {
                    _musicTracks.value = response.tracks
                    Log.d(TAG, "Loaded ${response.tracks.size} music tracks")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load music tracks", e)
            }
        }
    }

    /**
     * Load AI travel plan suggestions
     */
    private fun loadTravelPlans() {
        viewModelScope.launch {
            try {
                val response = apiService.getTravelPlans()
                if (response.success) {
                    _travelPlans.value = response.plans
                    Log.d(TAG, "Loaded ${response.plans.size} travel plans")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load travel plans", e)
            }
        }
    }

    /**
     * Select a music track
     */
    fun selectMusicTrack(track: MusicTrack?) {
        _selectedMusicTrack.value = track
    }

    /**
     * Add an image URL to the selected images
     */
    fun addImage(imageUrl: String) {
        val current = _selectedImages.value.toMutableList()
        if (current.size < 20 && !current.contains(imageUrl)) {
            current.add(imageUrl)
            _selectedImages.value = current
        }
    }

    /**
     * Remove an image from the selected images
     */
    fun removeImage(imageUrl: String) {
        _selectedImages.value = _selectedImages.value.filter { it != imageUrl }
    }

    /**
     * Clear all selected images
     */
    fun clearImages() {
        _selectedImages.value = emptyList()
    }

    /**
     * Upload an image from a URI (phone gallery)
     */
    fun uploadImageFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isUploadingImage.value = true
            _uploadError.value = null

            try {
                // Get file bytes from URI
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: throw Exception("Could not read image")
                inputStream.close()

                // Get file name
                val fileName = uri.lastPathSegment ?: "image_${System.currentTimeMillis()}.jpg"

                // Create multipart body
                val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("image", fileName, requestBody)

                // Upload to server
                val response = apiService.uploadVideoImage(part)

                if (response.success && response.data != null) {
                    addImage(response.data.url)
                    Log.d(TAG, "Image uploaded successfully: ${response.data.url}")
                } else {
                    _uploadError.value = response.message ?: "Failed to upload image"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload image", e)
                _uploadError.value = e.message ?: "Failed to upload image"
            } finally {
                _isUploadingImage.value = false
            }
        }
    }

    /**
     * Upload multiple images from URIs (phone gallery)
     */
    fun uploadImagesFromUris(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            _isUploadingImage.value = true
            _uploadError.value = null

            for (uri in uris) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes() ?: continue
                    inputStream.close()

                    val fileName = uri.lastPathSegment ?: "image_${System.currentTimeMillis()}.jpg"
                    val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("image", fileName, requestBody)

                    val response = apiService.uploadVideoImage(part)

                    if (response.success && response.data != null) {
                        addImage(response.data.url)
                        Log.d(TAG, "Image uploaded: ${response.data.url}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to upload image from $uri", e)
                }
            }

            _isUploadingImage.value = false
        }
    }

    /**
     * Clear upload error
     */
    fun clearUploadError() {
        _uploadError.value = null
    }

    /**
     * Use a travel plan's video prompt
     */
    fun useTravelPlan(plan: TravelPlanSuggestion): String {
        return plan.videoPrompt
    }

    /**
     * Generate a travel video with images and music
     */
    fun generateVideoWithMedia(prompt: String) {
        if (prompt.isBlank()) {
            _uiState.value = AiVideoUiState.Error("Please enter a prompt")
            return
        }

        if (prompt.length < 5) {
            _uiState.value = AiVideoUiState.Error("Prompt must be at least 5 characters")
            return
        }

        viewModelScope.launch {
            _uiState.value = AiVideoUiState.Loading

            try {
                val request = AiVideoGenerateWithMediaRequest(
                    prompt = prompt.trim(),
                    images = _selectedImages.value,
                    musicTrackId = _selectedMusicTrack.value?.id
                )
                val response = apiService.generateAiTravelVideoWithMedia(request)

                if (response.success && response.data != null) {
                    val predictionId = response.data.predictionId
                    _uiState.value = AiVideoUiState.Generating(
                        predictionId = predictionId,
                        progress = 0,
                        enhancedPrompt = response.data.enhancedPrompt
                    )

                    Log.d(TAG, "Video with media generation started: $predictionId")

                    // Start polling for status
                    pollForCompletion(predictionId, prompt)
                } else {
                    _uiState.value = AiVideoUiState.Error(
                        response.message ?: "Failed to start video generation"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate video with media", e)
                _uiState.value = AiVideoUiState.Error(
                    e.message ?: "Failed to generate video"
                )
            }
        }
    }
}

/**
 * Represents a generated video item
 */
data class GeneratedVideoItem(
    val id: String,
    val prompt: String,
    val videoUrl: String,
    val createdAt: Long
)

