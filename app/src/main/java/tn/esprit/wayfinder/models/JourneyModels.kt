package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Journey(
    @SerialName("_id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("booking_id") val bookingId: String? = null,
    val destination: String,
    @SerialName("image_urls") val imageUrls: List<String> = emptyList(),
    val slides: List<JourneySlide> = emptyList(),
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("video_status") val videoStatus: String = "pending",
    @SerialName("music_theme") val musicTheme: String? = null,
    @SerialName("caption_text") val captionText: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("comments_count") val commentsCount: Int = 0,
    @SerialName("is_public") val isPublic: Boolean = true,
    @SerialName("is_liked") val isLiked: Boolean = false,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    val user: UserPreview? = null
)

@Serializable
data class JourneySlide(
    @SerialName("imageUrl") val imageUrl: String,
    val caption: String? = null
)

@Serializable
data class CreateJourneyRequest(
    @SerialName("booking_id") val bookingId: String? = null,
    val description: String? = null,
    val tags: List<String>? = null,
    @SerialName("is_public") val isPublic: Boolean? = null,
    @SerialName("music_theme") val musicTheme: String? = null,
    @SerialName("caption_text") val captionText: String? = null
)

@Serializable
data class UpdateJourneyRequest(
    val description: String? = null,
    val tags: List<String>? = null,
    @SerialName("is_public") val isPublic: Boolean? = null,
    @SerialName("music_theme") val musicTheme: String? = null,
    @SerialName("caption_text") val captionText: String? = null
)

@Serializable
data class JourneyComment(
    @SerialName("_id") val id: String,
    @SerialName("journey_id") val journeyId: String,
    @SerialName("user_id") val userId: String,
    val content: String,
    @SerialName("parent_comment_id") val parentCommentId: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    val user: UserPreview? = null
)

@Serializable
data class CreateJourneyCommentRequest(
    val content: String,
    @SerialName("parent_comment_id") val parentCommentId: String? = null
)

@Serializable
data class JourneyLikeResponse(
    val liked: Boolean,
    val message: String
)

@Serializable
data class CanShareJourneyResponse(
    @SerialName("canShare") val canShare: Boolean,
    @SerialName("confirmedBookingsCount") val confirmedBookingsCount: Int,
    val message: String
)

