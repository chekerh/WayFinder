package tn.esprit.wayfinder.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonalizedRecommendations(
    val destinations: List<Destination>? = null,
    val offers: List<Offer>? = null,
    val activities: List<Activity>? = null,
    @SerialName("generated_at") val generatedAt: String,
    @SerialName("preferences_used") val preferencesUsed: Map<String, String>
)

@Serializable
data class Destination(
    val id: String,
    val name: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("match_score") val matchScore: Double,
    val reason: String,
    val highlights: List<String>,
    @SerialName("estimated_cost") val estimatedCost: EstimatedCost
)

@Serializable
data class EstimatedCost(
    val flight: Double,
    @SerialName("hotel_per_night") val hotelPerNight: Double,
    val currency: String = "USD"
)

@Serializable
data class Activity(
    val id: String,
    val name: String,
    val type: String,
    val destination: String,
    val price: Double,
    @SerialName("match_score") val matchScore: Double,
    val reason: String
)
