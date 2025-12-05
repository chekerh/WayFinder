package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Group Flight - Multiple users organizing a flight together
 */
@Serializable
data class GroupFlight(
    @SerialName("_id") val id: String,
    @SerialName("organizer_id") val organizerId: String,
    @SerialName("flight_offer_id") val flightOfferId: String? = null,
    @SerialName("destination_id") val destinationId: String,
    val destination: FlightDestination? = null,
    @SerialName("flight_offer") val flightOffer: FlightOffer? = null,
    @SerialName("members") val members: List<GroupFlightMember> = emptyList(),
    @SerialName("shared_hotel") val sharedHotel: SharedHotel? = null,
    @SerialName("total_cost") val totalCost: Double,
    @SerialName("cost_per_person") val costPerPerson: Double,
    @SerialName("savings") val savings: Double = 0.0, // Savings from sharing
    @SerialName("status") val status: GroupFlightStatus = GroupFlightStatus.PENDING,
    @SerialName("max_members") val maxMembers: Int = 4,
    @SerialName("departure_date") val departureDate: String? = null,
    @SerialName("return_date") val returnDate: String? = null,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String
)

@Serializable
enum class GroupFlightStatus {
    @SerialName("pending") PENDING,
    @SerialName("organizing") ORGANIZING,
    @SerialName("confirmed") CONFIRMED,
    @SerialName("cancelled") CANCELLED
}

@Serializable
data class GroupFlightMember(
    @SerialName("user_id") val userId: String,
    @SerialName("user") val user: UserPreview? = null,
    @SerialName("status") val status: MemberStatus = MemberStatus.INVITED,
    @SerialName("joined_at") val joinedAt: String? = null,
    @SerialName("payment_status") val paymentStatus: PaymentStatus = PaymentStatus.PENDING
)

@Serializable
enum class MemberStatus {
    @SerialName("invited") INVITED,
    @SerialName("accepted") ACCEPTED,
    @SerialName("declined") DECLINED
}

@Serializable
enum class PaymentStatus {
    @SerialName("pending") PENDING,
    @SerialName("paid") PAID,
    @SerialName("refunded") REFUNDED
}

@Serializable
data class SharedHotel(
    @SerialName("hotel_id") val hotelId: String? = null,
    @SerialName("name") val name: String,
    @SerialName("room_type") val roomType: String, // "single", "double", "triple", "quad"
    @SerialName("total_cost") val totalCost: Double,
    @SerialName("cost_per_person") val costPerPerson: Double,
    @SerialName("check_in") val checkIn: String,
    @SerialName("check_out") val checkOut: String,
    @SerialName("nights") val nights: Int
)

@Serializable
data class CreateGroupFlightRequest(
    @SerialName("destination_id") val destinationId: String,
    @SerialName("flight_offer_id") val flightOfferId: String? = null,
    @SerialName("max_members") val maxMembers: Int = 4,
    @SerialName("departure_date") val departureDate: String? = null,
    @SerialName("return_date") val returnDate: String? = null,
    @SerialName("shared_hotel") val sharedHotel: SharedHotelRequest? = null,
    @SerialName("invite_user_ids") val inviteUserIds: List<String> = emptyList()
)

@Serializable
data class SharedHotelRequest(
    @SerialName("name") val name: String,
    @SerialName("room_type") val roomType: String,
    @SerialName("total_cost") val totalCost: Double,
    @SerialName("check_in") val checkIn: String,
    @SerialName("check_out") val checkOut: String,
    @SerialName("nights") val nights: Int
)

@Serializable
data class JoinGroupFlightRequest(
    @SerialName("group_flight_id") val groupFlightId: String
)

@Serializable
data class GroupFlightInvitation(
    @SerialName("_id") val id: String,
    @SerialName("group_flight_id") val groupFlightId: String,
    @SerialName("group_flight") val groupFlight: GroupFlight? = null,
    @SerialName("inviter_id") val inviterId: String,
    @SerialName("inviter") val inviter: UserPreview? = null,
    @SerialName("invitee_id") val inviteeId: String,
    @SerialName("status") val status: MemberStatus = MemberStatus.INVITED,
    @SerialName("createdAt") val createdAt: String
)

@Serializable
data class GroupFlightCostBreakdown(
    @SerialName("flight_cost_per_person") val flightCostPerPerson: Double,
    @SerialName("hotel_cost_per_person") val hotelCostPerPerson: Double? = null,
    @SerialName("total_without_sharing") val totalWithoutSharing: Double,
    @SerialName("total_with_sharing") val totalWithSharing: Double,
    @SerialName("savings_per_person") val savingsPerPerson: Double,
    @SerialName("currency") val currency: String = "EUR"
)

