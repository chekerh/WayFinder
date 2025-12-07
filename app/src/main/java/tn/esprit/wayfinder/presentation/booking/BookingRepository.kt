package tn.esprit.wayfinder.presentation.booking

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tn.esprit.wayfinder.manager.CacheManager
import tn.esprit.wayfinder.models.Booking
import tn.esprit.wayfinder.models.ConfirmBookingRequest
import tn.esprit.wayfinder.models.CreateBookingRequest
import tn.esprit.wayfinder.models.OfferComparison
import tn.esprit.wayfinder.models.TripDetails
import tn.esprit.wayfinder.models.UpdateBookingRequest
import tn.esprit.wayfinder.network.ApiService

class BookingRepository(
    private val apiService: ApiService,
    private val context: Context? = null
) {
    
    private val cacheManager = context?.let { CacheManager(it) }
    private val TAG = "BookingRepository"
    
    suspend fun getBookingHistory(): List<Booking> = withContext(Dispatchers.IO) {
        // Try cache first
        cacheManager?.get<List<Booking>>(CacheManager.KEY_BOOKING_HISTORY)?.let { cached ->
            Log.d(TAG, "Returning cached booking history (${cached.size} bookings)")
            // Refresh in background
            refreshBookingHistoryInBackground()
            return@withContext cached
        }
        
        // Cache miss - fetch from API
        try {
            val response = apiService.getBookingHistory(page = 1, limit = 100)
            val bookings = response.data
            cacheManager?.put(CacheManager.KEY_BOOKING_HISTORY, bookings, CacheManager.TTL_MEDIUM)
            Log.d(TAG, "Fetched and cached ${bookings.size} bookings from API")
            bookings
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching booking history from API", e)
            // Return empty list on error
            emptyList()
        }
    }
    
    suspend fun confirmBooking(
        offerId: String,
        paymentDetails: Map<String, String>,
        totalPrice: Double? = null,
        tripDetails: TripDetails? = null
    ): Booking {
        val request = ConfirmBookingRequest(
            offerId = offerId,
            paymentDetails = paymentDetails,
            totalPrice = totalPrice,
            tripDetails = tripDetails
        )
        val booking = apiService.confirmBooking(request)
        // Invalidate booking history cache
        cacheManager?.remove(CacheManager.KEY_BOOKING_HISTORY)
        return booking
    }
    
    suspend fun getOfferComparison(offerId: String): OfferComparison {
        val cacheKey = "cache_offer_comparison_$offerId"
        // Try cache first
        cacheManager?.get<OfferComparison>(cacheKey)?.let { cached ->
            Log.d(TAG, "Returning cached offer comparison for: $offerId")
            return cached
        }
        
        // Cache miss - fetch from API
        val comparison = apiService.getOfferComparison(offerId)
        cacheManager?.put(cacheKey, comparison, CacheManager.TTL_MEDIUM)
        return comparison
    }

    suspend fun createBooking(request: CreateBookingRequest): Booking {
        val booking = apiService.createBooking(request)
        // Invalidate booking history cache
        cacheManager?.remove(CacheManager.KEY_BOOKING_HISTORY)
        return booking
    }

    suspend fun updateBooking(bookingId: String, request: UpdateBookingRequest): Booking {
        val booking = apiService.updateBooking(bookingId, request)
        // Invalidate booking history cache
        cacheManager?.remove(CacheManager.KEY_BOOKING_HISTORY)
        return booking
    }

    suspend fun cancelBooking(bookingId: String): Booking {
        val booking = apiService.cancelBooking(bookingId)
        // Invalidate booking history cache
        cacheManager?.remove(CacheManager.KEY_BOOKING_HISTORY)
        return booking
    }

    suspend fun getBooking(bookingId: String): Booking {
        val cacheKey = "cache_booking_$bookingId"
        // Try cache first
        cacheManager?.get<Booking>(cacheKey)?.let { cached ->
            Log.d(TAG, "Returning cached booking: $bookingId")
            return cached
        }
        
        // Cache miss - fetch from API
        val booking = apiService.getBookingById(bookingId)
        cacheManager?.put(cacheKey, booking, CacheManager.TTL_MEDIUM)
        return booking
    }
    
    private suspend fun refreshBookingHistoryInBackground() {
        try {
            val response = apiService.getBookingHistory(page = 1, limit = 100)
            cacheManager?.put(CacheManager.KEY_BOOKING_HISTORY, response.data, CacheManager.TTL_MEDIUM)
            Log.d(TAG, "Background refresh: updated booking history (${response.data.size} bookings)")
        } catch (e: Exception) {
            Log.w(TAG, "Background refresh failed for booking history", e)
        }
    }
}

