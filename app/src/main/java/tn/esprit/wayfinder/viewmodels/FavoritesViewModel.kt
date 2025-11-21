package tn.esprit.wayfinder.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import tn.esprit.wayfinder.models.Favorite
import tn.esprit.wayfinder.models.FlightDestination
import tn.esprit.wayfinder.presentation.favorites.FavoritesRepository

sealed class FavoritesUiState {
    object Idle : FavoritesUiState()
    object Loading : FavoritesUiState()
    data class Success(
        val favorites: List<Favorite>,
        val destinations: List<FlightDestination>
    ) : FavoritesUiState()
    data class Error(val message: String) : FavoritesUiState()
}

class FavoritesViewModel(
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    // Start with Idle state, loading will be triggered from UI
    private val _uiState = MutableStateFlow<FavoritesUiState>(FavoritesUiState.Idle)
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val _favoriteCount = MutableStateFlow(0)
    val favoriteCount: StateFlow<Int> = _favoriteCount.asStateFlow()

    fun loadFavorites() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = FavoritesUiState.Loading
                Log.d("FavoritesViewModel", "Loading favorites...")
                Log.d("FavoritesViewModel", "Thread: ${Thread.currentThread().name}")
                Log.d("FavoritesViewModel", "Calling favoritesRepository.getFavorites('flight')...")
                
                // Use a safety timeout of 200 seconds (longer than OkHttp's 180s read timeout)
                // This prevents infinite loading while allowing OkHttp to handle its own timeouts
                val favorites = withTimeoutOrNull(200000) {
                    try {
                        Log.d("FavoritesViewModel", "Inside withTimeoutOrNull, about to call API...")
                        Log.d("FavoritesViewModel", "Thread in timeout block: ${Thread.currentThread().name}")
                        // Let OkHttp handle its own timeouts (30s connect, 180s read)
                        // This allows Render free tier to wake up (takes ~50 seconds)
                        Log.d("FavoritesViewModel", "About to call favoritesRepository.getFavorites('flight')...")
                        val result = favoritesRepository.getFavorites("flight")
                        Log.d("FavoritesViewModel", "Repository call returned, received ${result.size} favorites")
                        result
                    } catch (e: Exception) {
                        Log.e("FavoritesViewModel", "Error inside withTimeoutOrNull: ${e.message}", e)
                        e.printStackTrace()
                        throw e
                    }
                }
                
                if (favorites == null) {
                    // Timeout occurred (either our 200s timeout or OkHttp's 180s timeout)
                    Log.e("FavoritesViewModel", "Timeout loading favorites (>180s)")
                    _uiState.value = FavoritesUiState.Error(
                        "Le serveur met trop de temps à répondre (timeout). " +
                        "Le serveur Render peut être en veille. Veuillez réessayer."
                    )
                    return@launch
                }
                
                Log.d("FavoritesViewModel", "Favorites loaded: ${favorites.size} items")
                
                val destinations = favorites.mapNotNull { favorite ->
                    convertFavoriteToDestination(favorite)
                }
                Log.d("FavoritesViewModel", "Destinations converted: ${destinations.size} items")
                
                _uiState.value = FavoritesUiState.Success(favorites, destinations)
                _favoriteCount.value = favorites.size
                Log.d("FavoritesViewModel", "State updated to Success")
            } catch (e: TimeoutCancellationException) {
                Log.e("FavoritesViewModel", "Timeout exception: ${e.message}", e)
                _uiState.value = FavoritesUiState.Error(
                    "Le serveur met trop de temps à répondre (timeout >180s). " +
                    "Le serveur Render peut être en veille. Veuillez réessayer."
                )
            } catch (e: Exception) {
                Log.e("FavoritesViewModel", "Error loading favorites: ${e.message}", e)
                e.printStackTrace()
                
                // Parse error message for better user feedback
                val errorMessage = when {
                    e.message?.contains("401") == true || 
                    e.message?.contains("Unauthorized") == true -> 
                        "Vous devez être connecté pour voir vos favoris"
                    e.message?.contains("404") == true -> 
                        "Service non disponible. Veuillez réessayer plus tard."
                    e.message?.contains("timeout") == true || 
                    e.message?.contains("Timeout") == true ||
                    e.message?.contains("Canceled") == true ||
                    e.javaClass.simpleName == "SocketTimeoutException" -> 
                        "Le serveur met trop de temps à répondre (Render en veille). Réessayez dans 10-15 secondes."
                    e.message?.contains("Unable to resolve host") == true || 
                    e.javaClass.simpleName == "UnknownHostException" -> 
                        "Impossible de se connecter au serveur. Vérifiez votre connexion Internet."
                    e.message?.contains("ConnectException") == true ||
                    e.javaClass.simpleName == "ConnectException" -> 
                        "Le serveur est en veille. Première connexion peut prendre 30-50 secondes. Réessayez."
                    e.message?.contains("IOException") == true ||
                    e.javaClass.simpleName == "IOException" -> 
                        "Erreur de connexion. Le serveur peut être en veille. Réessayez."
                    else -> {
                        Log.e("FavoritesViewModel", "Unknown error: ${e.javaClass.simpleName} - ${e.message}")
                        "Échec du chargement: ${e.message ?: "Erreur inconnue"}. Réessayez."
                    }
                }
                
                _uiState.value = FavoritesUiState.Error(errorMessage)
            }
        }
    }

    fun addFavorite(
        itemType: String,
        itemId: String,
        itemData: Map<String, Any>? = null
    ) {
        viewModelScope.launch {
            try {
                favoritesRepository.addFavorite(itemType, itemId, itemData)
                loadFavorites() // Refresh list
            } catch (e: Exception) {
                // Silently fail or show error
            }
        }
    }

    fun removeFavorite(itemType: String, itemId: String) {
        viewModelScope.launch {
            try {
                favoritesRepository.removeFavorite(itemType, itemId)
                loadFavorites() // Refresh list
            } catch (e: Exception) {
                // Silently fail or show error
            }
        }
    }

    fun checkFavorite(itemType: String, itemId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val isFavorite = favoritesRepository.checkFavorite(itemType, itemId)
                onResult(isFavorite)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun loadFavoriteCount() {
        viewModelScope.launch {
            try {
                val count = favoritesRepository.getFavoriteCount()
                _favoriteCount.value = count
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    private fun convertFavoriteToDestination(favorite: Favorite): FlightDestination? {
        val itemData = favorite.itemData
        return try {
            // Helper function to extract string from JsonElement
            fun getString(key: String): String? {
                val element = itemData[key] ?: return null
                return when {
                    element is JsonPrimitive -> {
                        val str = element.content
                        str.takeIf { it.isNotBlank() && it != "null" }
                    }
                    else -> element.toString().trim('"').takeIf { it.isNotBlank() && it != "null" }
                }
            }
            
            // Helper function to extract number from JsonElement
            fun getDouble(key: String): Double? {
                val element = itemData[key] ?: return null
                return when {
                    element is JsonPrimitive -> {
                        element.content.toDoubleOrNull()
                    }
                    else -> element.toString().trim('"').toDoubleOrNull()
                }
            }
            
            // Try both camelCase and snake_case for imageUrl
            val imageUrl = getString("imageUrl") 
                ?: getString("image_url")
                ?: getString("image")
            
            Log.d("FavoritesViewModel", "Converting favorite: ${favorite.itemId}, itemData keys: ${itemData.keys}, imageUrl: ${imageUrl?.take(50) ?: "null"}")
            
            FlightDestination(
                id = favorite.itemId,
                name = getString("name") ?: "Unknown",
                city = getString("city") ?: "",
                country = getString("country") ?: "",
                imageUrl = imageUrl,
                price = getDouble("price"),
                currency = getString("currency") ?: "EUR",
                description = getString("description"),
                departureDate = getString("departureDate") ?: getString("departure_date"),
                arrivalDate = getString("arrivalDate") ?: getString("arrival_date"),
                airline = getString("airline")
            ).also {
                Log.d("FavoritesViewModel", "Converted favorite: ${it.name}, imageUrl: ${it.imageUrl?.take(50) ?: "null"}")
            }
        } catch (e: Exception) {
            Log.e("FavoritesViewModel", "Error converting favorite to destination: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }
}

