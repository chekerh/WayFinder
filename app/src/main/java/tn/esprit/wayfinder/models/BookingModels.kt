package tn.esprit.wayfinder.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Offer(
    val id: String,
    val type: String,
    val destination: String,
    val price: Double
)

@Serializable
data class OfferComparison(
    @SerialName("offer_id") val offerId: String,
    @SerialName("base_price") val basePrice: Double,
    val taxes: Double,
    val baggage: Double,
    @SerialName("service_fees") val serviceFees: Double,
    val total: Double
)

@Serializable
data class Booking(
    @SerialName("_id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("offer_id") val offerId: String,
    val status: BookingStatus,
    @SerialName("payment_details") val paymentDetails: Map<String, @Contextual Any>,
    @SerialName("booking_date") val bookingDate: String,
    @SerialName("confirmation_number") val confirmationNumber: String,
    @SerialName("total_price") val totalPrice: Double,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String
)

enum class BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
