package tn.esprit.wayfinder.presentation.catalog

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tn.esprit.wayfinder.manager.CacheManager
import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.network.ApiService

class CatalogRepository(
    private val apiService: ApiService,
    private val context: Context? = null
) {
    
    private val cacheManager = context?.let { CacheManager(it) }
    private val TAG = "CatalogRepository"

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
    ): RecommendedFlightsResponse = withContext(Dispatchers.IO) {
        // Create cache key from parameters
        val cacheKey = "${CacheManager.KEY_RECOMMENDED_FLIGHTS}_${originLocationCode}_${destinationLocationCode}_${departureDate}_${returnDate}_${adults}_${travelClass}_${currencyCode}_${maxResults}_${maxPrice}"
        
        // Try cache first
        cacheManager?.get<RecommendedFlightsResponse>(cacheKey)?.let { cached ->
            Log.d(TAG, "Returning cached recommended flights")
            // Refresh in background
            refreshRecommendedFlightsInBackground(
                originLocationCode, destinationLocationCode, departureDate,
                returnDate, adults, travelClass, currencyCode, maxResults, maxPrice, cacheKey
            )
            return@withContext cached
        }
        
        // Cache miss - fetch from API
        try {
            val response = apiService.getRecommendedFlights(
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
            cacheManager?.put(cacheKey, response, CacheManager.TTL_MEDIUM)
            Log.d(TAG, "Fetched and cached recommended flights from API")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching recommended flights from API", e)
            throw e
        }
    }

    suspend fun getExploreOffers(
        origin: String? = null,
        destination: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        budget: Int? = null,
        limit: Int? = null
    ): ExploreOffersResponse = withContext(Dispatchers.IO) {
        // Create cache key from parameters
        val cacheKey = "${CacheManager.KEY_EXPLORE_OFFERS}_${origin}_${destination}_${dateFrom}_${dateTo}_${budget}_${limit}"
        
        // Try cache first
        cacheManager?.get<ExploreOffersResponse>(cacheKey)?.let { cached ->
            Log.d(TAG, "Returning cached explore offers")
            // Refresh in background
            refreshExploreOffersInBackground(origin, destination, dateFrom, dateTo, budget, limit, cacheKey)
            return@withContext cached
        }
        
        // Cache miss - fetch from API
        try {
            val response = apiService.getExploreOffers(origin, destination, dateFrom, dateTo, budget, limit)
            cacheManager?.put(cacheKey, response, CacheManager.TTL_MEDIUM)
            Log.d(TAG, "Fetched and cached explore offers from API")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching explore offers from API", e)
            throw e
        }
    }

    suspend fun getActivities(
        city: String? = null,
        themes: String? = null,
        limit: Int? = null,
        radiusMeters: Int? = null
    ): ActivityFeedResponse = withContext(Dispatchers.IO) {
        // Create cache key from parameters
        val cacheKey = "${CacheManager.KEY_ACTIVITIES}_${city}_${themes}_${limit}_${radiusMeters}"
        
        // Try cache first
        cacheManager?.get<ActivityFeedResponse>(cacheKey)?.let { cached ->
            Log.d(TAG, "Returning cached activities")
            // Refresh in background
            refreshActivitiesInBackground(city, themes, limit, radiusMeters, cacheKey)
            return@withContext cached
        }
        
        // Cache miss - fetch from API
        try {
            val response = apiService.getActivities(city, themes, limit, radiusMeters)
            cacheManager?.put(cacheKey, response, CacheManager.TTL_LONG) // Activities change less frequently
            Log.d(TAG, "Fetched and cached activities from API")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching activities from API", e)
            throw e
        }
    }
    
    private suspend fun refreshRecommendedFlightsInBackground(
        originLocationCode: String?,
        destinationLocationCode: String?,
        departureDate: String?,
        returnDate: String?,
        adults: Int?,
        travelClass: String?,
        currencyCode: String?,
        maxResults: Int?,
        maxPrice: Double?,
        cacheKey: String
    ) {
        try {
            val response = apiService.getRecommendedFlights(
                originLocationCode, destinationLocationCode, departureDate,
                returnDate, adults, travelClass, currencyCode, maxResults, maxPrice
            )
            cacheManager?.put(cacheKey, response, CacheManager.TTL_MEDIUM)
            Log.d(TAG, "Background refresh: updated recommended flights")
        } catch (e: Exception) {
            Log.w(TAG, "Background refresh failed for recommended flights", e)
        }
    }
    
    private suspend fun refreshExploreOffersInBackground(
        origin: String?,
        destination: String?,
        dateFrom: String?,
        dateTo: String?,
        budget: Int?,
        limit: Int?,
        cacheKey: String
    ) {
        try {
            val response = apiService.getExploreOffers(origin, destination, dateFrom, dateTo, budget, limit)
            cacheManager?.put(cacheKey, response, CacheManager.TTL_MEDIUM)
            Log.d(TAG, "Background refresh: updated explore offers")
        } catch (e: Exception) {
            Log.w(TAG, "Background refresh failed for explore offers", e)
        }
    }
    
    private suspend fun refreshActivitiesInBackground(
        city: String?,
        themes: String?,
        limit: Int?,
        radiusMeters: Int?,
        cacheKey: String
    ) {
        try {
            val response = apiService.getActivities(city, themes, limit, radiusMeters)
            cacheManager?.put(cacheKey, response, CacheManager.TTL_LONG)
            Log.d(TAG, "Background refresh: updated activities")
        } catch (e: Exception) {
            Log.w(TAG, "Background refresh failed for activities", e)
        }
    }

    // ============ HOTELS ============

    suspend fun searchHotels(
        cityCode: String,
        checkInDate: String,
        checkOutDate: String,
        adults: Int? = null,
        tripType: String? = null,
        ratings: String? = null,
        limit: Int? = null,
        currency: String? = null
    ): HotelSearchResponse = withContext(Dispatchers.IO) {
        val cacheKey = "hotels_${cityCode}_${checkInDate}_${checkOutDate}_${tripType}_${ratings}_${limit}"
        
        // Try cache first
        cacheManager?.get<HotelSearchResponse>(cacheKey)?.let { cached ->
            Log.d(TAG, "Returning cached hotels for $cityCode")
            return@withContext cached
        }
        
        try {
            val response = apiService.searchHotels(
                cityCode, checkInDate, checkOutDate, adults, tripType, ratings, limit, currency
            )
            cacheManager?.put(cacheKey, response, CacheManager.TTL_MEDIUM)
            Log.d(TAG, "Fetched and cached hotels from API for $cityCode")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching hotels from API", e)
            throw e
        }
    }

    suspend fun getHotelOffers(
        hotelIds: List<String>,
        checkInDate: String,
        checkOutDate: String,
        adults: Int? = null,
        currency: String? = null
    ): HotelOffersResponse = withContext(Dispatchers.IO) {
        val hotelIdsStr = hotelIds.joinToString(",")
        val cacheKey = "hotel_offers_${hotelIdsStr}_${checkInDate}_${checkOutDate}"
        
        cacheManager?.get<HotelOffersResponse>(cacheKey)?.let { cached ->
            Log.d(TAG, "Returning cached hotel offers")
            return@withContext cached
        }
        
        try {
            val response = apiService.getHotelOffers(hotelIdsStr, checkInDate, checkOutDate, adults, currency)
            cacheManager?.put(cacheKey, response, CacheManager.TTL_SHORT) // Prices change often
            Log.d(TAG, "Fetched and cached hotel offers from API")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching hotel offers from API", e)
            throw e
        }
    }

    suspend fun getHotelById(hotelId: String): HotelDetailResponse = withContext(Dispatchers.IO) {
        val cacheKey = "hotel_detail_$hotelId"
        
        cacheManager?.get<HotelDetailResponse>(cacheKey)?.let { cached ->
            Log.d(TAG, "Returning cached hotel detail for $hotelId")
            return@withContext cached
        }
        
        try {
            val response = apiService.getHotelById(hotelId)
            cacheManager?.put(cacheKey, response, CacheManager.TTL_LONG)
            Log.d(TAG, "Fetched and cached hotel detail from API")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching hotel detail from API", e)
            throw e
        }
    }

    suspend fun getHotelReviews(hotelId: String, placeId: String?): HotelReviewsResponse = withContext(Dispatchers.IO) {
        val cacheKey = "hotel_reviews_$hotelId"
        
        cacheManager?.get<HotelReviewsResponse>(cacheKey)?.let { cached ->
            Log.d(TAG, "Returning cached hotel reviews for $hotelId")
            return@withContext cached
        }
        
        try {
            val response = apiService.getHotelReviews(hotelId, placeId)
            cacheManager?.put(cacheKey, response, CacheManager.TTL_LONG)
            Log.d(TAG, "Fetched and cached hotel reviews from API")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching hotel reviews from API", e)
            throw e
        }
    }
}

