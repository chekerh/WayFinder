package tn.esprit.wayfinder.presentation.payment

import tn.esprit.wayfinder.models.PaypalCaptureResponse
import tn.esprit.wayfinder.models.PaypalOrderRequest
import tn.esprit.wayfinder.models.PaypalOrderResponse
import tn.esprit.wayfinder.models.PaypalOrderStatusResponse
import tn.esprit.wayfinder.network.ApiService

class PaypalRepository(private val apiService: ApiService) {

    suspend fun createOrder(request: PaypalOrderRequest): PaypalOrderResponse {
        return apiService.createPaypalOrder(request)
    }

    suspend fun captureOrder(orderId: String): PaypalCaptureResponse {
        return apiService.capturePaypalOrder(orderId)
    }

    suspend fun fetchOrderStatus(orderId: String): PaypalOrderStatusResponse {
        return apiService.getPaypalOrder(orderId)
    }
}


