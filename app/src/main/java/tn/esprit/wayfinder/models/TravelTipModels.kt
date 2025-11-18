package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TravelTip(
    @SerialName("_id") val id: String,
    @SerialName("destinationId") val destinationId: String,
    @SerialName("destinationName") val destinationName: String,
    val city: String? = null,
    val country: String? = null,
    val category: String, // "general", "transportation", "accommodation", "food", "culture", "safety", "budget", "weather"
    val title: String,
    val content: String,
    val tags: List<String> = emptyList(),
    @SerialName("helpfulCount") val helpfulCount: Int = 0,
    @SerialName("viewCount") val viewCount: Int = 0,
    @SerialName("isActive") val isActive: Boolean = true,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null
)

@Serializable
data class CreateTravelTipRequest(
    @SerialName("destinationId") val destinationId: String,
    @SerialName("destinationName") val destinationName: String,
    val city: String? = null,
    val country: String? = null,
    val category: String,
    val title: String,
    val content: String,
    val tags: List<String> = emptyList()
)

