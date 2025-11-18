package tn.esprit.wayfinder.presentation.traveltips

import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.network.ApiService

class TravelTipsRepository(private val apiService: ApiService) {

    suspend fun getTravelTips(destinationId: String, category: String? = null, limit: Int = 10): List<TravelTip> {
        return apiService.getTravelTips(destinationId, category, limit)
    }

    suspend fun generateTravelTips(
        destinationId: String,
        destinationName: String,
        city: String? = null,
        country: String? = null
    ): List<TravelTip> {
        return apiService.generateTravelTips(destinationId, destinationName, city, country)
    }

    suspend fun createTravelTip(request: CreateTravelTipRequest): TravelTip {
        return apiService.createTravelTip(request)
    }

    suspend fun markTipHelpful(tipId: String): TravelTip {
        return apiService.markTipHelpful(tipId)
    }

    suspend fun getTravelTipById(tipId: String): TravelTip {
        return apiService.getTravelTipById(tipId)
    }
}

