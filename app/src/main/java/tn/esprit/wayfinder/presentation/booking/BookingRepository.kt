package tn.esprit.wayfinder.presentation.booking

import tn.esprit.wayfinder.models.Booking
import tn.esprit.wayfinder.models.OfferComparison
import tn.esprit.wayfinder.network.ApiService

class BookingRepository(private val apiService: ApiService) {
    
    suspend fun getBookingHistory(): List<Booking> {
        return apiService.getBookingHistory()
    }
    
    suspend fun confirmBooking(
        offerId: String,
        paymentDetails: Map<String, Any>
    ): Booking {
        val request = mapOf(
            "offer_id" to offerId,
            "payment_details" to paymentDetails
        )
        return apiService.confirmBooking(request)
    }
    
    suspend fun getOfferComparison(offerId: String): OfferComparison {
        return apiService.getOfferComparison(offerId)
    }
}

