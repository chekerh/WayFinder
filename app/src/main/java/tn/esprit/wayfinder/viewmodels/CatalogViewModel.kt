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

                // Primary: Get recommended flights from Amadeus (personalized based on user preferences)
                try {
                    val flightsResponse = catalogRepository.getRecommendedFlights(
                        maxResults = 10
                    )
                    flightsResponse.data?.forEach { flight ->
                        val destination = convertFlightToDestination(flight)
                        if (destination != null) {
                            destinations.add(destination)
                        }
                    }
                } catch (e: Exception) {
                    // Log but don't fail - we'll try explore as fallback
                }

                // Fallback: Try to get explore offers (Tequila API - optional, restricted to B2B partners)
                // This is just a bonus - Amadeus alone is sufficient
                try {
                    val exploreResponse = catalogRepository.getExploreOffers(
                        origin = "TUN",
                        limit = 10
                    )
                    exploreOffers = exploreResponse.data
                    exploreResponse.data?.forEach { offer ->
                        val destination = convertExploreOfferToDestination(offer)
                        if (destination != null) {
                            destinations.add(destination)
                        }
                    }
                } catch (e: Exception) {
                    // Tequila is optional - if it fails, we continue with Amadeus results only
                    // No error thrown - this is expected if Tequila API key is not available
                }

                if (destinations.isEmpty()) {
                    _uiState.value = CatalogUiState.Error(
                        "No flights available. Please ensure Amadeus API keys are configured in the backend."
                    )
                } else {
                    _uiState.value = CatalogUiState.Success(
                        destinations = destinations.distinctBy { it.id },
                        exploreOffers = exploreOffers
                    )
                }
            } catch (e: Exception) {
                _uiState.value = CatalogUiState.Error(
                    e.message ?: "Failed to load flights. Please check backend API configuration."
                )
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

        return FlightDestination(
            id = flight.id ?: UUID.randomUUID().toString(),
            name = cityName,
            city = cityName,
            country = countryName,
            price = price,
            currency = flight.price?.currency ?: "EUR",
            description = "Flight to $cityName",
            departureDate = firstSegment.departure?.at,
            arrivalDate = lastSegment.arrival?.at,
            airline = firstSegment.carrierCode
        )
    }

    private fun convertExploreOfferToDestination(offer: ExploreOffer): FlightDestination? {
        val cityName = offer.cityTo ?: return null
        val countryName = offer.countryTo?.name ?: "Unknown"

        return FlightDestination(
            id = offer.id ?: UUID.randomUUID().toString(),
            name = cityName,
            city = cityName,
            country = countryName,
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
}

