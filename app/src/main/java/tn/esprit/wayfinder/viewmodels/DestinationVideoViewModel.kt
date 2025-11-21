package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.presentation.destinationvideo.DestinationVideoRepository

sealed class DestinationVideoUiState {
    object Idle : DestinationVideoUiState()
    object Loading : DestinationVideoUiState()
    data class Success(val destinations: List<DestinationWithVideoStatus>) : DestinationVideoUiState()
    data class Error(val message: String) : DestinationVideoUiState()
}

sealed class VideoGenerationState {
    object Idle : VideoGenerationState()
    object Generating : VideoGenerationState()
    data class Success(val message: String) : VideoGenerationState()
    data class Error(val message: String) : VideoGenerationState()
}

class DestinationVideoViewModel(
    private val destinationVideoRepository: DestinationVideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DestinationVideoUiState>(DestinationVideoUiState.Idle)
    val uiState: StateFlow<DestinationVideoUiState> = _uiState.asStateFlow()

    private val _generationState = MutableStateFlow<VideoGenerationState>(VideoGenerationState.Idle)
    val generationState: StateFlow<VideoGenerationState> = _generationState.asStateFlow()

    fun loadUserDestinations(userId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = DestinationVideoUiState.Loading
                val response = destinationVideoRepository.getUserDestinations(userId)
                _uiState.value = DestinationVideoUiState.Success(response.destinations)
            } catch (e: Exception) {
                _uiState.value = DestinationVideoUiState.Error(parseError(e))
            }
        }
    }

    fun generateVideo(userId: String, destination: String) {
        viewModelScope.launch {
            try {
                _generationState.value = VideoGenerationState.Generating
                val response = destinationVideoRepository.generateDestinationVideo(userId, destination)
                _generationState.value = VideoGenerationState.Success(response.message)
                // Reload destinations to update status
                loadUserDestinations(userId)
            } catch (e: Exception) {
                _generationState.value = VideoGenerationState.Error(parseError(e))
            }
        }
    }

    fun checkVideoStatus(userId: String, destination: String) {
        viewModelScope.launch {
            try {
                val status = destinationVideoRepository.getDestinationVideoStatus(userId, destination)
                // Update the destination status in the list if needed
                val currentState = _uiState.value
                if (currentState is DestinationVideoUiState.Success) {
                    val updatedDestinations = currentState.destinations.map { dest ->
                        if (dest.destination == destination) {
                            dest.copy(
                                videoStatus = status.status,
                                videoUrl = status.videoUrl,
                                imageCount = status.imageCount
                            )
                        } else {
                            dest
                        }
                    }
                    _uiState.value = DestinationVideoUiState.Success(updatedDestinations)
                }
            } catch (e: Exception) {
                android.util.Log.e("DestinationVideoViewModel", "Error checking video status: ${e.message}")
            }
        }
    }

    private fun parseError(throwable: Throwable): String {
        return when (throwable) {
            is retrofit2.HttpException -> {
                val errorBody = throwable.response()?.errorBody()?.string()
                errorBody ?: "HTTP ${throwable.code()}: ${throwable.message()}"
            }
            is java.net.UnknownHostException -> "Impossible de se connecter au serveur. Vérifiez votre connexion Internet."
            is java.net.SocketTimeoutException -> "Le serveur met trop de temps à répondre. Veuillez réessayer."
            is java.net.ConnectException -> "Impossible de se connecter au serveur. Vérifiez votre connexion Internet."
            else -> throwable.message ?: "Une erreur inattendue est survenue"
        }
    }
}

