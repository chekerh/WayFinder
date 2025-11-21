package tn.esprit.wayfinder.presentation.booking

import tn.esprit.wayfinder.models.Booking
import tn.esprit.wayfinder.models.ConfirmBookingRequest
import tn.esprit.wayfinder.models.CreateBookingRequest
import tn.esprit.wayfinder.models.OfferComparison
import tn.esprit.wayfinder.models.TripDetails
import tn.esprit.wayfinder.models.UpdateBookingRequest
import tn.esprit.wayfinder.network.ApiService

class BookingRepository(private val apiService: ApiService) {
    
    suspend fun getBookingHistory(): List<Booking> {
        return apiService.getBookingHistory()
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
        return apiService.confirmBooking(request)
    }
    
    suspend fun getOfferComparison(offerId: String): OfferComparison {
        return apiService.getOfferComparison(offerId)
    }

    suspend fun createBooking(request: CreateBookingRequest): Booking {
        return apiService.createBooking(request)
    }

    suspend fun updateBooking(bookingId: String, request: UpdateBookingRequest): Booking {
        return apiService.updateBooking(bookingId, request)
    }

    suspend fun cancelBooking(bookingId: String): Booking {
        return apiService.cancelBooking(bookingId)
    }

    suspend fun getBooking(bookingId: String): Booking {
        return apiService.getBookingById(bookingId)
    }
}

