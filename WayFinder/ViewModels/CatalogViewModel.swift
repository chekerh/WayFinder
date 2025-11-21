import Foundation
import SwiftUI

enum CatalogUiState {
    case idle
    case loading
    case success(destinations: [FlightDestination], fromCache: Bool = false, lastUpdated: TimeInterval? = nil, source: String? = nil)
    case error(message: String)
}

@MainActor
final class CatalogViewModel: ObservableObject {
    @Published var uiState: CatalogUiState = .idle
    
    private let catalogService: CatalogService
    private let flightsCache: FlightsCache
    
    init(catalogService: CatalogService = .shared, flightsCache: FlightsCache = .shared) {
        self.catalogService = catalogService
        self.flightsCache = flightsCache
        
        // Initialize with cached data if available
        if let cached = flightsCache.read(), !cached.destinations.isEmpty {
            emitCachedFlights(cached, showAll: false)
        }
    }
    
    func loadRecommendedFlights(
        showAll: Bool = false,
        originLocationCode: String? = nil,
        destinationLocationCode: String? = nil,
        departureDate: String? = nil,
        returnDate: String? = nil,
        adults: Int? = nil,
        travelClass: String? = nil,
        currencyCode: String? = nil,
        maxResults: Int? = 30,
        maxPrice: Double? = nil
    ) async {
        // Only show loading if we don't have cached data
        let cached = flightsCache.read()
        if cached == nil || cached?.destinations.isEmpty == true {
            uiState = .loading
        }
        
        // Show cached data first if available
        if let cached = cached, !cached.destinations.isEmpty {
            emitCachedFlights(cached, showAll: showAll)
        }
        
        do {
            let response = try await catalogService.fetchRecommendedFlights(
                originLocationCode: originLocationCode,
                destinationLocationCode: destinationLocationCode,
                departureDate: departureDate,
                returnDate: returnDate,
                adults: adults,
                travelClass: travelClass,
                currencyCode: currencyCode,
                maxResults: maxResults,
                maxPrice: maxPrice
            )
            
            let destinations = (response.data ?? []).compactMap { flight in
                catalogService.convertFlightToDestination(flight)
            }
            
            if destinations.isEmpty {
                if cached == nil {
                    uiState = .error(message: "No flights available. Please ensure Amadeus API keys are configured in the backend.")
                }
                return
            }
            
            // Store in cache
            flightsCache.store(destinations, source: "network")
            
            // Determine source from meta
            let source: String? = {
                if let meta = response.meta, meta.fallback == true {
                    return "fallback"
                }
                return "network"
            }()
            
            emitSuccess(
                destinations: destinations,
                showAll: showAll,
                fromCache: false,
                lastUpdated: Date().timeIntervalSince1970,
                source: source
            )
        } catch {
            // Always try to show cached data if available, even on error
            if let cachedOnError = flightsCache.read(), !cachedOnError.destinations.isEmpty {
                emitCachedFlights(cachedOnError, showAll: showAll)
            } else {
                // Only show error if we have no cached data
                let errorMessage: String
                if let urlError = error as? URLError {
                    switch urlError.code {
                    case .notConnectedToInternet, .networkConnectionLost:
                        errorMessage = "Unable to connect to server. Please check your internet connection."
                    case .timedOut:
                        errorMessage = "Connection timeout. The server is not responding. Please try again later."
                    default:
                        errorMessage = "Network error. Please check your internet connection and try again."
                    }
                } else {
                    errorMessage = error.localizedDescription
                }
                uiState = .error(message: errorMessage)
            }
        }
    }
    
    private func emitCachedFlights(_ cached: CachedFlights, showAll: Bool) {
        emitSuccess(
            destinations: cached.destinations,
            showAll: showAll,
            fromCache: true,
            lastUpdated: cached.updatedAt,
            source: cached.source
        )
    }
    
    private func emitSuccess(
        destinations: [FlightDestination],
        showAll: Bool,
        fromCache: Bool,
        lastUpdated: TimeInterval?,
        source: String?
    ) {
        let prepared = prepareDestinations(destinations, showAll: showAll)
        if prepared.isEmpty {
            uiState = .error(message: "No flights available for the selected filters.")
            return
        }
        uiState = .success(
            destinations: prepared,
            fromCache: fromCache,
            lastUpdated: lastUpdated,
            source: source
        )
    }
    
    private func prepareDestinations(_ destinations: [FlightDestination], showAll: Bool) -> [FlightDestination] {
        // First, remove exact duplicates using stable ID
        let uniqueById = Array(Set(destinations))
        
        if showAll {
            // For "show all", group by city and show only the cheapest flight per city
            let grouped = uniqueById.reduce(into: [String: FlightDestination]()) { result, destination in
                let cityKey = destination.city.lowercased()
                let maxValue = Double.greatestFiniteMagnitude
                if result[cityKey] == nil || (destination.price ?? maxValue) < (result[cityKey]?.price ?? maxValue) {
                    result[cityKey] = destination
                }
            }
            let maxValue = Double.greatestFiniteMagnitude
            return Array(grouped.values).sorted { ($0.price ?? maxValue) < ($1.price ?? maxValue) }
        } else {
            // For home screen, show up to 2 flights per city
            var grouped = [String: [FlightDestination]]()
            let maxValue = Double.greatestFiniteMagnitude
            for destination in uniqueById.sorted(by: { ($0.price ?? maxValue) < ($1.price ?? maxValue) }) {
                let cityKey = destination.city.lowercased()
                if grouped[cityKey] == nil {
                    grouped[cityKey] = []
                }
                if grouped[cityKey]?.count ?? 0 < 2 {
                    grouped[cityKey]?.append(destination)
                }
            }
            // Use destinationKey to remove duplicates within same city
            var seen = Set<String>()
            var result: [FlightDestination] = []
            for cityFlights in grouped.values {
                for destination in cityFlights {
                    let key = destinationKey(destination)
                    if !seen.contains(key) {
                        seen.insert(key)
                        result.append(destination)
                    }
                }
            }
            return result.sorted { ($0.price ?? maxValue) < ($1.price ?? maxValue) }
        }
    }
    
    private func destinationKey(_ destination: FlightDestination) -> String {
        let city = destination.city.lowercased()
        let airline = destination.airline?.lowercased() ?? "unknown"
        let departure = destination.departureDate ?? ""
        let arrival = destination.arrivalDate ?? ""
        let priceKey = destination.price.map { String(format: "%.2f", $0) } ?? "0"
        return [city, airline, departure, arrival, priceKey].joined(separator: "|")
    }
}
