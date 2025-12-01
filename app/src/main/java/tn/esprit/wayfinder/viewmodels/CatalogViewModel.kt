package tn.esprit.wayfinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tn.esprit.wayfinder.data.FlightsCache
import tn.esprit.wayfinder.data.CachedFlights
import tn.esprit.wayfinder.models.FlightDestination
import tn.esprit.wayfinder.models.FlightOffer
import tn.esprit.wayfinder.models.FlightSegment
import tn.esprit.wayfinder.presentation.catalog.CatalogRepository

sealed class CatalogUiState {
    object Idle : CatalogUiState()
    object Loading : CatalogUiState()
    data class Success(
        val destinations: List<FlightDestination>,
        val fromCache: Boolean = false,
        val lastUpdated: Long? = null,
        val source: String? = null
    ) : CatalogUiState()
    data class Error(val message: String) : CatalogUiState()
}

class CatalogViewModel(
    private val catalogRepository: CatalogRepository,
    private val flightsCache: FlightsCache
) : ViewModel() {

    private val _uiState = MutableStateFlow<CatalogUiState>(CatalogUiState.Idle)
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        // Initialize with cached data if available to avoid showing loading state unnecessarily
        viewModelScope.launch {
            val cached = flightsCache.read()
            if (cached != null && cached.destinations.isNotEmpty()) {
                emitCachedFlights(cached, showAll = false)
            }
        }
    }

    fun loadRecommendedFlights(
        showAll: Boolean = false,
        destinationLocationCode: String? = null,
        maxResults: Int? = null
    ) {
        viewModelScope.launch {
            // Only show loading if we don't have cached data
            val cached = flightsCache.read()
            if (cached == null || cached.destinations.isEmpty()) {
                _uiState.value = CatalogUiState.Loading
            }
            
            if (cached != null && cached.destinations.isNotEmpty() && destinationLocationCode == null) {
                emitCachedFlights(cached, showAll)
            }

            try {
                val destinations = fetchDestinationsFromNetwork(
                    destinationLocationCode = destinationLocationCode,
                    maxResults = maxResults
                )
                if (destinations.isEmpty()) {
                    if (cached == null) {
                        _uiState.value = CatalogUiState.Error(
                            "No flights available. Please ensure Amadeus API keys are configured in the backend."
                        )
                    }
                    return@launch
                }

                if (destinationLocationCode == null) {
                flightsCache.store(destinations, source = "network")
                }
                emitSuccess(destinations, showAll, fromCache = false, lastUpdated = System.currentTimeMillis(), source = "network")
            } catch (e: Exception) {
                // Always try to show cached data if available, even on error
                val cachedOnError = flightsCache.read()
                if (cachedOnError != null && cachedOnError.destinations.isNotEmpty() && destinationLocationCode == null) {
                    emitCachedFlights(cachedOnError, showAll)
                } else {
                    // Only show error if we have no cached data
                    val errorMessage = when {
                        e is java.net.UnknownHostException || e.cause is java.net.UnknownHostException -> {
                            "Unable to connect to server. Please check your internet connection."
                        }
                        e is java.net.SocketTimeoutException -> {
                            "Connection timeout. The server is not responding. Please try again later."
                        }
                        e is java.io.IOException -> {
                            "Network error. Please check your internet connection and try again."
                        }
                        else -> e.message ?: "Failed to load flights. Please check backend API configuration."
                    }
                    _uiState.value = CatalogUiState.Error(errorMessage)
                }
            }
        }
    }

    private suspend fun fetchDestinationsFromNetwork(
        destinationLocationCode: String? = null,
        maxResults: Int? = null
    ): List<FlightDestination> {
        val result = mutableListOf<FlightDestination>()
        val flightsResponse = catalogRepository.getRecommendedFlights(
            destinationLocationCode = destinationLocationCode,
            maxResults = maxResults ?: 30
        )
        flightsResponse.data?.forEach { flight ->
            val destination = convertFlightToDestination(flight)
            if (destination != null) {
                result.add(destination)
            }
        }
        return result
    }

    private fun emitCachedFlights(cached: CachedFlights, showAll: Boolean) {
        emitSuccess(
            destinations = cached.destinations,
            showAll = showAll,
            fromCache = true,
            lastUpdated = cached.updatedAt,
            source = cached.source ?: "cache"
        )
    }

    private fun emitSuccess(
        destinations: List<FlightDestination>,
        showAll: Boolean,
        fromCache: Boolean,
        lastUpdated: Long?,
        source: String?
    ) {
        val prepared = prepareDestinations(destinations, showAll)
        if (prepared.isEmpty()) {
            _uiState.value = CatalogUiState.Error("No flights available for the selected filters.")
            return
        }
        _uiState.value = CatalogUiState.Success(
            destinations = prepared,
            fromCache = fromCache,
            lastUpdated = lastUpdated,
            source = source
        )
    }

    private fun prepareDestinations(destinations: List<FlightDestination>, showAll: Boolean): List<FlightDestination> {
        // First, remove exact duplicates using stable ID
        val uniqueById = destinations.distinctBy { it.id }
        
        return if (showAll) {
            // For "show all", group by city and show only the cheapest flight per city
            // This prevents showing multiple flights to the same destination
            uniqueById
                .groupBy { it.city.lowercase() }
                .flatMap { (_, cityFlights) ->
                    // Sort by price and take only the cheapest per city
                    cityFlights
                        .sortedBy { it.price ?: Double.MAX_VALUE }
                        .take(1) // Only show one flight per city
                }
                .sortedBy { it.price ?: Double.MAX_VALUE } // Sort all by price
        } else {
            // For home screen, show up to 2 flights per city
            uniqueById
                .groupBy { it.city.lowercase() }
                .flatMap { (_, cityFlights) ->
                    cityFlights
                        .sortedBy { it.price ?: Double.MAX_VALUE }
                        .take(2)
                }
                .distinctBy { destinationKey(it) }
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
        
        // Generate stable ID based on flight characteristics to prevent duplicates
        val uniqueId = flight.id ?: buildStableFlightId(destinationCode, firstSegment, lastSegment, price)
        
        return FlightDestination(
            id = uniqueId,
            name = cityName,
            city = cityName,
            country = countryName,
            imageUrl = imageUrl,
            price = price,
            currency = flight.price?.currency ?: "EUR",
            description = flight.description ?: "Flight to $cityName via ${firstSegment.carrierCode ?: "various airlines"}",
            departureDate = firstSegment.departure?.at,
            arrivalDate = lastSegment.arrival?.at,
            airline = firstSegment.carrierCode
        )
    }

    private fun buildStableFlightId(
        destinationCode: String,
        firstSegment: FlightSegment,
        lastSegment: FlightSegment,
        price: Double
    ): String {
        val carrier = firstSegment.carrierCode ?: "UNKNOWN"
        val departure = firstSegment.departure?.at ?: "NA"
        val arrival = lastSegment.arrival?.at ?: "NA"
        val priceKey = "%.2f".format(price)
        return listOf(destinationCode.uppercase(), carrier.uppercase(), departure, arrival, priceKey).joinToString("_")
    }

    private fun destinationKey(destination: FlightDestination): String {
        val city = destination.city.lowercase()
        val airline = destination.airline?.lowercase() ?: "unknown"
        val departure = destination.departureDate ?: ""
        val arrival = destination.arrivalDate ?: ""
        val priceKey = destination.price?.let { "%.2f".format(it) } ?: "0"
        return listOf(city, airline, departure, arrival, priceKey).joinToString("|")
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
            "NRT", "HND" -> "Tokyo"
            "BKK" -> "Bangkok"
            "SIN" -> "Singapore"
            "ICN" -> "Seoul"
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
            "NRT", "HND" -> "Japan"
            "BKK" -> "Thailand"
            "SIN" -> "Singapore"
            "ICN" -> "South Korea"
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
            "tokyo" -> "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800&h=600&fit=crop&q=80"
            "bangkok" -> "https://images.unsplash.com/photo-1552465011-b4e21bf6e79a?w=800&h=600&fit=crop&q=80"
            "singapore" -> "https://images.unsplash.com/photo-1525625293386-3f8f99389edd?w=800&h=600&fit=crop&q=80"
            "seoul" -> "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=800&h=600&fit=crop&q=80"
            else -> "https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=800&h=600&fit=crop&q=80" // Generic travel image
        }
    }
}

