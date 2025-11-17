import Foundation

@MainActor
final class BookingViewModel: ObservableObject {
    @Published var bookings: [Booking] = []
    @Published var offers: [BookingOffer] = []
    @Published var isLoading = false
    @Published var isLoadingOffers = false
    @Published var errorMessage: String?
    @Published var offersErrorMessage: String?
    
    private let service: BookingService
    
    init(service: BookingService = .shared) {
        self.service = service
    }
    
    /// Charge l'historique des réservations
    func loadHistory() async {
        guard !isLoading else { return }
        isLoading = true
        defer { isLoading = false }
        errorMessage = nil
        
        print("🔄 [BookingViewModel] Loading booking history")
        do {
            bookings = try await service.getBookingHistory()
            print("✅ [BookingViewModel] Loaded \(bookings.count) bookings")
        } catch {
            print("❌ [BookingViewModel] Error loading history: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }
    
    /// Recherche des offres pour une destination
    func searchOffers(destination: String) async {
        guard !isLoadingOffers else { return }
        isLoadingOffers = true
        defer { isLoadingOffers = false }
        offersErrorMessage = nil
        
        print("🔄 [BookingViewModel] Searching offers for: \(destination)")
        do {
            offers = try await service.searchOffers(destination: destination)
            print("✅ [BookingViewModel] Found \(offers.count) offers")
        } catch {
            print("❌ [BookingViewModel] Error searching offers: \(error.localizedDescription)")
            offersErrorMessage = error.localizedDescription
        }
    }
    
    /// Confirme une réservation
    func confirmBooking(destination: String, offerId: String?) async throws -> ConfirmBookingResponse {
        print("🔄 [BookingViewModel] Confirming booking for: \(destination)")
        let response = try await service.confirmBooking(destination: destination, offerId: offerId)
        print("✅ [BookingViewModel] Booking confirmed: \(response.confirmationNumber)")
        
        // Recharger l'historique après confirmation
        await loadHistory()
        
        return response
    }
}

