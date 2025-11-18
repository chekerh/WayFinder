package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.presentation.pricealerts.PriceAlertsRepository

sealed class PriceAlertsUiState {
    object Idle : PriceAlertsUiState()
    object Loading : PriceAlertsUiState()
    data class Success(val alerts: List<PriceAlert>) : PriceAlertsUiState()
    data class Error(val message: String) : PriceAlertsUiState()
}

class PriceAlertsViewModel(private val priceAlertsRepository: PriceAlertsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<PriceAlertsUiState>(PriceAlertsUiState.Idle)
    val uiState: StateFlow<PriceAlertsUiState> = _uiState.asStateFlow()

    fun loadPriceAlerts(activeOnly: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = PriceAlertsUiState.Loading
            try {
                val alerts = priceAlertsRepository.getPriceAlerts(activeOnly)
                _uiState.value = PriceAlertsUiState.Success(alerts)
            } catch (e: Exception) {
                _uiState.value = PriceAlertsUiState.Error(e.message ?: "Failed to load price alerts")
            }
        }
    }

    fun createPriceAlert(request: CreatePriceAlertRequest, onSuccess: (PriceAlert) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val alert = priceAlertsRepository.createPriceAlert(request)
                val currentState = _uiState.value as? PriceAlertsUiState.Success
                _uiState.value = PriceAlertsUiState.Success(
                    alerts = listOf(alert) + (currentState?.alerts ?: emptyList())
                )
                onSuccess(alert)
            } catch (e: Exception) {
                _uiState.value = PriceAlertsUiState.Error(e.message ?: "Failed to create price alert")
            }
        }
    }

    fun updatePriceAlert(alertId: String, request: UpdatePriceAlertRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val updatedAlert = priceAlertsRepository.updatePriceAlert(alertId, request)
                val currentState = _uiState.value as? PriceAlertsUiState.Success
                val updatedAlerts = currentState?.alerts?.map { alert ->
                    if (alert.id == alertId) updatedAlert else alert
                } ?: emptyList()
                _uiState.value = PriceAlertsUiState.Success(alerts = updatedAlerts)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = PriceAlertsUiState.Error(e.message ?: "Failed to update price alert")
            }
        }
    }

    fun deletePriceAlert(alertId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                priceAlertsRepository.deletePriceAlert(alertId)
                val currentState = _uiState.value as? PriceAlertsUiState.Success
                val updatedAlerts = currentState?.alerts?.filter { it.id != alertId } ?: emptyList()
                _uiState.value = PriceAlertsUiState.Success(alerts = updatedAlerts)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = PriceAlertsUiState.Error(e.message ?: "Failed to delete price alert")
            }
        }
    }

    fun deactivatePriceAlert(alertId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val updatedAlert = priceAlertsRepository.deactivatePriceAlert(alertId)
                val currentState = _uiState.value as? PriceAlertsUiState.Success
                val updatedAlerts = currentState?.alerts?.map { alert ->
                    if (alert.id == alertId) updatedAlert else alert
                } ?: emptyList()
                _uiState.value = PriceAlertsUiState.Success(alerts = updatedAlerts)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = PriceAlertsUiState.Error(e.message ?: "Failed to deactivate price alert")
            }
        }
    }
}

