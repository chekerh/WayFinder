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
            // Try to decode as paginated response first
            if let paginatedResponse = try? decoder.decode(PaginatedResponse<Booking>.self, from: data) {
                print("✅ [BookingService] Successfully decoded \(paginatedResponse.data.count) bookings from paginated response (page \(paginatedResponse.pagination.page)/\(paginatedResponse.pagination.totalPages))")
                return paginatedResponse.data
            }
            
            // Fallback: try to decode as direct array (for backward compatibility)
            let result = try decoder.decode([Booking].self, from: data)
            print("✅ [BookingService] Successfully decoded \(result.count) bookings from array response")
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
            
            // Provide a more user-friendly error message
            let errorMessage = "Erreur de décodage JSON: \(error.localizedDescription)"
            throw APIError.decodingError(NSError(domain: "BookingService", code: -1, userInfo: [NSLocalizedDescriptionKey: errorMessage]))
        }
    }
    
    /// Récupère toutes les réservations (alias de history)
    func getAllBookings() async throws -> [Booking] {
        let builder = DefaultRequest(
            method: "GET",
            path: "booking"
        )
        // Decode paginated response and extract data array
        let paginatedResponse: PaginatedResponse<Booking> = try await APIService.shared.request(builder, decodeTo: PaginatedResponse<Booking>.self)
        return paginatedResponse.data
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
        
        // Use requestRaw to get raw data and manually decode for better error handling
        let (data, _) = try await APIService.shared.requestRaw(builder)
        
        // Log raw JSON for debugging
        if let jsonString = String(data: data, encoding: .utf8) {
            print("🔍 [BookingService] Raw JSON response for update: \(jsonString.prefix(1000))")
        }
        
        // Si la réponse est vide, considérer que la mise à jour a réussi
        guard !data.isEmpty else {
            print("⚠️ [BookingService] Empty response from server, considering update successful")
            // Recharger la réservation depuis le serveur pour obtenir la version mise à jour
            return try await getBooking(id: id)
        }
        
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        
        do {
            let result = try decoder.decode(Booking.self, from: data)
            print("✅ [BookingService] Successfully decoded updated booking")
            return result
        } catch {
            print("❌ [BookingService] Decoding error for updated booking: \(error)")
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
            // Si erreur de décodage, essayer de recharger la réservation depuis le serveur
            print("⚠️ [BookingService] Trying to reload booking from server")
            return try await getBooking(id: id)
        }
    }
    
    /// Annule une réservation
    func cancelBooking(id: String) async throws {
        let builder = DefaultRequest(
            method: "DELETE",
            path: "booking/\(id)"
        )
        let _: EmptyResponse = try await APIService.shared.request(builder, decodeTo: EmptyResponse.self)
    }
    
    /// Supprime définitivement une réservation (hors annulation)
    func deleteBooking(id: String) async throws {
        let builder = DefaultRequest(
            method: "DELETE",
            path: "booking/\(id)/permanent"
        )
        let _: EmptyResponse = try await APIService.shared.request(builder, decodeTo: EmptyResponse.self)
    }
}

