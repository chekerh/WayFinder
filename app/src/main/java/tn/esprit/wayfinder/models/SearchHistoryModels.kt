package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SearchHistory(
    @SerialName("_id") val id: String,
    @SerialName("userId") val userId: String,
    @SerialName("searchType") val searchType: String, // "flight", "hotel", "destination", "activity", "general"
    @SerialName("searchParams") val searchParams: Map<String, JsonElement>,
    @SerialName("searchQuery") val searchQuery: String? = null,
    @SerialName("isSaved") val isSaved: Boolean = false,
    @SerialName("savedName") val savedName: String? = null,
    @SerialName("searchCount") val searchCount: Int = 1,
    @SerialName("lastSearchedAt") val lastSearchedAt: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String
)

@Serializable
data class CreateSearchHistoryRequest(
    @SerialName("searchType") val searchType: String,
    @SerialName("searchParams") val searchParams: Map<String, JsonElement>,
    @SerialName("searchQuery") val searchQuery: String? = null,
    @SerialName("isSaved") val isSaved: Boolean = false,
    @SerialName("savedName") val savedName: String? = null
)

@Serializable
data class SaveSearchRequest(
    @SerialName("savedName") val savedName: String
)

@Serializable
data class SearchStatsResponse(
    @SerialName("totalSearches") val totalSearches: Int,
    @SerialName("savedSearches") val savedSearches: Int,
    @SerialName("searchesByType") val searchesByType: Map<String, Int>,
    @SerialName("mostSearchedDestination") val mostSearchedDestination: String? = null
)

