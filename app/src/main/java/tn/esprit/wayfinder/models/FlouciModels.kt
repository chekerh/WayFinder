package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Flouci Payment Request
 */
@Serializable
data class FlouciPaymentRequest(
    @SerialName("amount")
    val amount: Int, // Amount in smallest currency unit (e.g., cents for TND)
    
    @SerialName("currency")
    val currency: String = "TND", // TND, EUR, USD, etc.
    
    @SerialName("success_link")
    val successLink: String,
    
    @SerialName("fail_link")
    val failLink: String,
    
    @SerialName("app_transaction_id")
    val appTransactionId: String? = null,
    
    @SerialName("app_transaction_time")
    val appTransactionTime: Long? = null,
    
    @SerialName("customer")
    val customer: FlouciCustomer? = null,
    
    @SerialName("webhook")
    val webhook: String? = null
)

@Serializable
data class FlouciCustomer(
    @SerialName("id")
    val id: String? = null,
    
    @SerialName("name")
    val name: String? = null,
    
    @SerialName("email")
    val email: String? = null,
    
    @SerialName("phone")
    val phone: String? = null
)

/**
 * Flouci Payment Response
 */
@Serializable
data class FlouciPaymentResponse(
    @SerialName("status")
    val status: String,
    
    @SerialName("message")
    val message: String? = null,
    
    @SerialName("result")
    val result: FlouciPaymentResult? = null
)

@Serializable
data class FlouciPaymentResult(
    @SerialName("link")
    val link: String,
    
    @SerialName("payment_id")
    val paymentId: String
)

/**
 * Flouci Payment Status Response
 */
@Serializable
data class FlouciPaymentStatusResponse(
    @SerialName("status")
    val status: String,
    
    @SerialName("message")
    val message: String? = null,
    
    @SerialName("result")
    val result: FlouciPaymentStatus? = null
)

@Serializable
data class FlouciPaymentStatus(
    @SerialName("id")
    val id: String,
    
    @SerialName("status")
    val status: String, // "success", "failed", "pending"
    
    @SerialName("amount")
    val amount: Int,
    
    @SerialName("currency")
    val currency: String,
    
    @SerialName("app_transaction_id")
    val appTransactionId: String? = null,
    
    @SerialName("created_at")
    val createdAt: String? = null,
    
    @SerialName("customer")
    val customer: FlouciCustomer? = null
)

