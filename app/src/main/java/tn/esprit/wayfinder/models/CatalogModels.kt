package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.io.Serializable as JavaSerializable

@Serializable
data class FlightOffer(
    val id: String? = null,
    val price: FlightPrice? = null,
    val itineraries: List<FlightItinerary>? = null,
    val travelerPricings: List<TravelerPricing>? = null,
    val validatingAirlineCodes: List<String>? = null,
    val source: String? = null
)

@Serializable
data class FlightPrice(
    val total: String? = null,
    val base: String? = null,
    val currency: String? = "EUR"
)

@Serializable
data class FlightItinerary(
    val duration: String? = null,
    val segments: List<FlightSegment>? = null
)

@Serializable
data class FlightSegment(
    val departure: FlightLocation? = null,
    val arrival: FlightLocation? = null,
    val carrierCode: String? = null,
    val number: String? = null,
    val aircraft: FlightAircraft? = null,
    val duration: String? = null
)

@Serializable
data class FlightLocation(
    val iataCode: String? = null,
    val terminal: String? = null,
    val at: String? = null
)

@Serializable
data class FlightAircraft(
    val code: String? = null
)

@Serializable
data class TravelerPricing(
    val travelerId: String? = null,
    val fareOption: String? = null,
    val travelerType: String? = null,
    val price: FlightPrice? = null
)

// Explore offers from Tequila API
@Serializable
data class ExploreOffer(
    val id: String? = null,
    val flyFrom: String? = null,
    @SerialName("flyTo") val flyTo: String? = null,
    val cityFrom: String? = null,
    val cityTo: String? = null,
    val countryFrom: ExploreCountry? = null,
    val countryTo: ExploreCountry? = null,
    val price: Int? = null,
    val currency: String? = "EUR",
    val localDeparture: String? = null,
    val localArrival: String? = null,
    val utcDeparture: String? = null,
    val utcArrival: String? = null,
    val airlines: List<String>? = null,
    val route: List<ExploreRoute>? = null,
    val bookingToken: String? = null,
    val deepLink: String? = null,
    val mapIdfrom: String? = null,
    val mapIdto: String? = null
)

@Serializable
data class ExploreCountry(
    val code: String? = null,
    val name: String? = null
)

@Serializable
data class ExploreRoute(
    val flyFrom: String? = null,
    val flyTo: String? = null,
    val cityFrom: String? = null,
    val cityTo: String? = null,
    val airline: String? = null,
    val flightNo: Int? = null,
    val localArrival: String? = null,
    val utcArrival: String? = null,
    val localDeparture: String? = null,
    val utcDeparture: String? = null
)

// Simplified destination model for UI (renamed to avoid conflict with RecommendationModels.Destination)
@Serializable
data class FlightDestination(
    val id: String,
    val name: String,
    val city: String,
    val country: String,
    val imageUrl: String? = null,
    val price: Double? = null,
    val currency: String = "EUR",
    val description: String? = null,
    val departureDate: String? = null,
    val arrivalDate: String? = null,
    val airline: String? = null
) : JavaSerializable

// Response wrapper for recommended flights
@Serializable
data class RecommendedFlightsResponse(
    val data: List<FlightOffer>? = null,
    val meta: JsonElement? = null
)

// Response wrapper for explore offers
@Serializable
data class ExploreOffersResponse(
    val data: List<ExploreOffer>? = null,
    val currency: String? = null,
    val all_stopover_airports: List<String>? = null
)

