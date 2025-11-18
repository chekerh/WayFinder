package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class PriceAlert(
    @SerialName("_id") val id: String,
    @SerialName("userId") val userId: String,
    @SerialName("alertType") val alertType: String, // "flight", "hotel", "destination", "activity"
    @SerialName("itemId") val itemId: String,
    @SerialName("itemData") val itemData: Map<String, JsonElement>,
    @SerialName("targetPrice") val targetPrice: Double,
    val currency: String,
    val condition: String = "below", // "below" or "above"
    @SerialName("currentPrice") val currentPrice: Double? = null,
    @SerialName("isActive") val isActive: Boolean = true,
    @SerialName("isTriggered") val isTriggered: Boolean = false,
    @SerialName("triggeredAt") val triggeredAt: String? = null,
    @SerialName("expiresAt") val expiresAt: String? = null,
    @SerialName("triggerCount") val triggerCount: Int = 0,
    @SerialName("sendNotification") val sendNotification: Boolean = true,
    @SerialName("sendEmail") val sendEmail: Boolean = false,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String
)

@Serializable
data class CreatePriceAlertRequest(
    @SerialName("alertType") val alertType: String,
    @SerialName("itemId") val itemId: String,
    @SerialName("itemData") val itemData: Map<String, JsonElement>,
    @SerialName("targetPrice") val targetPrice: Double,
    val currency: String,
    val condition: String = "below",
    @SerialName("currentPrice") val currentPrice: Double? = null,
    @SerialName("expiresAt") val expiresAt: String? = null,
    @SerialName("sendNotification") val sendNotification: Boolean = true,
    @SerialName("sendEmail") val sendEmail: Boolean = false
)

@Serializable
data class UpdatePriceAlertRequest(
    @SerialName("targetPrice") val targetPrice: Double? = null,
    val condition: String? = null,
    @SerialName("expiresAt") val expiresAt: String? = null,
    @SerialName("isActive") val isActive: Boolean? = null,
    @SerialName("sendNotification") val sendNotification: Boolean? = null,
    @SerialName("sendEmail") val sendEmail: Boolean? = null
)

