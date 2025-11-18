package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    private val _uiState = MutableStateFlow<FavoritesUiState>(FavoritesUiState.Idle)
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val _favoriteCount = MutableStateFlow(0)
    val favoriteCount: StateFlow<Int> = _favoriteCount.asStateFlow()

    fun loadFavorites() {
        viewModelScope.launch {
            try {
                _uiState.value = FavoritesUiState.Loading
                val favorites = favoritesRepository.getFavorites("flight")
                val destinations = favorites.mapNotNull { favorite ->
                    convertFavoriteToDestination(favorite)
                }
                _uiState.value = FavoritesUiState.Success(favorites, destinations)
                _favoriteCount.value = favorites.size
            } catch (e: Exception) {
                _uiState.value = FavoritesUiState.Error(
                    e.message ?: "Failed to load favorites"
                )
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
            FlightDestination(
                id = favorite.itemId,
                name = itemData["name"]?.toString() ?: "Unknown",
                city = itemData["city"]?.toString() ?: "",
                country = itemData["country"]?.toString() ?: "",
                imageUrl = itemData["imageUrl"]?.toString(),
                price = itemData["price"]?.toString()?.toDoubleOrNull(),
                currency = itemData["currency"]?.toString() ?: "EUR",
                description = itemData["description"]?.toString(),
                departureDate = itemData["departureDate"]?.toString(),
                arrivalDate = itemData["arrivalDate"]?.toString(),
                airline = itemData["airline"]?.toString()
            )
        } catch (e: Exception) {
            null
        }
    }
}

