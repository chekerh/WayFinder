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
data class TripDetails(
    val origin: String? = null,
    val destination: String? = null,
    @SerialName("departure_date") val departureDate: String? = null,
    @SerialName("return_date") val returnDate: String? = null,
    @SerialName("travel_class") val travelClass: String? = null,
    val seats: String? = null
)

@Serializable
data class BookingPassenger(
    @SerialName("full_name") val fullName: String,
    @SerialName("traveler_type") val travelerType: String? = null,
    @SerialName("document_number") val documentNumber: String? = null
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
    @SerialName("trip_details") val tripDetails: TripDetails? = null,
    val passengers: List<BookingPassenger>? = emptyList(),
    val notes: String? = null,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String
)

@Serializable
data class CreateBookingRequest(
    @SerialName("offer_id") val offerId: String,
    @SerialName("payment_details") val paymentDetails: Map<String, @Contextual Any>,
    @SerialName("trip_details") val tripDetails: TripDetails? = null,
    val passengers: List<BookingPassenger>? = null,
    val notes: String? = null,
    @SerialName("total_price") val totalPrice: Double? = null
)

@Serializable
data class UpdateBookingRequest(
    @SerialName("payment_details") val paymentDetails: Map<String, @Contextual Any>? = null,
    @SerialName("trip_details") val tripDetails: TripDetails? = null,
    val passengers: List<BookingPassenger>? = null,
    val notes: String? = null,
    val status: BookingStatus? = null,
    @SerialName("total_price") val totalPrice: Double? = null
)

enum class BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
