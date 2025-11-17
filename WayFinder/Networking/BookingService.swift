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
    func confirmBooking(destination: String, offerId: String?) async throws -> ConfirmBookingResponse {
        let request = ConfirmBookingRequest(destination: destination, offerId: offerId)
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "POST",
            path: "booking/confirm",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        return try await APIService.shared.request(builder, decodeTo: ConfirmBookingResponse.self)
    }
    
    /// Récupère l'historique des réservations de l'utilisateur (nécessite authentification)
    func getBookingHistory() async throws -> [Booking] {
        let builder = DefaultRequest(
            method: "GET",
            path: "booking/history"
        )
        return try await APIService.shared.request(builder, decodeTo: [Booking].self)
    }
}

