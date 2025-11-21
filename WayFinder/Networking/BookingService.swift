import Foundation

@MainActor
final class BookingService {
    static let shared = BookingService()
    private init() {}
    
    /// Recherche des offres disponibles pour une destination
    func searchOffers(destination: String) async throws -> [BookingOffer] {
        let builder = DefaultRequest(
            method: "GET",
            path: "booking/offers",
            queryItems: [URLQueryItem(name: "destination", value: destination)]
        )
        return try await APIService.shared.request(builder, decodeTo: [BookingOffer].self)
    }
    
    /// Confirme une réservation (nécessite authentification)
    func confirmBooking(offerId: String, paymentDetails: [String: Any], totalPrice: Double?) async throws -> ConfirmBookingResponse {
        // Manual encoding to handle payment_details as object
        var json: [String: Any] = [
            "offer_id": offerId,
            "payment_details": paymentDetails
        ]
        if let totalPrice = totalPrice {
            json["total_price"] = totalPrice
        }
        
        let body = try JSONSerialization.data(withJSONObject: json)
        
        let builder = DefaultRequest(
            method: "POST",
            path: "booking/confirm",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        
        // Use custom decoder for ConfirmBookingResponse to handle _id and createdAt properly
        let (data, _) = try await APIService.shared.requestRaw(builder)
        
        // Log raw JSON for debugging
        if let jsonString = String(data: data, encoding: .utf8) {
            print("🔍 [BookingService] Raw JSON response: \(jsonString)")
        }
        
        let decoder = JSONDecoder()
        // Don't use convertFromSnakeCase as we handle keys manually in init(from decoder:)
        decoder.dateDecodingStrategy = .iso8601
        
        do {
            let result = try decoder.decode(ConfirmBookingResponse.self, from: data)
            print("✅ [BookingService] Successfully decoded ConfirmBookingResponse")
            return result
        } catch {
            print("❌ [BookingService] Decoding error: \(error)")
            if let decodingError = error as? DecodingError {
                switch decodingError {
                case .keyNotFound(let key, let context):
                    print("❌ [BookingService] Missing key: \(key.stringValue) at path: \(context.codingPath)")
                case .typeMismatch(let type, let context):
                    print("❌ [BookingService] Type mismatch for type \(type) at path: \(context.codingPath)")
                case .valueNotFound(let type, let context):
                    print("❌ [BookingService] Value not found for type \(type) at path: \(context.codingPath)")
                case .dataCorrupted(let context):
                    print("❌ [BookingService] Data corrupted at path: \(context.codingPath), \(context.debugDescription)")
                @unknown default:
                    print("❌ [BookingService] Unknown decoding error: \(decodingError)")
                }
            }
            throw error
        }
    }
    
    /// Récupère l'historique des réservations de l'utilisateur (nécessite authentification)
    func getBookingHistory() async throws -> [Booking] {
        let builder = DefaultRequest(
            method: "GET",
            path: "booking/history"
        )
        
        // Use requestRaw to get raw data and manually decode for better error handling
        let (data, _) = try await APIService.shared.requestRaw(builder)
        
        // Log raw JSON for debugging
        if let jsonString = String(data: data, encoding: .utf8) {
            print("🔍 [BookingService] Raw JSON response for history: \(jsonString.prefix(1000))")
        }
        
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        
        do {
            let result = try decoder.decode([Booking].self, from: data)
            print("✅ [BookingService] Successfully decoded \(result.count) bookings")
            return result
        } catch {
            print("❌ [BookingService] Decoding error for booking history: \(error)")
            if let decodingError = error as? DecodingError {
                switch decodingError {
                case .keyNotFound(let key, let context):
                    print("❌ [BookingService] Missing key: \(key.stringValue) at path: \(context.codingPath)")
                case .typeMismatch(let type, let context):
                    print("❌ [BookingService] Type mismatch for type \(type) at path: \(context.codingPath)")
                case .valueNotFound(let type, let context):
                    print("❌ [BookingService] Value not found for type \(type) at path: \(context.codingPath)")
                case .dataCorrupted(let context):
                    print("❌ [BookingService] Data corrupted at path: \(context.codingPath), \(context.debugDescription)")
                @unknown default:
                    print("❌ [BookingService] Unknown decoding error: \(decodingError)")
                }
            }
            throw APIError.decodingError(error)
        }
    }
    
    /// Récupère toutes les réservations (alias de history)
    func getAllBookings() async throws -> [Booking] {
        let builder = DefaultRequest(
            method: "GET",
            path: "booking"
        )
        return try await APIService.shared.request(builder, decodeTo: [Booking].self)
    }
    
    /// Récupère une réservation par ID
    func getBooking(id: String) async throws -> Booking {
        let builder = DefaultRequest(
            method: "GET",
            path: "booking/\(id)"
        )
        return try await APIService.shared.request(builder, decodeTo: Booking.self)
    }
    
    /// Compare les offres pour une réservation
    func compareOffers(offerId: String) async throws -> OfferComparison {
        let builder = DefaultRequest(
            method: "GET",
            path: "booking/compare",
            queryItems: [URLQueryItem(name: "offer_id", value: offerId)]
        )
        return try await APIService.shared.request(builder, decodeTo: OfferComparison.self)
    }
    
    /// Crée une nouvelle réservation
    func createBooking(
        destinationId: String,
        offerId: String? = nil,
        travelDates: TravelDates? = nil,
        passengers: Int? = nil,
        totalPrice: Double? = nil,
        currency: String? = nil,
        status: String? = nil
    ) async throws -> Booking {
        let request = CreateBookingRequest(
            destinationId: destinationId,
            offerId: offerId,
            travelDates: travelDates,
            passengers: passengers,
            totalPrice: totalPrice,
            currency: currency,
            status: status
        )
        
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "POST",
            path: "booking",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        return try await APIService.shared.request(builder, decodeTo: Booking.self)
    }
    
    /// Met à jour une réservation
    func updateBooking(
        id: String,
        status: String? = nil,
        travelDates: TravelDates? = nil,
        passengers: Int? = nil,
        totalPrice: Double? = nil,
        currency: String? = nil
    ) async throws -> Booking {
        let request = UpdateBookingRequest(
            status: status,
            travelDates: travelDates,
            passengers: passengers,
            totalPrice: totalPrice,
            currency: currency
        )
        
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "PUT",
            path: "booking/\(id)",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        return try await APIService.shared.request(builder, decodeTo: Booking.self)
    }
    
    /// Annule une réservation
    func cancelBooking(id: String) async throws {
        let builder = DefaultRequest(
            method: "DELETE",
            path: "booking/\(id)"
        )
        let _: EmptyResponse = try await APIService.shared.request(builder, decodeTo: EmptyResponse.self)
    }
}

