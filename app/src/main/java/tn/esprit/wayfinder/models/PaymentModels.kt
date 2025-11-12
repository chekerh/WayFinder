package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Payment(
    @SerialName("_id") val id: String,
    @SerialName("transaction_id") val transactionId: String,
    @SerialName("user_id") val userId: String,
    val amount: Double,
    @SerialName("payment_status") val paymentStatus: String,
    @SerialName("transaction_date") val transactionDate: String,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String
)
