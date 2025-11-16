package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.models.ExploreOffer
import tn.esprit.wayfinder.models.ExploreOffersResponse
import tn.esprit.wayfinder.models.FlightDestination
import tn.esprit.wayfinder.models.FlightOffer
import tn.esprit.wayfinder.models.RecommendedFlightsResponse
import tn.esprit.wayfinder.presentation.catalog.CatalogRepository
import java.text.SimpleDateFormat
import java.util.*

sealed class CatalogUiState {
    object Idle : CatalogUiState()
    object Loading : CatalogUiState()
    data class Success(
        val destinations: List<FlightDestination>,
        val exploreOffers: List<ExploreOffer>? = null
    ) : CatalogUiState()
    data class Error(val message: String) : CatalogUiState()
}

class CatalogViewModel(private val catalogRepository: CatalogRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<CatalogUiState>(CatalogUiState.Idle)
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    fun loadRecommendedFlights() {
        viewModelScope.launch {
            try {
                _uiState.value = CatalogUiState.Loading

                val destinations = mutableListOf<FlightDestination>()
                var exploreOffers: List<ExploreOffer>? = null
                var networkError: Exception? = null

                // PRIORITY: Try to get explore offers first (Tequila API - returns multiple destinations)
                // This gives us variety in destinations
                try {
                    val exploreResponse = catalogRepository.getExploreOffers(
                        origin = "TUN",
                        limit = 20  // Get more to have variety
                    )
                    exploreOffers = exploreResponse.data
                    exploreResponse.data?.forEach { offer ->
                        val destination = convertExploreOfferToDestination(offer)
                        if (destination != null) {
                            destinations.add(destination)
                        }
                    }
                } catch (e: Exception) {
                    // Check for network connectivity issues
                    if (e is java.net.UnknownHostException || e.cause is java.net.UnknownHostException) {
                        networkError = e
                    }
                    // Tequila is optional - if it fails, we continue with Amadeus results only
                }

                // Get recommended flights from Amadeus (now returns multiple destinations from backend)
                // Add ALL flights - we want to show multiple flight options even to the same destination
                try {
                    val flightsResponse = catalogRepository.getRecommendedFlights(
                        maxResults = 15  // Request more flights
                    )
                    flightsResponse.data?.forEach { flight ->
                        val destination = convertFlightToDestination(flight)
                        if (destination != null) {
                            // Add all flights - don't filter by city, show multiple options
                            destinations.add(destination)
                        }
                    }
                } catch (e: Exception) {
                    // Check for network connectivity issues
                    if (e is java.net.UnknownHostException || e.cause is java.net.UnknownHostException) {
                        networkError = e
                    }
                    // Log but don't fail - we'll use explore results if available
                }

                // If we have a network error, show a specific message
                if (networkError != null) {
                    _uiState.value = CatalogUiState.Error(
                        "Unable to connect to server. Please check your internet connection. " +
                        "If using Render free tier, the service may be waking up (wait 30-60 seconds)."
                    )
                } else if (destinations.isEmpty()) {
                    _uiState.value = CatalogUiState.Error(
                        "No flights available. Please ensure Amadeus API keys are configured in the backend."
                    )
                } else {
                    // Remove duplicates by ID only (keep flights with same city but different IDs)
                    // This allows multiple flight options to the same destination
                    val uniqueDestinations = destinations.distinctBy { it.id }
                    
                    _uiState.value = CatalogUiState.Success(
                        destinations = uniqueDestinations,
                        exploreOffers = exploreOffers
                    )
                }
            } catch (e: Exception) {
                // Check for network connectivity issues
                val errorMessage = when {
                    e is java.net.UnknownHostException || e.cause is java.net.UnknownHostException -> {
                        "Unable to connect to server. Please check your internet connection. " +
                        "If using Render free tier, the service may be waking up (wait 30-60 seconds)."
                    }
                    e is java.net.SocketTimeoutException -> {
                        "Connection timeout. The server may be slow to respond. Please try again."
                    }
                    else -> {
                        e.message ?: "Failed to load flights. Please check backend API configuration."
                    }
                }
                _uiState.value = CatalogUiState.Error(errorMessage)
            }
        }
    }

    fun loadExploreOffers(origin: String = "TUN", limit: Int = 10) {
        viewModelScope.launch {
            try {
                _uiState.value = CatalogUiState.Loading
                val response = catalogRepository.getExploreOffers(
                    origin = origin,
                    limit = limit
                )

                val destinations = response.data?.mapNotNull { convertExploreOfferToDestination(it) } ?: emptyList()

                _uiState.value = CatalogUiState.Success(
                    destinations = destinations,
                    exploreOffers = response.data
                )
            } catch (e: Exception) {
                _uiState.value = CatalogUiState.Error(e.message ?: "Failed to load explore offers")
            }
        }
    }

    private fun convertFlightToDestination(flight: FlightOffer): FlightDestination? {
        val firstItinerary = flight.itineraries?.firstOrNull() ?: return null
        val firstSegment = firstItinerary.segments?.firstOrNull() ?: return null
        val lastSegment = firstItinerary.segments?.lastOrNull() ?: firstSegment

        val destinationCode = lastSegment.arrival?.iataCode ?: return null
        val cityName = getCityName(destinationCode)
        val countryName = getCountryName(destinationCode)

        val price = flight.price?.total?.replace("[^0-9.]".toRegex(), "")?.toDoubleOrNull() ?: 0.0

        // Generate better image URL using Unsplash with city-specific search
        val imageUrl = getCityImageUrl(cityName)
        
        // Generate unique ID that includes airline and time to differentiate flights
        val uniqueId = flight.id ?: "${destinationCode}_${firstSegment.carrierCode}_${firstSegment.departure?.at}_${UUID.randomUUID()}"
        
        return FlightDestination(
            id = uniqueId,
            name = cityName,
            city = cityName,
            country = countryName,
            imageUrl = imageUrl,
            price = price,
            currency = flight.price?.currency ?: "EUR",
            description = "Flight to $cityName via ${firstSegment.carrierCode ?: "various airlines"}",
            departureDate = firstSegment.departure?.at,
            arrivalDate = lastSegment.arrival?.at,
            airline = firstSegment.carrierCode
        )
    }

    private fun convertExploreOfferToDestination(offer: ExploreOffer): FlightDestination? {
        val cityName = offer.cityTo ?: return null
        val countryName = offer.countryTo?.name ?: "Unknown"
        
        // Generate better image URL using Unsplash with city-specific search
        val imageUrl = getCityImageUrl(cityName)

        return FlightDestination(
            id = offer.id ?: UUID.randomUUID().toString(),
            name = cityName,
            city = cityName,
            country = countryName,
            imageUrl = imageUrl,
            price = offer.price?.toDouble() ?: 0.0,
            currency = offer.currency ?: "EUR",
            description = "Flight to $cityName, ${countryName}",
            departureDate = offer.localDeparture,
            arrivalDate = offer.localArrival,
            airline = offer.airlines?.firstOrNull()
        )
    }

    private fun getCityName(airportCode: String): String {
        // Simple mapping - in production, use a proper airport database
        return when (airportCode) {
            "CDG", "ORY" -> "Paris"
            "LHR", "LGW" -> "London"
            "JFK", "LGA" -> "New York"
            "LAX" -> "Los Angeles"
            "DXB" -> "Dubai"
            "FCO" -> "Rome"
            "MAD" -> "Madrid"
            "BCN" -> "Barcelona"
            "AMS" -> "Amsterdam"
            "FRA" -> "Frankfurt"
            "MUC" -> "Munich"
            "IST" -> "Istanbul"
            "CAI" -> "Cairo"
            "TUN" -> "Tunis"
            else -> airportCode
        }
    }

    private fun getCountryName(airportCode: String): String {
        return when (airportCode) {
            "CDG", "ORY" -> "France"
            "LHR", "LGW" -> "United Kingdom"
            "JFK", "LGA", "LAX" -> "United States"
            "DXB" -> "UAE"
            "FCO" -> "Italy"
            "MAD", "BCN" -> "Spain"
            "AMS" -> "Netherlands"
            "FRA", "MUC" -> "Germany"
            "IST" -> "Turkey"
            "CAI" -> "Egypt"
            "TUN" -> "Tunisia"
            else -> "Unknown"
        }
    }

    private fun getCityImageUrl(cityName: String): String {
        // Return city-specific high-quality Unsplash images
        return when (cityName.lowercase()) {
            "paris" -> "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800&h=600&fit=crop&q=80"
            "london" -> "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800&h=600&fit=crop&q=80"
            "new york" -> "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800&h=600&fit=crop&q=80"
            "dubai" -> "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=800&h=600&fit=crop&q=80"
            "rome" -> "https://images.unsplash.com/photo-1529260830199-42c24126f198?w=800&h=600&fit=crop&q=80"
            "madrid" -> "https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=800&h=600&fit=crop&q=80"
            "barcelona" -> "https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=800&h=600&fit=crop&q=80"
            "amsterdam" -> "https://images.unsplash.com/photo-1534351590666-13e3e96b5017?w=800&h=600&fit=crop&q=80"
            "frankfurt" -> "https://images.unsplash.com/photo-1587330979470-3585ac3ac6cd?w=800&h=600&fit=crop&q=80"
            "munich" -> "https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=800&h=600&fit=crop&q=80"
            "istanbul" -> "https://images.unsplash.com/photo-1524231757912-21f4fe3a7200?w=800&h=600&fit=crop&q=80"
            "cairo" -> "https://images.unsplash.com/photo-1572252009286-268acec5ca0a?w=800&h=600&fit=crop&q=80"
            "tunis" -> "https://images.unsplash.com/photo-1572252009286-268acec5ca0a?w=800&h=600&fit=crop&q=80"
            "los angeles" -> "https://images.unsplash.com/photo-1515895306158-439192690299?w=800&h=600&fit=crop&q=80"
            else -> "https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=800&h=600&fit=crop&q=80" // Generic travel image
        }
    }
}

