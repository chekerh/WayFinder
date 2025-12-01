package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserPreview(
    @SerialName("_id") val id: String,
    val username: String,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
    @SerialName("followedAt") val followedAt: String? = null
)

@Serializable
data class SharedTrip(
    @SerialName("_id") val id: String,
    @SerialName("userId") val userId: UserPreview,
    val title: String,
    val description: String? = null,
    @SerialName("tripType") val tripType: String, // "itinerary", "booking", "destination", "custom"
    @SerialName("tripId") val tripId: String? = null,
    val images: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val metadata: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    @SerialName("likesCount") val likesCount: Int = 0,
    @SerialName("commentsCount") val commentsCount: Int = 0,
    @SerialName("sharesCount") val sharesCount: Int = 0,
    @SerialName("isPublic") val isPublic: Boolean = true,
    @SerialName("isVisible") val isVisible: Boolean = true,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String
)

@Serializable
data class ShareTripRequest(
    val title: String,
    val description: String? = null,
    @SerialName("tripType") val tripType: String,
    @SerialName("tripId") val tripId: String? = null,
    val images: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val metadata: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    @SerialName("isPublic") val isPublic: Boolean = true
)

@Serializable
data class UpdateSharedTripRequest(
    val title: String? = null,
    val description: String? = null,
    val images: List<String>? = null,
    val tags: List<String>? = null,
    @SerialName("isPublic") val isPublic: Boolean? = null
)

@Serializable
data class FollowUserRequest(
    @SerialName("userId") val userId: String
)

@Serializable
data class FollowStatusResponse(
    @SerialName("isFollowing") val isFollowing: Boolean
)

@Serializable
data class FollowResponse(
    val message: String,
    val following: Boolean
)

@Serializable
data class FollowCountsResponse(
    val followers: Int,
    val following: Int
)

@Serializable
data class LikeResponse(
    val message: String,
    @SerialName("likesCount") val likesCount: Int
)

@Serializable
data class CountryMemory(
    val country: String,
    val lat: Double,
    val lng: Double,
    val trips: List<SharedTrip> = emptyList(),
    val count: Int = 0
)

@Serializable
data class MapMemoriesResponse(
    val countries: List<CountryMemory> = emptyList(),
    @SerialName("totalCountries") val totalCountries: Int = 0,
    @SerialName("totalMemories") val totalMemories: Int = 0
)

