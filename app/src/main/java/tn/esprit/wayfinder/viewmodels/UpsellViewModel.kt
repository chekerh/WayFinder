package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.models.SelectedUpsell
import tn.esprit.wayfinder.models.UpsellProduct
import tn.esprit.wayfinder.presentation.upsells.UpsellRepository

sealed class UpsellUiState {
    object Idle : UpsellUiState()
    object Loading : UpsellUiState()
    data class Success(val products: List<UpsellProduct>) : UpsellUiState()
    data class Error(val message: String) : UpsellUiState()
}

class UpsellViewModel(private val upsellRepository: UpsellRepository) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UpsellUiState>(UpsellUiState.Idle)
    val uiState: StateFlow<UpsellUiState> = _uiState.asStateFlow()
    
    private val _selectedUpsells = MutableStateFlow<List<SelectedUpsell>>(emptyList())
    val selectedUpsells: StateFlow<List<SelectedUpsell>> = _selectedUpsells.asStateFlow()
    
    fun loadUpsellProducts(
        destinationId: String? = null, 
        dates: String? = null,
        destinationCity: String? = null,
        userPreferences: Map<String, kotlinx.serialization.json.JsonElement>? = null
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = UpsellUiState.Loading
                val products = upsellRepository.getUpsellProducts(
                    destinationId = destinationId,
                    dates = dates,
                    destinationCity = destinationCity,
                    userPreferences = userPreferences
                )
                _uiState.value = UpsellUiState.Success(products)
            } catch (e: Exception) {
                _uiState.value = UpsellUiState.Error(
                    e.message ?: "Failed to load upsell products"
                )
            }
        }
    }
    
    fun toggleUpsell(product: UpsellProduct) {
        val current = _selectedUpsells.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.productId == product.id }
        
        if (existingIndex >= 0) {
            // Remove if already selected
            current.removeAt(existingIndex)
        } else {
            // Add new selection
            val commissionAmount = (product.price * product.commissionRate / 100.0)
            val selected = SelectedUpsell(
                productId = product.id,
                quantity = 1,
                price = product.price,
                currency = product.currency,
                commissionRate = product.commissionRate,
                commissionAmount = commissionAmount
            )
            current.add(selected)
        }
        
        _selectedUpsells.value = current
    }
    
    fun isUpsellSelected(productId: String): Boolean {
        return _selectedUpsells.value.any { it.productId == productId }
    }
    
    fun getTotalUpsellPrice(): Double {
        return _selectedUpsells.value.sumOf { it.price * it.quantity }
    }
    
    fun getTotalCommission(): Double {
        return _selectedUpsells.value.sumOf { it.commissionAmount * it.quantity }
    }
    
    fun clearSelection() {
        _selectedUpsells.value = emptyList()
    }
}

