package tn.esprit.wayfinder.presentation.itinerary

import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.network.ApiService

class ItineraryRepository(private val apiService: ApiService) {

    suspend fun getItineraries(includePublic: Boolean = false): List<Itinerary> {
        return apiService.getItineraries(includePublic)
    }

    suspend fun getItinerary(id: String): Itinerary {
        return apiService.getItinerary(id)
    }

    suspend fun createItinerary(request: CreateItineraryRequest): Itinerary {
        return apiService.createItinerary(request)
    }

    suspend fun updateItinerary(id: String, request: UpdateItineraryRequest): Itinerary {
        return apiService.updateItinerary(id, request)
    }

    suspend fun deleteItinerary(id: String): Map<String, String> {
        return apiService.deleteItinerary(id)
    }

    suspend fun addActivity(
        itineraryId: String,
        dayDate: String,
        activity: AddActivityRequest
    ): Itinerary {
        return apiService.addActivity(itineraryId, dayDate, activity)
    }

    suspend fun removeActivity(
        itineraryId: String,
        dayDate: String,
        activityIndex: Int
    ): Itinerary {
        return apiService.removeActivity(itineraryId, dayDate, activityIndex)
    }
}

