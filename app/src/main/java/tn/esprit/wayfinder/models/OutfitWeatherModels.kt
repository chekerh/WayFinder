package tn.esprit.wayfinder.models

import kotlinx.serialization.Serializable

@Serializable
data class Outfit(
    val _id: String? = null,
    val user_id: String,
    val booking_id: String,
    val image_url: String,
    val detected_items: List<String> = emptyList(),
    val weather_data: WeatherData? = null,
    val recommendation: OutfitRecommendation? = null,
    val is_approved: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class WeatherData(
    val temperature: Int,
    val condition: String,
    val humidity: Int? = null,
    val wind_speed: Double? = null
)

@Serializable
data class OutfitRecommendation(
    val is_suitable: Boolean,
    val score: Int,
    val feedback: String,
    val suggestions: List<String> = emptyList()
)

@Serializable
data class AnalyzeOutfitRequest(
    val booking_id: String,
    val image_url: String
)

@Serializable
data class UploadOutfitResponse(
    val message: String,
    val image_url: String,
    val analysis: Outfit
)

