package tn.esprit.wayfinder.presentation.chat

import tn.esprit.wayfinder.models.*
import tn.esprit.wayfinder.network.ApiService
import kotlin.math.roundToInt

class ChatRepository(private val apiService: ApiService) {

    suspend fun sendMessage(message: String, model: ChatModel? = null): ChatMessageResponse {
        return apiService.sendChatMessage(ChatMessageRequest(message, model))
    }

    suspend fun switchModel(model: ChatModel): SwitchModelResponse {
        return apiService.switchChatModel(SwitchModelRequest(model))
    }

    suspend fun getHistory(limit: Int = 50): List<ChatHistoryItem> {
        return apiService.getChatHistory(limit)
    }

    suspend fun clearHistory(): ClearHistoryResponse {
        return apiService.clearChatHistory()
    }

    suspend fun getAvailableModels(): AvailableModelsResponse {
        return apiService.getAvailableModels()
    }

    suspend fun getPersonalizedFlightPacks(limit: Int = 3): List<FlightPack> {
        val flights = runCatching {
            apiService.getRecommendedFlights(
                maxResults = limit
            )
        }.getOrNull()?.data.orEmpty()

        val mappedFlights = flights.mapNotNull { offer ->
            val firstItinerary = offer.itineraries?.firstOrNull()
            val lastItinerary = offer.itineraries?.lastOrNull()
            val firstSegment = firstItinerary?.segments?.firstOrNull()
            val lastSegment = lastItinerary?.segments?.lastOrNull()

            val origin = firstSegment?.departure?.iataCode ?: return@mapNotNull null
            val destination = lastSegment?.arrival?.iataCode ?: return@mapNotNull null
            val airline = offer.validatingAirlineCodes?.firstOrNull()
            val priceValue = offer.price?.total?.toDoubleOrNull()?.roundToInt()
            val currency = offer.price?.currency ?: "EUR"
            val priceLabel = priceValue?.let { "$it $currency" } ?: "${offer.price?.total ?: "—"} $currency"

            FlightPack(
                title = "$origin → $destination",
                price = priceLabel,
                origin = origin,
                destination = destination,
                airline = airline,
                details = offer.description ?: "Flight operated by ${airline ?: "various airlines"}"
            )
        }.take(limit)

        if (mappedFlights.isNotEmpty()) return mappedFlights

        val recommendations = apiService.getPersonalizedRecommendations(
            type = "destinations",
            limit = limit
        )

        val destinations = recommendations.destinations.orEmpty()
        if (destinations.isEmpty()) return emptyList()

        return destinations.map { destination ->
            val priceLabel = destination.estimatedCost?.let { estimated ->
                val rounded = estimated.flight.toInt()
                "≈$rounded ${estimated.currency}"
            } ?: "See details"

            FlightPack(
                title = destination.name,
                price = priceLabel,
                origin = "Wayfinder",
                destination = destination.name,
                airline = destination.highlights.firstOrNull(),
                details = destination.reason
            )
        }
    }
}

