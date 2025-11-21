package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.models.PaypalOrderRequest
import tn.esprit.wayfinder.presentation.payment.PaypalRepository
import java.util.UUID

sealed class PaymentUiState {
    object Idle : PaymentUiState()
    object Loading : PaymentUiState()
    data class PaymentLinkReady(val approvalUrl: String, val orderId: String) : PaymentUiState()
    data class Success(val orderId: String, val status: String) : PaymentUiState()
    data class Error(val message: String) : PaymentUiState()
}

/**
 * ViewModel for handling PayPal payments via the backend.
 */
class PaymentViewModel(
    private val paypalRepository: PaypalRepository
) : ViewModel() {
    
    private val _paymentState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val paymentState: StateFlow<PaymentUiState> = _paymentState.asStateFlow()
    
    fun createPayment(
        amount: Double,
        currency: String = "USD",
        successLink: String,
        failLink: String,
        description: String? = null
    ) {
        viewModelScope.launch {
            try {
                _paymentState.value = PaymentUiState.Loading
                
                val request = PaypalOrderRequest(
                    amount = amount,
                    currency = currency.uppercase(),
                    returnUrl = successLink,
                    cancelUrl = failLink,
                    description = description,
                    referenceId = UUID.randomUUID().toString()
                )

                val response = paypalRepository.createOrder(request)

                val approvalUrl = response.approvalUrl
                if (!approvalUrl.isNullOrBlank()) {
                    _paymentState.value = PaymentUiState.PaymentLinkReady(
                        approvalUrl = approvalUrl,
                        orderId = response.orderId
                    )
                } else {
                    _paymentState.value = PaymentUiState.Error("Missing PayPal approval link")
                }
            } catch (e: Exception) {
                _paymentState.value = PaymentUiState.Error(
                    e.message ?: "Failed to create PayPal order. Please check your connection."
                )
            }
        }
    }
    
    fun verifyPayment(orderId: String) {
        viewModelScope.launch {
            try {
                _paymentState.value = PaymentUiState.Loading
                
                val capture = paypalRepository.captureOrder(orderId)
                val status = capture.status ?: "UNKNOWN"
                
                    _paymentState.value = PaymentUiState.Success(
                    orderId = orderId,
                    status = status
                    )
            } catch (e: Exception) {
                _paymentState.value = PaymentUiState.Error(
                    e.message ?: "Failed to capture PayPal order"
                )
            }
        }
    }
    
    fun resetPaymentState() {
        _paymentState.value = PaymentUiState.Idle
    }
}

