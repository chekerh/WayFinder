package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Favorite(
    @SerialName("_id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("item_type") val itemType: String,
    @SerialName("item_id") val itemId: String,
    @SerialName("item_data") val itemData: Map<String, kotlinx.serialization.json.JsonElement>,
    @SerialName("favorited_at") val favoritedAt: String,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null
)

@Serializable
data class CreateFavoriteRequest(
    @SerialName("item_type") val itemType: String,
    @SerialName("item_id") val itemId: String,
    @SerialName("item_data") val itemData: Map<String, kotlinx.serialization.json.JsonElement>? = null
)

@Serializable
data class FavoriteCheckResponse(
    @SerialName("isFavorite") val isFavorite: Boolean
)

@Serializable
data class FavoriteCountResponse(
    val count: Int
)

