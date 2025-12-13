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

// --- AI Travel Video Models --- //

@Serializable
data class AiVideoStatusResponse(
    @SerialName("available") val available: Boolean,
    @SerialName("suggestions") val suggestions: List<String> = emptyList(),
    @SerialName("message") val message: String? = null
)

@Serializable
data class AiVideoSuggestionsResponse(
    @SerialName("suggestions") val suggestions: List<String>
)

@Serializable
data class AiVideoGenerateRequest(
    @SerialName("prompt") val prompt: String
)

@Serializable
data class AiVideoGenerateResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: AiVideoGenerateData? = null
)

@Serializable
data class AiVideoGenerateData(
    @SerialName("predictionId") val predictionId: String,
    @SerialName("status") val status: String,
    @SerialName("originalPrompt") val originalPrompt: String,
    @SerialName("enhancedPrompt") val enhancedPrompt: String,
    @SerialName("estimatedTime") val estimatedTime: String? = null
)

@Serializable
data class AiVideoCheckStatusResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("data") val data: AiVideoStatusData? = null
)

@Serializable
data class AiVideoStatusData(
    @SerialName("predictionId") val predictionId: String,
    @SerialName("status") val status: String,
    @SerialName("videoUrl") val videoUrl: String? = null,
    @SerialName("progress") val progress: Int? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("isComplete") val isComplete: Boolean = false,
    @SerialName("isFailed") val isFailed: Boolean = false
)

@Serializable
data class GenericResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("message") val message: String? = null
)

