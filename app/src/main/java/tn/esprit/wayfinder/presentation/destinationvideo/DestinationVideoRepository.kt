package tn.esprit.wayfinder.presentation.destinationvideo

import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.network.ApiService

class DestinationVideoRepository(private val apiService: ApiService) {

    suspend fun generateDestinationVideo(userId: String, destination: String): GenerateVideoResponse {
        return apiService.generateDestinationVideo(userId, destination)
    }

    suspend fun getDestinationVideoStatus(userId: String, destination: String): DestinationVideoStatus {
        return apiService.getDestinationVideoStatus(userId, destination)
    }

    suspend fun getUserDestinations(userId: String): UserDestinationsResponse {
        return apiService.getUserDestinations(userId)
    }
}

