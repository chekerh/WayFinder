package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Review(
    val id: String,
    @SerialName("userId") val userId: User,
    @SerialName("itemType") val itemType: String,
    @SerialName("itemId") val itemId: String,
    val rating: Int,
    val comment: String? = null,
    val details: Map<String, Int>? = null,
    @SerialName("isVisible") val isVisible: Boolean = true,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String
)

@Serializable
data class CreateReviewRequest(
    @SerialName("itemType") val itemType: String,
    @SerialName("itemId") val itemId: String,
    val rating: Int,
    val comment: String? = null,
    val details: Map<String, Int>? = null
)

@Serializable
data class UpdateReviewRequest(
    val rating: Int? = null,
    val comment: String? = null,
    val details: Map<String, Int>? = null
)

@Serializable
data class ReviewStatsResponse(
    @SerialName("averageRating") val averageRating: Double,
    @SerialName("totalReviews") val totalReviews: Int,
    @SerialName("ratingDistribution") val ratingDistribution: Map<String, Int>
)

