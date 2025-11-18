package tn.esprit.wayfinder.presentation.payment

import tn.esprit.wayfinder.models.FlouciPaymentRequest
import tn.esprit.wayfinder.models.FlouciPaymentResponse
import tn.esprit.wayfinder.models.FlouciPaymentStatusResponse
import tn.esprit.wayfinder.network.ApiService

/**
 * Repository for Flouci payment operations.
 * All Flouci API calls are handled by the backend for security.
 */
class FlouciRepository(private val apiService: ApiService) {
    
    /**
     * Create a Flouci payment through the backend.
     * The backend handles the actual Flouci API call.
     */
    suspend fun createPayment(request: FlouciPaymentRequest): FlouciPaymentResponse {
        return apiService.createFlouciPayment(request)
    }
    
    /**
     * Get payment status from Flouci through the backend.
     * The backend handles the actual Flouci API call.
     */
    suspend fun getPaymentStatus(paymentId: String): FlouciPaymentStatusResponse {
        return apiService.getFlouciPaymentStatus(paymentId)
    }
}

