package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.models.FlouciPaymentRequest
import tn.esprit.wayfinder.presentation.payment.FlouciRepository
import java.util.UUID

sealed class PaymentUiState {
    object Idle : PaymentUiState()
    object Loading : PaymentUiState()
    data class PaymentLinkReady(val paymentLink: String, val paymentId: String) : PaymentUiState()
    data class Success(val paymentId: String, val status: String) : PaymentUiState()
    data class Error(val message: String) : PaymentUiState()
}

/**
 * ViewModel for handling Flouci payments.
 * All Flouci API calls are made through the backend for security.
 */
class PaymentViewModel(
    private val flouciRepository: FlouciRepository
) : ViewModel() {
    
    private val _paymentState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val paymentState: StateFlow<PaymentUiState> = _paymentState.asStateFlow()
    
    /**
     * Create a Flouci payment request through the backend.
     * @param amount Amount in smallest currency unit (e.g., cents for TND)
     * @param currency Currency code (TND, EUR, USD, etc.)
     * @param successLink Deep link or URL to redirect on success
     * @param failLink Deep link or URL to redirect on failure
     * @param customerName Optional customer name
     * @param customerEmail Optional customer email
     * @param customerPhone Optional customer phone
     */
    fun createPayment(
        amount: Double,
        currency: String = "TND",
        successLink: String,
        failLink: String,
        customerName: String? = null,
        customerEmail: String? = null,
        customerPhone: String? = null
    ) {
        viewModelScope.launch {
            try {
                _paymentState.value = PaymentUiState.Loading
                
                // Convert amount to smallest currency unit (cents/millimes)
                val amountInCents = when (currency.uppercase()) {
                    "TND" -> (amount * 1000).toInt() // TND uses millimes
                    "EUR", "USD" -> (amount * 100).toInt() // EUR/USD use cents
                    else -> (amount * 100).toInt() // Default to cents
                }
                
                val request = FlouciPaymentRequest(
                    amount = amountInCents,
                    currency = currency.uppercase(),
                    successLink = successLink,
                    failLink = failLink,
                    appTransactionId = UUID.randomUUID().toString(),
                    appTransactionTime = System.currentTimeMillis(),
                    customer = if (customerName != null || customerEmail != null || customerPhone != null) {
                        tn.esprit.wayfinder.models.FlouciCustomer(
                            id = null,
                            name = customerName,
                            email = customerEmail,
                            phone = customerPhone
                        )
                    } else null,
                    webhook = null // Can be configured on backend
                )
                
                // Call backend endpoint (which handles Flouci API)
                val response = flouciRepository.createPayment(request)
                
                if (response.status == "OK" && response.result != null) {
                    _paymentState.value = PaymentUiState.PaymentLinkReady(
                        paymentLink = response.result.link,
                        paymentId = response.result.paymentId
                    )
                } else {
                    _paymentState.value = PaymentUiState.Error(
                        response.message ?: "Failed to create payment"
                    )
                }
            } catch (e: Exception) {
                _paymentState.value = PaymentUiState.Error(
                    e.message ?: "Failed to create payment. Please check your internet connection."
                )
            }
        }
    }
    
    /**
     * Verify payment status through the backend.
     * The backend handles the actual Flouci API call.
     */
    fun verifyPayment(paymentId: String) {
        viewModelScope.launch {
            try {
                _paymentState.value = PaymentUiState.Loading
                
                // Call backend endpoint (which handles Flouci API)
                val response = flouciRepository.getPaymentStatus(paymentId)
                
                if (response.status == "OK" && response.result != null) {
                    val paymentStatus = response.result.status
                    _paymentState.value = PaymentUiState.Success(
                        paymentId = paymentId,
                        status = paymentStatus
                    )
                } else {
                    _paymentState.value = PaymentUiState.Error(
                        response.message ?: "Failed to verify payment"
                    )
                }
            } catch (e: Exception) {
                _paymentState.value = PaymentUiState.Error(
                    e.message ?: "Failed to verify payment"
                )
            }
        }
    }
    
    fun resetPaymentState() {
        _paymentState.value = PaymentUiState.Idle
    }
}

