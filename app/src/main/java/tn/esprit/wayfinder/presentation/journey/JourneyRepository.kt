package tn.esprit.wayfinder.presentation.journey

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.network.ApiService

class JourneyRepository(private val apiService: ApiService) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val textMediaType = "text/plain".toMediaTypeOrNull()
    private val jsonMediaType = "application/json".toMediaTypeOrNull()

    suspend fun createJourney(
        images: List<MultipartBody.Part>,
        bookingId: String? = null,
        destination: String? = null,
        description: String? = null,
        tags: List<String>? = null,
        isPublic: Boolean? = null
    ): Journey {
        fun stringBody(value: String?): RequestBody? =
            value?.takeIf { it.isNotBlank() }?.toRequestBody(textMediaType)

        val bookingIdBody = stringBody(bookingId)
        val destinationBody = stringBody(destination)
        val descriptionBody = stringBody(description)
        val tagsBody = tags
            ?.takeIf { it.isNotEmpty() }
            ?.let { json.encodeToString(it).toRequestBody(jsonMediaType) }
        val isPublicBody = isPublic?.toString()?.toRequestBody(textMediaType)

        return apiService.createJourney(
            images = images,
            bookingId = bookingIdBody,
            destination = destinationBody,
            description = descriptionBody,
            tags = tagsBody,
            isPublic = isPublicBody
        )
    }
    
    suspend fun getJourneys(limit: Int? = null, skip: Int? = null): List<Journey> {
        return apiService.getJourneys(limit, skip)
    }
    
    suspend fun getMyJourneys(limit: Int? = null, skip: Int? = null): List<Journey> {
        return apiService.getMyJourneys(limit, skip)
    }
    
    suspend fun canShareJourney(): CanShareJourneyResponse {
        return apiService.canShareJourney()
    }
    
    suspend fun getJourneyById(id: String): Journey {
        return apiService.getJourneyById(id)
    }
    
    suspend fun updateJourney(id: String, request: UpdateJourneyRequest): Journey {
        return apiService.updateJourney(id, request)
    }
    
    suspend fun deleteJourney(id: String) {
        apiService.deleteJourney(id)
    }
    
    suspend fun likeJourney(id: String): JourneyLikeResponse {
        return apiService.likeJourney(id)
    }
    
    suspend fun addComment(journeyId: String, content: String, parentCommentId: String? = null): JourneyComment {
        val request = CreateJourneyCommentRequest(content, parentCommentId)
        return apiService.addJourneyComment(journeyId, request)
    }
    
    suspend fun getComments(journeyId: String, limit: Int? = null, skip: Int? = null): List<JourneyComment> {
        return apiService.getJourneyComments(journeyId, limit, skip)
    }
    
    suspend fun deleteComment(commentId: String) {
        apiService.deleteJourneyComment(commentId)
    }
    
    suspend fun regenerateVideo(journeyId: String): Map<String, String> {
        return apiService.regenerateJourneyVideo(journeyId)
    }
}

