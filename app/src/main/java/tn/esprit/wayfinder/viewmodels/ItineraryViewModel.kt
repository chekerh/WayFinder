package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.presentation.itinerary.ItineraryRepository

sealed class ItineraryUiState {
    object Idle : ItineraryUiState()
    object Loading : ItineraryUiState()
    data class Success(val itineraries: List<Itinerary>) : ItineraryUiState()
    data class ItineraryLoaded(val itinerary: Itinerary) : ItineraryUiState()
    data class Error(val message: String) : ItineraryUiState()
}

class ItineraryViewModel(private val itineraryRepository: ItineraryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<ItineraryUiState>(ItineraryUiState.Idle)
    val uiState: StateFlow<ItineraryUiState> = _uiState.asStateFlow()

    fun loadItineraries(includePublic: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = ItineraryUiState.Loading
            try {
                val itineraries = itineraryRepository.getItineraries(includePublic)
                _uiState.value = ItineraryUiState.Success(itineraries)
            } catch (e: Exception) {
                _uiState.value = ItineraryUiState.Error(e.message ?: "Failed to load itineraries")
            }
        }
    }

    fun loadItinerary(id: String) {
        viewModelScope.launch {
            _uiState.value = ItineraryUiState.Loading
            try {
                val itinerary = itineraryRepository.getItinerary(id)
                _uiState.value = ItineraryUiState.ItineraryLoaded(itinerary)
            } catch (e: Exception) {
                _uiState.value = ItineraryUiState.Error(e.message ?: "Failed to load itinerary")
            }
        }
    }

    fun createItinerary(request: CreateItineraryRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                itineraryRepository.createItinerary(request)
                loadItineraries() // Refresh list
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = ItineraryUiState.Error(e.message ?: "Failed to create itinerary")
            }
        }
    }

    fun updateItinerary(id: String, request: UpdateItineraryRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                itineraryRepository.updateItinerary(id, request)
                loadItinerary(id) // Refresh current itinerary
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = ItineraryUiState.Error(e.message ?: "Failed to update itinerary")
            }
        }
    }

    fun deleteItinerary(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                itineraryRepository.deleteItinerary(id)
                loadItineraries() // Refresh list
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = ItineraryUiState.Error(e.message ?: "Failed to delete itinerary")
            }
        }
    }

    fun addActivity(
        itineraryId: String,
        dayDate: String,
        activity: AddActivityRequest,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                itineraryRepository.addActivity(itineraryId, dayDate, activity)
                loadItinerary(itineraryId) // Refresh current itinerary
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = ItineraryUiState.Error(e.message ?: "Failed to add activity")
            }
        }
    }

    fun removeActivity(
        itineraryId: String,
        dayDate: String,
        activityIndex: Int,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                itineraryRepository.removeActivity(itineraryId, dayDate, activityIndex)
                loadItinerary(itineraryId) // Refresh current itinerary
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = ItineraryUiState.Error(e.message ?: "Failed to remove activity")
            }
        }
    }
}

