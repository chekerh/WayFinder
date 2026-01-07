import Foundation

struct RecommendedFlightsResponse: Decodable {
    let data: [FlightOffer]?
    let meta: FlightMeta?
}

struct FlightMeta: Decodable {
    let fallback: Bool?
    
    enum CodingKeys: String, CodingKey {
        case fallback
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        fallback = try? container.decode(Bool.self, forKey: .fallback)
    }
}

struct FlightOffer: Decodable {
    let id: String?
    let price: FlightPrice?
    let itineraries: [FlightItinerary]?
    let travelerPricings: [TravelerPricing]?
    let validatingAirlineCodes: [String]?
    let source: String?
}

struct FlightPrice: Decodable {
    let total: String?
    let base: String?
    let currency: String?
}

struct FlightItinerary: Decodable {
    let duration: String?
    let segments: [FlightSegment]?
}

struct FlightSegment: Decodable {
    let departure: FlightLocation?
    let arrival: FlightLocation?
    let carrierCode: String?
    let number: String?
    let aircraft: FlightAircraft?
    let duration: String?
}

struct FlightAircraft: Decodable {
    let code: String?
}

struct FlightLocation: Decodable {
    let iataCode: String?
    let terminal: String?
    let at: String?
}

struct TravelerPricing: Decodable {
    let travelerId: String?
    let fareOption: String?
    let travelerType: String?
    let price: FlightPrice?
}

final class CatalogService {
    static let shared = CatalogService()
    private init() {}
    
    func fetchRecommendedFlights(
        originLocationCode: String? = nil,
        destinationLocationCode: String? = nil,
        departureDate: String? = nil,
        returnDate: String? = nil,
        adults: Int? = nil,
        travelClass: String? = nil,
        currencyCode: String? = nil,
        maxResults: Int? = nil,
        maxPrice: Double? = nil
    ) async throws -> RecommendedFlightsResponse {
        var queryItems: [URLQueryItem] = []
        
        if let origin = originLocationCode {
            queryItems.append(URLQueryItem(name: "originLocationCode", value: origin))
        }
        if let destination = destinationLocationCode {
            queryItems.append(URLQueryItem(name: "destinationLocationCode", value: destination))
        }
        if let date = departureDate {
            queryItems.append(URLQueryItem(name: "departureDate", value: date))
        }
        if let date = returnDate {
            queryItems.append(URLQueryItem(name: "returnDate", value: date))
        }
        if let adults = adults {
            queryItems.append(URLQueryItem(name: "adults", value: "\(adults)"))
        }
        if let travelClass = travelClass {
            queryItems.append(URLQueryItem(name: "travelClass", value: travelClass))
        }
        if let currency = currencyCode {
            queryItems.append(URLQueryItem(name: "currencyCode", value: currency))
        }
        if let max = maxResults {
            queryItems.append(URLQueryItem(name: "maxResults", value: "\(max)"))
        }
        if let price = maxPrice {
            queryItems.append(URLQueryItem(name: "maxPrice", value: "\(price)"))
        }
        
        let request = DefaultRequest(
            method: "GET",
            path: "/catalog/recommended",
            queryItems: queryItems.isEmpty ? nil : queryItems
        )
        
        return try await APIService.shared.request(request, decodeTo: RecommendedFlightsResponse.self)
    }
    
    // MARK: - Hotels API
    
