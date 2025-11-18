package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItineraryActivity(
    val name: String,
    val description: String? = null,
    val location: String? = null,
    @SerialName("startTime") val startTime: String? = null,
    @SerialName("endTime") val endTime: String? = null,
    val category: String? = null,
    val cost: Double? = null,
    val currency: String? = "EUR",
    val notes: String? = null
)

@Serializable
data class DayPlan(
    val date: String,
    val activities: List<ItineraryActivity> = emptyList(),
    val notes: String? = null
)

@Serializable
data class Itinerary(
    @SerialName("_id") val id: String,
    @SerialName("userId") val userId: String,
    val title: String,
    val description: String? = null,
    val destination: String,
    @SerialName("startDate") val startDate: String,
    @SerialName("endDate") val endDate: String,
    val days: List<DayPlan> = emptyList(),
    val tags: List<String> = emptyList(),
    @SerialName("isPublic") val isPublic: Boolean = false,
    @SerialName("totalBudget") val totalBudget: Double? = null,
    val currency: String? = "EUR",
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null
)

@Serializable
data class CreateItineraryRequest(
    val title: String,
    val description: String? = null,
    val destination: String,
    @SerialName("startDate") val startDate: String,
    @SerialName("endDate") val endDate: String,
    val days: List<DayPlan>? = null,
    val tags: List<String>? = null,
    @SerialName("isPublic") val isPublic: Boolean = false,
    @SerialName("totalBudget") val totalBudget: Double? = null,
    val currency: String? = "EUR"
)

@Serializable
data class UpdateItineraryRequest(
    val title: String? = null,
    val description: String? = null,
    val destination: String? = null,
    @SerialName("startDate") val startDate: String? = null,
    @SerialName("endDate") val endDate: String? = null,
    val days: List<DayPlan>? = null,
    val tags: List<String>? = null,
    @SerialName("isPublic") val isPublic: Boolean? = null,
    @SerialName("totalBudget") val totalBudget: Double? = null,
    val currency: String? = null
)

@Serializable
data class AddActivityRequest(
    val name: String,
    val description: String? = null,
    val location: String? = null,
    @SerialName("startTime") val startTime: String? = null,
    @SerialName("endTime") val endTime: String? = null,
    val category: String? = null,
    val cost: Double? = null,
    val currency: String? = "EUR",
    val notes: String? = null
)

