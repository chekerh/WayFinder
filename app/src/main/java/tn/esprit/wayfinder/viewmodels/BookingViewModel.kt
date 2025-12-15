package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import tn.esprit.wayfinder.models.Booking
import tn.esprit.wayfinder.models.OfferComparison
import tn.esprit.wayfinder.models.TripDetails
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
                android.util.Log.d("BookingViewModel", "Loading booking history...")
                val bookings = bookingRepository.getBookingHistory()
                android.util.Log.d("BookingViewModel", "Loaded ${bookings.size} bookings")
                bookings.forEachIndexed { index, booking ->
                    android.util.Log.d("BookingViewModel", "Booking $index: id=${booking.id}, status=${booking.status}, destination=${booking.tripDetails?.destination ?: "null"}")
                }
                _bookingHistoryState.value = BookingUiState.Success(bookings)
            } catch (e: Exception) {
                val errorMessage = parseError(e)
                android.util.Log.e("BookingViewModel", "Error loading booking history: $errorMessage", e)
                _bookingHistoryState.value = BookingUiState.Error(errorMessage)
            }
        }
    }
    
    fun confirmBooking(
        offerId: String,
        paymentMethod: String = "credit_card",
        cardNumber: String? = null,
        cardHolderName: String? = null,
        totalPrice: Double,
        destination: String? = null, // Destination name (e.g., "Paris, France")
        destinationCountry: String? = null, // Destination country (e.g., "France")
        accommodationId: String? = null,
        accommodationName: String? = null,
        accommodationPrice: Double? = null,
        accommodationCurrency: String? = null,
        checkInDate: String? = null,
        checkOutDate: String? = null
    ) {
        // Prevent multiple simultaneous requests
        if (_reservationState.value is ReservationUiState.Loading) {
            android.util.Log.w("BookingViewModel", "Booking confirmation already in progress, ignoring duplicate request")
            return
        }
        
        viewModelScope.launch {
            try {
                _reservationState.value = ReservationUiState.Loading
                
                // Validate offerId before making API call
                if (offerId.isBlank()) {
                    _reservationState.value = ReservationUiState.Error("Offer ID is required")
                    return@launch
                }
                
                // Ensure payment_details is never empty and contains at least one non-empty value
                // Backend requires @IsNotEmpty() and @IsObject() on payment_details
                val paymentDetails = mutableMapOf<String, String>()
                val cleanCardNumber = cardNumber?.replace(" ", "")?.trim() ?: ""
                val cleanCardHolder = cardHolderName?.trim() ?: ""
                
                // Always include method (required by backend validation)
                paymentDetails["method"] = paymentMethod.ifEmpty { "credit_card" }
                
                // Only add card_number and card_holder if they have values
                if (cleanCardNumber.isNotEmpty()) {
                    paymentDetails["card_number"] = cleanCardNumber
                }
                if (cleanCardHolder.isNotEmpty()) {
                    paymentDetails["card_holder"] = cleanCardHolder
                }
                
                // Ensure payment_details is not empty (backend @IsNotEmpty() validation)
                if (paymentDetails.isEmpty()) {
                    _reservationState.value = ReservationUiState.Error("Payment details are required")
                    return@launch
                }
                
                // Create trip_details if destination is provided
                // Include check-in/check-out dates for accommodation bookings
                val tripDetails = if (destination != null && destination.isNotBlank()) {
                    val destinationName = if (destinationCountry != null && destinationCountry.isNotBlank()) {
                        "$destination, $destinationCountry"
                    } else {
                        destination
                    }
                    TripDetails(
                        destination = destinationName,
                        departureDate = checkInDate, // Use check-in date as departure_date for accommodation
                        returnDate = checkOutDate // Use check-out date as return_date for accommodation
                    )
                } else {
                    null
                }
                
                // Create accommodation request if accommodation data is provided
                val accommodationRequest = if (accommodationId != null && accommodationName != null && accommodationPrice != null && accommodationCurrency != null) {
                    tn.esprit.wayfinder.models.AccommodationRequest(
                        id = accommodationId,
                        name = accommodationName,
                        price = accommodationPrice,
                        currency = accommodationCurrency
                    )
                } else {
                    null
                }
                
                android.util.Log.d("BookingViewModel", "Confirming booking: offerId=$offerId, totalPrice=$totalPrice, destination=$destination, tripDetails=$tripDetails, accommodation=$accommodationRequest")
                
                val booking = bookingRepository.confirmBooking(offerId, paymentDetails, totalPrice, tripDetails, accommodationRequest)
                _reservationState.value = ReservationUiState.Success(booking)
            } catch (e: Exception) {
                val errorMessage = parseError(e)
                android.util.Log.e("BookingViewModel", "Error confirming booking: $errorMessage", e)
                _reservationState.value = ReservationUiState.Error(errorMessage)
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

    // Single booking state
    private val _singleBookingState = MutableStateFlow<ReservationUiState>(ReservationUiState.Idle)
    val singleBookingState: StateFlow<ReservationUiState> = _singleBookingState.asStateFlow()

    fun loadBooking(bookingId: String) {
        viewModelScope.launch {
            try {
                _singleBookingState.value = ReservationUiState.Loading
                val booking = bookingRepository.getBooking(bookingId)
                _singleBookingState.value = ReservationUiState.Success(booking)
            } catch (e: Exception) {
                _singleBookingState.value = ReservationUiState.Error(
                    e.message ?: "Failed to load booking"
                )
            }
        }
    }

    fun createBooking(
        offerId: String,
        tripDetails: tn.esprit.wayfinder.models.TripDetails? = null,
        passengers: List<tn.esprit.wayfinder.models.BookingPassenger>? = null,
        notes: String? = null,
        totalPrice: Double? = null,
        paymentDetails: Map<String, String> = emptyMap()
    ) {
        viewModelScope.launch {
            try {
                _reservationState.value = ReservationUiState.Loading
                val request = tn.esprit.wayfinder.models.CreateBookingRequest(
                    offerId = offerId,
                    paymentDetails = paymentDetails,
                    tripDetails = tripDetails,
                    passengers = passengers,
                    notes = notes,
                    totalPrice = totalPrice
                )
                val booking = bookingRepository.createBooking(request)
                _reservationState.value = ReservationUiState.Success(booking)
            } catch (e: Exception) {
                _reservationState.value = ReservationUiState.Error(
                    e.message ?: "Failed to create booking"
                )
            }
        }
    }

    fun updateBooking(
        bookingId: String,
        tripDetails: tn.esprit.wayfinder.models.TripDetails? = null,
        passengers: List<tn.esprit.wayfinder.models.BookingPassenger>? = null,
        notes: String? = null,
        paymentDetails: Map<String, String>? = null,
        totalPrice: Double? = null
    ) {
        viewModelScope.launch {
            try {
                _singleBookingState.value = ReservationUiState.Loading
                val request = tn.esprit.wayfinder.models.UpdateBookingRequest(
                    paymentDetails = paymentDetails,
                    tripDetails = tripDetails,
                    passengers = passengers,
                    notes = notes,
                    totalPrice = totalPrice
                )
                val booking = bookingRepository.updateBooking(bookingId, request)
                _singleBookingState.value = ReservationUiState.Success(booking)
                // Refresh booking history
                loadBookingHistory()
            } catch (e: Exception) {
                _singleBookingState.value = ReservationUiState.Error(
                    e.message ?: "Failed to update booking"
                )
            }
        }
    }

    fun cancelBooking(bookingId: String) {
        viewModelScope.launch {
            try {
                _singleBookingState.value = ReservationUiState.Loading
                val booking = bookingRepository.cancelBooking(bookingId)
                _singleBookingState.value = ReservationUiState.Success(booking)
                // Refresh booking history
                loadBookingHistory()
            } catch (e: Exception) {
                _singleBookingState.value = ReservationUiState.Error(
                    parseError(e)
                )
            }
        }
    }

    fun deleteBooking(bookingId: String) {
        viewModelScope.launch {
            try {
                _singleBookingState.value = ReservationUiState.Loading
                bookingRepository.deleteBooking(bookingId)
                // After permanent delete, refresh booking history so the item disappears
                loadBookingHistory()
                // Reset single booking state since this booking no longer exists
                _singleBookingState.value = ReservationUiState.Idle
            } catch (e: Exception) {
                _singleBookingState.value = ReservationUiState.Error(
                    parseError(e)
                )
            }
        }
    }

    private fun parseError(throwable: Throwable): String {
        return when (throwable) {
            is HttpException -> {
                // Handle rate limiting (429) with user-friendly message
                if (throwable.code() == 429) {
                    return "Trop de requêtes. Veuillez patienter quelques instants avant de réessayer."
                }
                
                val errorBody = throwable.response()?.errorBody()?.string()
                if (!errorBody.isNullOrBlank()) {
                    val parsedMessage = try {
                        val element = Json.parseToJsonElement(errorBody)
                        // Try to get message from error response
                        element.jsonObject["message"]?.jsonPrimitive?.content
                            ?: element.jsonObject["error"]?.jsonPrimitive?.content
                            ?: element.jsonObject.toString()
                    } catch (_: Exception) {
                        errorBody
                    }
                    parsedMessage ?: throwable.message()
                } else {
                    "HTTP ${throwable.code()}: ${throwable.message()}"
                }
            }
            is java.net.UnknownHostException -> "Impossible de se connecter au serveur. Vérifiez votre connexion Internet."
            is java.net.SocketTimeoutException -> "Le serveur met trop de temps à répondre. Réessayez dans un instant."
            else -> throwable.message ?: "Une erreur inattendue est survenue"
        }
    }
}