    func searchHotels(
        locationCode: String,
        checkInDate: String,
        checkOutDate: String? = nil,
        adults: Int,
        children: Int? = nil,
        currencyCode: String? = nil,
        radius: Int? = nil,
        maxResults: Int? = nil,
        priceRangeMin: Double? = nil,
        priceRangeMax: Double? = nil,
        minRating: Int? = nil,
        boardType: String? = nil,
        accommodationType: String? = nil
    ) async throws -> AccommodationSearchResponse {
        // Créer une clé de cache basée sur la destination et le type d'hébergement
        let cacheKey = accommodationType ?? "hotel"
        let cache = AppDataCache.shared.hotelsCache(for: locationCode, type: cacheKey)
        
        // Charger depuis le cache d'abord si disponible
        if let cachedHotels = cache.load() {
            print("✅ [CatalogService] Loaded \(cachedHotels.count) hotels from cache for \(locationCode) (\(cacheKey))")
            
            // Mettre à jour en arrière-plan sans bloquer
            Task {
                do {
                    let freshResponse = try await fetchAccommodationsFromAPI(
                        locationCode: locationCode,
                        checkInDate: checkInDate,
                        checkOutDate: checkOutDate,
                        adults: adults,
                        children: children,
                        currencyCode: currencyCode,
                        radius: radius,
                        maxResults: maxResults,
                        priceRangeMin: priceRangeMin,
                        priceRangeMax: priceRangeMax,
                        minRating: minRating,
                        boardType: boardType,
                        accommodationType: accommodationType
                    )
                    cache.store(freshResponse.data)
                    print("✅ [CatalogService] Updated cache with \(freshResponse.data.count) accommodations")
                } catch {
                    print("⚠️ [CatalogService] Failed to update accommodations cache: \(error.localizedDescription)")
                }
            }
            
            return AccommodationSearchResponse(data: cachedHotels, meta: nil)
        }
        
        // Pas de cache : charger depuis l'API
        let response = try await fetchAccommodationsFromAPI(
            locationCode: locationCode,
            checkInDate: checkInDate,
            checkOutDate: checkOutDate,
            adults: adults,
            children: children,
            currencyCode: currencyCode,
            radius: radius,
            maxResults: maxResults,
            priceRangeMin: priceRangeMin,
            priceRangeMax: priceRangeMax,
            minRating: minRating,
            boardType: boardType,
            accommodationType: accommodationType
        )
        
        // Sauvegarder dans le cache
        cache.store(response.data)
        
        return response
    }
    
    private func fetchAccommodationsFromAPI(
        locationCode: String,
        checkInDate: String,
        checkOutDate: String? = nil,
        adults: Int,
        children: Int? = nil,
        currencyCode: String? = nil,
        radius: Int? = nil,
        maxResults: Int? = nil,
        priceRangeMin: Double? = nil,
        priceRangeMax: Double? = nil,
        minRating: Int? = nil,
        boardType: String? = nil,
        accommodationType: String? = nil
    ) async throws -> AccommodationSearchResponse {
        var queryItems: [URLQueryItem] = [
            URLQueryItem(name: "locationCode", value: locationCode),
            URLQueryItem(name: "checkInDate", value: checkInDate),
            URLQueryItem(name: "adults", value: "\(adults)")
        ]
        
        if let checkOut = checkOutDate {
            queryItems.append(URLQueryItem(name: "checkOutDate", value: checkOut))
        }
        if let children = children {
            queryItems.append(URLQueryItem(name: "children", value: "\(children)"))
        }
        if let currency = currencyCode {
            queryItems.append(URLQueryItem(name: "currencyCode", value: currency))
        }
        if let radius = radius {
            queryItems.append(URLQueryItem(name: "radius", value: "\(radius)"))
        }
        if let max = maxResults {
            queryItems.append(URLQueryItem(name: "maxResults", value: "\(max)"))
        }
        if let minPrice = priceRangeMin {
            queryItems.append(URLQueryItem(name: "priceRangeMin", value: "\(minPrice)"))
        }
        if let maxPrice = priceRangeMax {
            queryItems.append(URLQueryItem(name: "priceRangeMax", value: "\(maxPrice)"))
        }
        if let rating = minRating {
            queryItems.append(URLQueryItem(name: "minRating", value: "\(rating)"))
        }
        if let board = boardType {
            queryItems.append(URLQueryItem(name: "boardType", value: board))
        }
        if let accommodationType = accommodationType {
            queryItems.append(URLQueryItem(name: "accommodationType", value: accommodationType))
        }
        
        let request = DefaultRequest(
            method: "GET",
            path: "/catalog/accommodations/search",
            queryItems: queryItems
        )
        
        return try await APIService.shared.request(request, decodeTo: AccommodationSearchResponse.self)
    }
    
