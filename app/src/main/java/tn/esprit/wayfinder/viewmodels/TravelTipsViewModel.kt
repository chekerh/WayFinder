package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.presentation.traveltips.TravelTipsRepository

sealed class TravelTipsUiState {
    object Idle : TravelTipsUiState()
    object Loading : TravelTipsUiState()
    data class Success(val tips: List<TravelTip>) : TravelTipsUiState()
    data class Error(val message: String) : TravelTipsUiState()
}

class TravelTipsViewModel(private val travelTipsRepository: TravelTipsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<TravelTipsUiState>(TravelTipsUiState.Idle)
    val uiState: StateFlow<TravelTipsUiState> = _uiState.asStateFlow()

    fun loadTravelTips(destinationId: String, category: String? = null, limit: Int = 10) {
        viewModelScope.launch {
            _uiState.value = TravelTipsUiState.Loading
            try {
                val tips = travelTipsRepository.getTravelTips(destinationId, category, limit)
                _uiState.value = TravelTipsUiState.Success(tips)
            } catch (e: Exception) {
                _uiState.value = TravelTipsUiState.Error(e.message ?: "Failed to load travel tips")
            }
        }
    }

    fun generateTravelTips(
        destinationId: String,
        destinationName: String,
        city: String? = null,
        country: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = TravelTipsUiState.Loading
            try {
                val tips = travelTipsRepository.generateTravelTips(destinationId, destinationName, city, country)
                _uiState.value = TravelTipsUiState.Success(tips)
            } catch (e: Exception) {
                _uiState.value = TravelTipsUiState.Error(e.message ?: "Failed to generate travel tips")
            }
        }
    }

    fun markTipHelpful(tipId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val updatedTip = travelTipsRepository.markTipHelpful(tipId)
                val currentState = _uiState.value as? TravelTipsUiState.Success
                val updatedTips = currentState?.tips?.map { tip ->
                    if (tip.id == tipId) updatedTip else tip
                } ?: emptyList()
                _uiState.value = TravelTipsUiState.Success(updatedTips)
                onSuccess()
            } catch (e: Exception) {
                // Silently fail - don't update UI state on error
            }
        }
    }
}

