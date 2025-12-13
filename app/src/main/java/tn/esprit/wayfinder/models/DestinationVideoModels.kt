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

// --- Music Track Models --- //

@Serializable
data class MusicTrack(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("genre") val genre: String,
    @SerialName("duration") val duration: String,
    @SerialName("previewUrl") val previewUrl: String? = null
)

@Serializable
data class MusicTracksResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("tracks") val tracks: List<MusicTrack> = emptyList()
)

// --- Travel Plan Models --- //

@Serializable
data class TravelPlanSuggestion(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("destinations") val destinations: List<String> = emptyList(),
    @SerialName("duration") val duration: String,
    @SerialName("activities") val activities: List<String> = emptyList(),
    @SerialName("videoPrompt") val videoPrompt: String
)

@Serializable
data class TravelPlansResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("plans") val plans: List<TravelPlanSuggestion> = emptyList()
)

// --- Generate with Media --- //

@Serializable
data class AiVideoGenerateWithMediaRequest(
    @SerialName("prompt") val prompt: String,
    @SerialName("images") val images: List<String> = emptyList(),
    @SerialName("musicTrackId") val musicTrackId: String? = null
)

@Serializable
data class AiVideoGenerateWithMediaData(
    @SerialName("predictionId") val predictionId: String,
    @SerialName("status") val status: String,
    @SerialName("originalPrompt") val originalPrompt: String,
    @SerialName("enhancedPrompt") val enhancedPrompt: String,
    @SerialName("musicTrack") val musicTrack: MusicTrack? = null,
    @SerialName("estimatedTime") val estimatedTime: String? = null
)

@Serializable
data class AiVideoGenerateWithMediaResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: AiVideoGenerateWithMediaData? = null
)

// --- Image Upload Models --- //

@Serializable
data class ImageUploadResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: ImageUploadData? = null
)

@Serializable
data class ImageUploadData(
    @SerialName("url") val url: String,
    @SerialName("originalName") val originalName: String,
    @SerialName("size") val size: Long? = null
)

@Serializable
data class ImagesUploadResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: ImagesUploadData? = null
)

@Serializable
data class ImagesUploadData(
    @SerialName("images") val images: List<ImageUploadData> = emptyList(),
    @SerialName("count") val count: Int = 0
)