    func getHotelDetails(
        hotelId: String,
        checkInDate: String? = nil,
        checkOutDate: String? = nil,
        adults: Int? = nil,
        currencyCode: String? = nil
    ) async throws -> HotelDetailsResponse {
        var queryItems: [URLQueryItem] = []
        
        if let checkIn = checkInDate {
            queryItems.append(URLQueryItem(name: "checkInDate", value: checkIn))
        }
        if let checkOut = checkOutDate {
            queryItems.append(URLQueryItem(name: "checkOutDate", value: checkOut))
        }
        if let adults = adults {
            queryItems.append(URLQueryItem(name: "adults", value: "\(adults)"))
        }
        if let currency = currencyCode {
            queryItems.append(URLQueryItem(name: "currencyCode", value: currency))
        }
        
        let request = DefaultRequest(
            method: "GET",
            path: "/catalog/hotels/\(hotelId)",
            queryItems: queryItems.isEmpty ? nil : queryItems
        )
        
        return try await APIService.shared.request(request, decodeTo: HotelDetailsResponse.self)
    }
    
    // MARK: - Activities API
    
    func getActivities(
        city: String,
        limit: Int = 10,
        categories: [String]? = nil
    ) async throws -> ActivitiesResponse {
        var queryItems: [URLQueryItem] = [
            URLQueryItem(name: "city", value: city),
            URLQueryItem(name: "limit", value: "\(limit)")
        ]
        
        if let cats = categories, !cats.isEmpty {
            queryItems.append(URLQueryItem(name: "categories", value: cats.joined(separator: ",")))
        }
        
        let request = DefaultRequest(
            method: "GET",
            path: "/catalog/activities",
            queryItems: queryItems
        )
        
        return try await APIService.shared.request(request, decodeTo: ActivitiesResponse.self)
    }
    
    func getActivityFeed(
        latitude: Double? = nil,
        longitude: Double? = nil,
        city: String? = nil
    ) async throws -> ActivityFeedResponse {
        var queryItems: [URLQueryItem] = []
        
        if let lat = latitude, let lon = longitude {
            queryItems.append(URLQueryItem(name: "latitude", value: "\(lat)"))
            queryItems.append(URLQueryItem(name: "longitude", value: "\(lon)"))
        }
        if let city = city {
            queryItems.append(URLQueryItem(name: "city", value: city))
        }
        
        let request = DefaultRequest(
            method: "GET",
            path: "/catalog/activity-feed",
            queryItems: queryItems.isEmpty ? nil : queryItems
        )
        
        return try await APIService.shared.request(request, decodeTo: ActivityFeedResponse.self)
    }
}

// MARK: - Conversion logic (exact copy from Android)
extension CatalogService {
    func convertFlightToDestination(_ flight: FlightOffer) -> FlightDestination? {
        guard let firstItinerary = flight.itineraries?.first else {
            return nil
        }
        guard let firstSegment = firstItinerary.segments?.first else {
            return nil
        }
        let lastSegment = firstItinerary.segments?.last ?? firstSegment
        
        guard let destinationCode = lastSegment.arrival?.iataCode else {
            return nil
        }
        
        let cityName = getCityName(destinationCode)
        let countryName = getCountryName(destinationCode)
        
        // Parse price - remove non-numeric characters except decimal point
        let priceString = flight.price?.total ?? "0"
        let cleanedPrice = priceString.replacingOccurrences(of: "[^0-9.]", with: "", options: .regularExpression)
        let price = Double(cleanedPrice) ?? 0.0
        
        // Generate better image URL using Unsplash with city-specific search
        let imageUrl = getCityImageUrl(cityName)
        
        // Generate stable ID based on flight characteristics to prevent duplicates
        let uniqueId = flight.id ?? buildStableFlightId(
            destinationCode: destinationCode,
            firstSegment: firstSegment,
            lastSegment: lastSegment,
            price: price
        )
        
        return FlightDestination(
            id: uniqueId,
            name: cityName,
            city: cityName,
            country: countryName,
            imageUrl: imageUrl,
            price: price > 0 ? price : nil,
            currency: flight.price?.currency ?? "EUR",
            description: "Flight to \(cityName) via \(firstSegment.carrierCode ?? "various airlines")",
            departureDate: firstSegment.departure?.at,
            arrivalDate: lastSegment.arrival?.at,
            airline: firstSegment.carrierCode
        )
    }
    
    private func buildStableFlightId(
        destinationCode: String,
        firstSegment: FlightSegment,
        lastSegment: FlightSegment,
        price: Double
    ) -> String {
        let carrier = firstSegment.carrierCode ?? "UNKNOWN"
        let departure = firstSegment.departure?.at ?? "NA"
        let arrival = lastSegment.arrival?.at ?? "NA"
        let priceKey = String(format: "%.2f", price)
        return [
            destinationCode.uppercased(),
            carrier.uppercased(),
            departure,
            arrival,
            priceKey
        ].joined(separator: "_")
    }
    
