package tn.esprit.wayfinder.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.models.Hotel
import tn.esprit.wayfinder.models.HotelDetailResponse
import tn.esprit.wayfinder.models.HotelReview
import tn.esprit.wayfinder.presentation.catalog.CatalogRepository
import java.text.SimpleDateFormat
import java.util.*

sealed class HotelsUiState {
    object Idle : HotelsUiState()
    object Loading : HotelsUiState()
    data class Success(
        val hotels: List<Hotel>,
        val source: String = "api"
    ) : HotelsUiState()
    data class Error(val message: String) : HotelsUiState()
}

sealed class HotelDetailUiState {
    object Idle : HotelDetailUiState()
    object Loading : HotelDetailUiState()
    data class Success(
        val hotel: Hotel,
        val reviews: List<HotelReview> = emptyList()
    ) : HotelDetailUiState()
    data class Error(val message: String) : HotelDetailUiState()
}

class HotelsViewModel(
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val _hotelsState = MutableStateFlow<HotelsUiState>(HotelsUiState.Idle)
    val hotelsState: StateFlow<HotelsUiState> = _hotelsState.asStateFlow()

    private val _hotelDetailState = MutableStateFlow<HotelDetailUiState>(HotelDetailUiState.Idle)
    val hotelDetailState: StateFlow<HotelDetailUiState> = _hotelDetailState.asStateFlow()

    private val _selectedHotel = MutableStateFlow<Hotel?>(null)
    val selectedHotel: StateFlow<Hotel?> = _selectedHotel.asStateFlow()

    companion object {
        private const val TAG = "HotelsViewModel"
    }

    /**
     * Search for hotels in a city
     */
    fun searchHotels(
        cityCode: String? = null,
        cityName: String? = null,
        tripType: String? = null,
        accommodationType: String? = null,
        checkInDate: String? = null,
        checkOutDate: String? = null,
        adults: Int = 2,
        ratings: String? = null,
        limit: Int = 20,
        currency: String = "EUR"
    ) {
        viewModelScope.launch {
            _hotelsState.value = HotelsUiState.Loading
            
            try {
                // Generate default dates if not provided
                val defaultCheckIn = checkInDate ?: getDefaultCheckInDate()
                val defaultCheckOut = checkOutDate ?: getDefaultCheckOutDate()
                
                // If cityName is provided, use it; otherwise use cityCode
                val finalCityName = cityName ?: (if (cityCode?.length == 3) null else cityCode)
                val finalCityCode = if (cityName != null) null else cityCode
                
                Log.d(TAG, "Searching hotels: cityCode=$finalCityCode, cityName=$finalCityName, tripType=$tripType, checkIn=$defaultCheckIn, checkOut=$defaultCheckOut")
                
                val response = catalogRepository.searchHotels(
                    cityCode = finalCityCode,
                    cityName = finalCityName,
                    checkInDate = defaultCheckIn,
                    checkOutDate = defaultCheckOut,
                    adults = adults,
                    tripType = tripType,
                    accommodationType = accommodationType,
                    ratings = ratings,
                    limit = limit,
                    currency = currency
                )
                
                Log.d(TAG, "Found ${response.data.size} hotels")
                
                _hotelsState.value = HotelsUiState.Success(
                    hotels = response.data,
                    source = response.meta?.source ?: "api"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error searching hotels", e)
                _hotelsState.value = HotelsUiState.Error(
                    e.message ?: "Failed to load hotels"
                )
            }
        }
    }

    /**
     * Get hotel details with reviews
     */
    fun getHotelDetails(hotelId: String) {
        viewModelScope.launch {
            _hotelDetailState.value = HotelDetailUiState.Loading
            
            try {
                val response = catalogRepository.getHotelById(hotelId)
                
                if (response.hotel != null) {
                    _selectedHotel.value = response.hotel
                    _hotelDetailState.value = HotelDetailUiState.Success(
                        hotel = response.hotel,
                        reviews = response.reviews
                    )
                } else {
                    _hotelDetailState.value = HotelDetailUiState.Error(
                        response.error ?: "Hotel not found"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting hotel details", e)
                _hotelDetailState.value = HotelDetailUiState.Error(
                    e.message ?: "Failed to load hotel details"
                )
            }
        }
    }

    /**
     * Select a hotel for booking
     */
    fun selectHotel(hotel: Hotel) {
        _selectedHotel.value = hotel
    }

    /**
     * Clear selected hotel
     */
    fun clearSelectedHotel() {
        _selectedHotel.value = null
    }

    /**
     * Convert city name to IATA city code
     */
    fun getCityCode(destination: String?): String {
        if (destination.isNullOrBlank()) {
            return "PAR" // Default to Paris
        }
        
        // Common city mappings (expanded)
        val cityMappings = mapOf(
            "paris" to "PAR",
            "london" to "LON",
            "londres" to "LON",
            "rome" to "ROM",
            "roma" to "ROM",
            "barcelona" to "BCN",
            "barcelone" to "BCN",
            "dubai" to "DXB",
            "new york" to "NYC",
            "new york city" to "NYC",
            "tokyo" to "TYO",
            "amsterdam" to "AMS",
            "madrid" to "MAD",
            "berlin" to "BER",
            "munich" to "MUC",
            "vienna" to "VIE",
            "vienne" to "VIE",
            "lisbon" to "LIS",
            "lisbonne" to "LIS",
            "athens" to "ATH",
            "athènes" to "ATH",
            "istanbul" to "IST",
            "bangkok" to "BKK",
            "singapore" to "SIN",
            "singapour" to "SIN",
            "tunis" to "TUN",
            "tunisia" to "TUN",
            "cairo" to "CAI",
            "doha" to "DOH",
            "abu dhabi" to "AUH",
            "riyadh" to "RUH",
            "jeddah" to "JED",
            "mumbai" to "BOM",
            "delhi" to "DEL",
            "bangalore" to "BLR",
            "sydney" to "SYD",
            "melbourne" to "MEL",
            "toronto" to "YYZ",
            "montreal" to "YUL",
            "vancouver" to "YVR"
        )
        
        val normalized = destination.lowercase().trim()
        
        // Try exact match first
        cityMappings[normalized]?.let { return it }
        
        // Try partial match (contains)
        cityMappings.entries.firstOrNull { 
            normalized.contains(it.key) || it.key.contains(normalized)
        }?.value?.let { return it }
        
        // Fallback: use first 3 chars uppercase
        return normalized.take(3).uppercase().takeIf { it.length == 3 } ?: "PAR"
    }

    private fun getDefaultCheckInDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 14) // 2 weeks from now
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }

    private fun getDefaultCheckOutDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 17) // 2 weeks + 3 days
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }
}

