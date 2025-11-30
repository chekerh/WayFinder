package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import tn.esprit.wayfinder.models.Outfit
import tn.esprit.wayfinder.network.ApiService
import java.io.IOException

sealed class OutfitWeatherUiState {
    object Idle : OutfitWeatherUiState()
    object Loading : OutfitWeatherUiState()
    data class Success(val outfit: Outfit) : OutfitWeatherUiState()
    data class Error(val message: String) : OutfitWeatherUiState()
}

class OutfitWeatherViewModel(private val apiService: ApiService) : ViewModel() {

    private val _uiState = MutableStateFlow<OutfitWeatherUiState>(OutfitWeatherUiState.Idle)
    val uiState: StateFlow<OutfitWeatherUiState> = _uiState.asStateFlow()

    private val _outfitHistory = MutableStateFlow<List<Outfit>>(emptyList())
    val outfitHistory: StateFlow<List<Outfit>> = _outfitHistory.asStateFlow()

    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory.asStateFlow()

    val isLoading: Boolean
        get() = _uiState.value is OutfitWeatherUiState.Loading

    val outfit: Outfit?
        get() = (_uiState.value as? OutfitWeatherUiState.Success)?.outfit

    val errorMessage: String?
        get() = (_uiState.value as? OutfitWeatherUiState.Error)?.message

    fun uploadOutfit(imagePart: MultipartBody.Part, bookingId: String) {
        viewModelScope.launch {
            _uiState.value = OutfitWeatherUiState.Loading
            try {
                android.util.Log.d("OutfitWeatherViewModel", "Starting upload for booking: $bookingId")
                val bookingIdBody = bookingId.toRequestBody("text/plain".toMediaType())
                val response = apiService.uploadOutfitImage(imagePart, bookingIdBody)
                android.util.Log.d("OutfitWeatherViewModel", "Upload successful, analysis: ${response.analysis}")
                _uiState.value = OutfitWeatherUiState.Success(response.analysis)
            } catch (e: HttpException) {
                val errorMessage = try {
                    val errorBody = e.response()?.errorBody()?.string()
                    android.util.Log.e("OutfitWeatherViewModel", "HTTP error: ${e.code()}, body: $errorBody")
                    errorBody ?: "Erreur lors de l'upload (${e.code()})"
                } catch (ex: Exception) {
                    android.util.Log.e("OutfitWeatherViewModel", "Error reading error body: ${ex.message}")
                    "Erreur lors de l'upload de l'image"
                }
                _uiState.value = OutfitWeatherUiState.Error(errorMessage)
            } catch (e: IOException) {
                android.util.Log.e("OutfitWeatherViewModel", "IO error: ${e.message}")
                _uiState.value = OutfitWeatherUiState.Error("Erreur de connexion. Vérifiez votre Internet.")
            } catch (e: Exception) {
                android.util.Log.e("OutfitWeatherViewModel", "Unexpected error: ${e.message}", e)
                _uiState.value = OutfitWeatherUiState.Error("Erreur inattendue: ${e.message ?: "Erreur inconnue"}")
            }
        }
    }

    fun analyzeOutfit(bookingId: String, imageUrl: String) {
        viewModelScope.launch {
            _uiState.value = OutfitWeatherUiState.Loading
            try {
                val request = tn.esprit.wayfinder.models.AnalyzeOutfitRequest(bookingId, imageUrl)
                val outfit = apiService.analyzeOutfit(request)
                _uiState.value = OutfitWeatherUiState.Success(outfit)
            } catch (e: HttpException) {
                val errorMessage = try {
                    e.response()?.errorBody()?.string() ?: "Erreur lors de l'analyse"
                } catch (ex: Exception) {
                    "Erreur lors de l'analyse de la tenue"
                }
                _uiState.value = OutfitWeatherUiState.Error(errorMessage)
            } catch (e: IOException) {
                _uiState.value = OutfitWeatherUiState.Error("Erreur de connexion. Vérifiez votre Internet.")
            } catch (e: Exception) {
                _uiState.value = OutfitWeatherUiState.Error("Erreur inattendue: ${e.message}")
            }
        }
    }