    private func getCityName(_ airportCode: String) -> String {
        switch airportCode.uppercased() {
        case "CDG", "ORY": return "Paris"
        case "LHR", "LGW": return "London"
        case "JFK", "LGA": return "New York"
        case "LAX": return "Los Angeles"
        case "DXB": return "Dubai"
        case "FCO": return "Rome"
        case "MAD": return "Madrid"
        case "BCN": return "Barcelona"
        case "AMS": return "Amsterdam"
        case "FRA": return "Frankfurt"
        case "MUC": return "Munich"
        case "IST": return "Istanbul"
        case "CAI": return "Cairo"
        case "TUN": return "Tunis"
        case "BER": return "Berlin"
        case "VIE": return "Vienna"
        case "PRG": return "Prague"
        case "BUD": return "Budapest"
        case "ATH": return "Athens"
        case "LIS": return "Lisbon"
        case "CPH": return "Copenhagen"
        case "STO": return "Stockholm"
        case "OSL": return "Oslo"
        case "HEL": return "Helsinki"
        case "DUB": return "Dublin"
        case "EDI": return "Edinburgh"
        case "ZUR": return "Zurich"
        case "BRU": return "Brussels"
        default: return airportCode
        }
    }
    
    private func getCountryName(_ airportCode: String) -> String {
        switch airportCode.uppercased() {
        case "CDG", "ORY": return "France"
        case "LHR", "LGW": return "United Kingdom"
        case "JFK", "LGA", "LAX": return "United States"
        case "DXB": return "UAE"
        case "FCO": return "Italy"
        case "MAD", "BCN": return "Spain"
        case "AMS": return "Netherlands"
        case "FRA", "MUC", "BER": return "Germany"
        case "IST": return "Turkey"
        case "CAI": return "Egypt"
        case "TUN": return "Tunisia"
        case "VIE": return "Austria"
        case "PRG": return "Czech Republic"
        case "BUD": return "Hungary"
        case "ATH": return "Greece"
        case "LIS": return "Portugal"
        case "CPH": return "Denmark"
        case "STO": return "Sweden"
        case "OSL": return "Norway"
        case "HEL": return "Finland"
        case "DUB": return "Ireland"
        case "EDI": return "United Kingdom"
        case "ZUR": return "Switzerland"
        case "BRU": return "Belgium"
        default: return "Unknown"
        }
    }
    
    private func getCityImageUrl(_ cityName: String) -> String {
        switch cityName.lowercased() {
        case "paris":
            return "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800&h=600&fit=crop&q=80"
        case "london":
            return "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800&h=600&fit=crop&q=80"
        case "new york":
            return "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800&h=600&fit=crop&q=80"
        case "dubai":
            return "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=800&h=600&fit=crop&q=80"
        case "rome":
            return "https://images.unsplash.com/photo-1529260830199-42c24126f198?w=800&h=600&fit=crop&q=80"
        case "madrid":
            return "https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=800&h=600&fit=crop&q=80"
        case "barcelona":
            return "https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=800&h=600&fit=crop&q=80"
        case "amsterdam":
            return "https://images.unsplash.com/photo-1534351590666-13e3e96b5017?w=800&h=600&fit=crop&q=80"
        case "frankfurt":
            return "https://images.unsplash.com/photo-1587330979470-3585ac3ac6cd?w=800&h=600&fit=crop&q=80"
        case "munich":
            return "https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=800&h=600&fit=crop&q=80"
        case "istanbul":
            return "https://images.unsplash.com/photo-1524231757912-21f4fe3a7200?w=800&h=600&fit=crop&q=80"
        case "cairo":
            return "https://images.unsplash.com/photo-1572252009286-268acec5ca0a?w=800&h=600&fit=crop&q=80"
        case "tunis":
            return "https://images.unsplash.com/photo-1572252009286-268acec5ca0a?w=800&h=600&fit=crop&q=80"
        case "los angeles":
            return "https://images.unsplash.com/photo-1515895306158-439192690299?w=800&h=600&fit=crop&q=80"
        default:
            return "https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=800&h=600&fit=crop&q=80"
        }
    }
}
