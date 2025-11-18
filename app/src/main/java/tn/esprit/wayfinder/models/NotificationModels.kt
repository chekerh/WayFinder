package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Notification(
    @SerialName("_id") val id: String,
    @SerialName("userId") val userId: String,
    val type: String,
    val title: String,
    val message: String,
    val data: Map<String, JsonElement>? = null,
    @SerialName("isRead") val isRead: Boolean = false,
    @SerialName("readAt") val readAt: String? = null,
    @SerialName("actionUrl") val actionUrl: String? = null,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String
)

@Serializable
data class CreateNotificationRequest(
    val type: String,
    val title: String,
    val message: String,
    val data: Map<String, JsonElement>? = null,
    @SerialName("actionUrl") val actionUrl: String? = null
)

@Serializable
data class UnreadCountResponse(
    val count: Int
)

