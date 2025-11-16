package tn.esprit.wayfinder.presentation.catalog

import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.network.ApiService

class CatalogRepository(private val apiService: ApiService) {

    suspend fun getRecommendedFlights(
        originLocationCode: String? = null,
        destinationLocationCode: String? = null,
        departureDate: String? = null,
        returnDate: String? = null,
        adults: Int? = null,
        travelClass: String? = null,
        currencyCode: String? = null,
        maxResults: Int? = null,
        maxPrice: Double? = null
    ): RecommendedFlightsResponse {
        return apiService.getRecommendedFlights(
            originLocationCode,
            destinationLocationCode,
            departureDate,
            returnDate,
            adults,
            travelClass,
            currencyCode,
            maxResults,
            maxPrice
        )
    }

    suspend fun getExploreOffers(
        origin: String? = null,
        destination: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        budget: Int? = null,
        limit: Int? = null
    ): ExploreOffersResponse {
        return apiService.getExploreOffers(origin, destination, dateFrom, dateTo, budget, limit)
    }

    suspend fun getActivities(
        city: String? = null,
        themes: String? = null,
        limit: Int? = null,
        radiusMeters: Int? = null
    ): kotlinx.serialization.json.JsonElement {
        return apiService.getActivities(city, themes, limit, radiusMeters)
    }
}