    fun loadOutfit(outfitId: String) {
        viewModelScope.launch {
            _uiState.value = OutfitWeatherUiState.Loading
            try {
                android.util.Log.d("OutfitWeatherViewModel", "Loading outfit: $outfitId")
                val outfit = apiService.getOutfit(outfitId)
                android.util.Log.d("OutfitWeatherViewModel", "Outfit loaded successfully: ${outfit._id}")
                _uiState.value = OutfitWeatherUiState.Success(outfit)
            } catch (e: HttpException) {
                val errorMessage = try {
                    val errorBody = e.response()?.errorBody()?.string()
                    android.util.Log.e("OutfitWeatherViewModel", "HTTP error: ${e.code()}, body: $errorBody")
                    errorBody ?: "Erreur lors du chargement (${e.code()})"
                } catch (ex: Exception) {
                    android.util.Log.e("OutfitWeatherViewModel", "Error reading error body: ${ex.message}")
                    "Erreur lors du chargement de l'outfit"
                }
                _uiState.value = OutfitWeatherUiState.Error(errorMessage)
            } catch (e: IOException) {
                android.util.Log.e("OutfitWeatherViewModel", "IO error: ${e.message}")
                _uiState.value = OutfitWeatherUiState.Error("Erreur de connexion. Vérifiez votre Internet.")
            } catch (e: Exception) {
                android.util.Log.e("OutfitWeatherViewModel", "Unexpected error: ${e.message}", e)
                _uiState.value = OutfitWeatherUiState.Error("Erreur inattendue: ${e.message ?: "Erreur inconnue"}")
            }
        }
    }

    fun loadOutfitHistory(bookingId: String) {
        viewModelScope.launch {
            _isLoadingHistory.value = true
            try {
                android.util.Log.d("OutfitWeatherViewModel", "Loading outfit history for booking: $bookingId")
                val outfits = apiService.getOutfitsForBooking(bookingId)
                android.util.Log.d("OutfitWeatherViewModel", "Loaded ${outfits.size} outfits")
                _outfitHistory.value = outfits.sortedByDescending { it.createdAt }
            } catch (e: HttpException) {
                android.util.Log.e("OutfitWeatherViewModel", "HTTP error loading history: ${e.code()}")
                _outfitHistory.value = emptyList()
            } catch (e: IOException) {
                android.util.Log.e("OutfitWeatherViewModel", "IO error loading history: ${e.message}")
                _outfitHistory.value = emptyList()
            } catch (e: Exception) {
                android.util.Log.e("OutfitWeatherViewModel", "Unexpected error loading history: ${e.message}", e)
                _outfitHistory.value = emptyList()
            } finally {
                _isLoadingHistory.value = false
            }
        }
    }

    fun deleteOutfit(outfitId: String, bookingId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("OutfitWeatherViewModel", "Deleting outfit: $outfitId for booking: $bookingId")
                apiService.deleteOutfit(outfitId)
                android.util.Log.d("OutfitWeatherViewModel", "Outfit deleted successfully, refreshing history")
                // Remove from local state immediately for better UX
                _outfitHistory.value = _outfitHistory.value.filter { it._id != outfitId }
                // Refresh history from server to ensure consistency
                loadOutfitHistory(bookingId)
            } catch (e: HttpException) {
                android.util.Log.e("OutfitWeatherViewModel", "HTTP error deleting outfit: ${e.code()}, ${e.message()}")
                // Reload history even on error to show current state
                loadOutfitHistory(bookingId)
            } catch (e: IOException) {
                android.util.Log.e("OutfitWeatherViewModel", "IO error deleting outfit: ${e.message}")
                // Reload history even on error to show current state
                loadOutfitHistory(bookingId)
            } catch (e: Exception) {
                android.util.Log.e("OutfitWeatherViewModel", "Unexpected error deleting outfit: ${e.message}", e)
                // Reload history even on error to show current state
                loadOutfitHistory(bookingId)
            }
        }
    }

    fun resetState() {
        _uiState.value = OutfitWeatherUiState.Idle
    }
}

