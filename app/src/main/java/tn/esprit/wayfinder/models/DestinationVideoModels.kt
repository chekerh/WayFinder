package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DestinationVideoStatus(
    @SerialName("status") val status: String, // "not_started" | "processing" | "ready" | "failed"
    @SerialName("videoUrl") val videoUrl: String? = null,
    @SerialName("imageCount") val imageCount: Int = 0,
    @SerialName("generatedAt") val generatedAt: String? = null,
    @SerialName("errorMessage") val errorMessage: String? = null
)

@Serializable
data class GenerateVideoResponse(
    @SerialName("status") val status: String,
    @SerialName("userId") val userId: String,
    @SerialName("destination") val destination: String,
    @SerialName("imageCount") val imageCount: Int,
    @SerialName("message") val message: String
)

@Serializable
data class DestinationWithVideoStatus(
    @SerialName("destination") val destination: String,
    @SerialName("videoStatus") val videoStatus: String,
    @SerialName("videoUrl") val videoUrl: String? = null,
    @SerialName("imageCount") val imageCount: Int = 0,
    @SerialName("errorMessage") val errorMessage: String? = null
)

@Serializable
data class UserDestinationsResponse(
    @SerialName("destinations") val destinations: List<DestinationWithVideoStatus>
)

