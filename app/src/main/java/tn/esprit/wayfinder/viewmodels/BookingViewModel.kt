package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.models.Booking
import tn.esprit.wayfinder.models.OfferComparison
import tn.esprit.wayfinder.presentation.booking.BookingRepository

sealed class BookingUiState {
    object Idle : BookingUiState()
    object Loading : BookingUiState()
    data class Success(val bookings: List<Booking>) : BookingUiState()
    data class Error(val message: String) : BookingUiState()
}

sealed class ReservationUiState {
    object Idle : ReservationUiState()
    object Loading : ReservationUiState()
    data class Success(val booking: Booking) : ReservationUiState()
    data class Error(val message: String) : ReservationUiState()
}

sealed class OfferComparisonUiState {
    object Idle : OfferComparisonUiState()
    object Loading : OfferComparisonUiState()
    data class Success(val comparison: OfferComparison) : OfferComparisonUiState()
    data class Error(val message: String) : OfferComparisonUiState()
}

class BookingViewModel(private val bookingRepository: BookingRepository) : ViewModel() {
    
    private val _bookingHistoryState = MutableStateFlow<BookingUiState>(BookingUiState.Idle)
    val bookingHistoryState: StateFlow<BookingUiState> = _bookingHistoryState.asStateFlow()
    
    private val _reservationState = MutableStateFlow<ReservationUiState>(ReservationUiState.Idle)
    val reservationState: StateFlow<ReservationUiState> = _reservationState.asStateFlow()

    private val _offerComparisonState = MutableStateFlow<OfferComparisonUiState>(OfferComparisonUiState.Idle)
    val offerComparisonState: StateFlow<OfferComparisonUiState> = _offerComparisonState.asStateFlow()
    
    fun loadBookingHistory() {
        viewModelScope.launch {
            try {
                _bookingHistoryState.value = BookingUiState.Loading
                val bookings = bookingRepository.getBookingHistory()
                _bookingHistoryState.value = BookingUiState.Success(bookings)
            } catch (e: Exception) {
                _bookingHistoryState.value = BookingUiState.Error(
                    e.message ?: "Failed to load booking history"
                )
            }
        }
    }
    
    fun confirmBooking(
        offerId: String,
        paymentMethod: String = "credit_card",
        cardNumber: String? = null,
        cardHolderName: String? = null
    ) {
        viewModelScope.launch {
            try {
                _reservationState.value = ReservationUiState.Loading
                val paymentDetails = mapOf(
                    "method" to paymentMethod,
                    "card_number" to (cardNumber ?: ""),
                    "card_holder" to (cardHolderName ?: "")
                )
                val booking = bookingRepository.confirmBooking(offerId, paymentDetails)
                _reservationState.value = ReservationUiState.Success(booking)
            } catch (e: Exception) {
                _reservationState.value = ReservationUiState.Error(
                    e.message ?: "Failed to confirm booking"
                )
            }
        }
    }

    fun loadOfferComparison(offerId: String) {
        viewModelScope.launch {
            try {
                _offerComparisonState.value = OfferComparisonUiState.Loading
                val comparison = bookingRepository.getOfferComparison(offerId)
                _offerComparisonState.value = OfferComparisonUiState.Success(comparison)
            } catch (e: Exception) {
                _offerComparisonState.value = OfferComparisonUiState.Error(
                    e.message ?: "Failed to load price breakdown"
                )
            }
        }
    }

    fun resetReservationState() {
        _reservationState.value = ReservationUiState.Idle
    }
}

