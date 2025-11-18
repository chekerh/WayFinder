package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityCoordinates(
    val lat: Double? = null,
    val lon: Double? = null
)

@Serializable
data class TravelActivity(
    val id: String,
    val city: String,
    val country: String,
    val name: String,
    val category: String,
    val description: String? = null,
    @SerialName("imageUrl") val imageUrl: String? = null,
    val address: String? = null,
    val price: Double? = null,
    val rating: Double? = null,
    val tags: List<String> = emptyList(),
    val coordinates: ActivityCoordinates? = null
)

@Serializable
data class ActivityFeedResponse(
    val city: String,
    val source: String,
    val total: Int,
    val items: List<TravelActivity>
)

