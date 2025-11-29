import Foundation

// Helper function pour accéder à BookingService.shared depuis un contexte non isolé
nonisolated private func getBookingServiceShared() -> BookingService {
    return MainActor.assumeIsolated {
        BookingService.shared
    }
}

@MainActor
final class BookingViewModel: ObservableObject {
    @Published var bookings: [Booking] = []
    @Published var offers: [BookingOffer] = []
    @Published var isLoading = false
    @Published var isLoadingOffers = false
    @Published var errorMessage: String?
    @Published var offersErrorMessage: String?
    
    private let service: BookingService
    
    nonisolated init(service: BookingService? = nil) {
        // Accéder à .shared depuis un contexte non isolé
        if let service = service {
            self.service = service
        } else {
            // Utiliser une fonction helper nonisolated pour accéder à .shared
            self.service = getBookingServiceShared()
        }
    }
    
    /// Charge l'historique des réservations
    func loadHistory() async {
        guard !isLoading else { return }
        isLoading = true
        defer { isLoading = false }
        errorMessage = nil
        
        print("🔄 [BookingViewModel] Loading booking history")
        do {
            var allBookings = try await service.getBookingHistory()
            print("✅ [BookingViewModel] Loaded \(allBookings.count) bookings")
            
            // Filtrer automatiquement les réservations expirées
            let expiredCount = allBookings.count
            allBookings = filterExpiredBookings(allBookings)
            let remainingCount = allBookings.count
            
            if expiredCount != remainingCount {
                print("🗑️ [BookingViewModel] Filtered out \(expiredCount - remainingCount) expired bookings")
            }
            
            bookings = allBookings
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
    func confirmBooking(offerId: String, paymentDetails: [String: Any], totalPrice: Double?) async throws -> ConfirmBookingResponse {
        print("🔄 [BookingViewModel] Confirming booking for offer: \(offerId)")
        let response = try await service.confirmBooking(offerId: offerId, paymentDetails: paymentDetails, totalPrice: totalPrice)
        print("✅ [BookingViewModel] Booking confirmed: \(response.confirmationNumber)")
        
        // Recharger l'historique après confirmation
        await loadHistory()
        
        return response
    }
    
    /// Supprime définitivement une réservation (hors annulation)
    func deleteBooking(id: String) async throws {
        print("🔄 [BookingViewModel] Permanently deleting booking: \(id)")
        try await service.deleteBooking(id: id)
        print("✅ [BookingViewModel] Booking permanently deleted successfully")
        
        // Retirer la réservation de la liste localement
        bookings.removeAll { $0.id == id }
        
        // Recharger l'historique pour s'assurer de la synchronisation
        await loadHistory()
    }
    
    /// Annule une réservation (change le statut à cancelled)
    func cancelBooking(id: String) async throws {
        print("🔄 [BookingViewModel] Cancelling booking: \(id)")
        try await service.cancelBooking(id: id)
        print("✅ [BookingViewModel] Booking cancelled successfully")
        
        // Recharger l'historique pour mettre à jour le statut
        await loadHistory()
    }
    
    /// Filtre les réservations expirées (dont la date de voyage ou de création est passée)
    func filterExpiredBookings(_ bookings: [Booking]) -> [Booking] {
        let now = Date()
        let calendar = Calendar.current
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        var expiredBookingIds: [String] = []
        
        let validBookings = bookings.filter { booking in
            var isExpired = false
            var expirationReason = ""
            
            // D'abord, vérifier la date de création - si elle est dans le passé, la réservation est expirée
            let createdAtString = booking.createdAt
            var createdAt: Date?
            
            // Essayer plusieurs formats de date pour la date de création
            if let date = formatter.date(from: createdAtString) {
                createdAt = date
            } else {
                // Format ISO8601 sans fractions
                let simpleFormatter = ISO8601DateFormatter()
                simpleFormatter.formatOptions = [.withInternetDateTime]
                createdAt = simpleFormatter.date(from: createdAtString)
            }
            
            // Vérifier si la date de création est dans le passé (jour actuel inclus)
            if let createdAt = createdAt {
                // Comparer les dates au niveau du jour (ignorer l'heure)
                let startOfToday = calendar.startOfDay(for: now)
                let startOfCreationDate = calendar.startOfDay(for: createdAt)
                
                // Si la date de création est avant aujourd'hui, considérer comme expirée
                if startOfCreationDate < startOfToday {
                    isExpired = true
                    let daysSinceCreation = calendar.dateComponents([.day], from: startOfCreationDate, to: startOfToday).day ?? 0
                    expirationReason = "created on \(createdAtString) (\(daysSinceCreation) day(s) ago)"
                }
            }
            
            // Ensuite, vérifier les dates de voyage si elles existent
            if !isExpired {
                // Si la réservation a une date de retour, vérifier qu'elle n'est pas passée
                if let returnDateString = booking.returnDate,
                   let returnDate = formatter.date(from: returnDateString) {
                    let startOfReturnDate = calendar.startOfDay(for: returnDate)
                    let startOfToday = calendar.startOfDay(for: now)
                    
                    if startOfReturnDate < startOfToday {
                        isExpired = true
                        expirationReason = "return date \(returnDateString) is in the past"
                    }
                }
                // Si pas de date de retour, vérifier la date de départ
                else if let departureDateString = booking.departureDate,
                        let departureDate = formatter.date(from: departureDateString) {
                    let startOfDepartureDate = calendar.startOfDay(for: departureDate)
                    let startOfToday = calendar.startOfDay(for: now)
                    
                    if startOfDepartureDate < startOfToday {
                        isExpired = true
                        expirationReason = "departure date \(departureDateString) is in the past"
                    }
                }
            }
            
            // Si on ne peut pas parser la date de création, considérer comme expirée pour être sûr
            if createdAt == nil {
                isExpired = true
                expirationReason = "cannot parse createdAt date: \(createdAtString)"
                print("⚠️ [BookingViewModel] Cannot parse createdAt date: \(createdAtString) for booking \(booking.confirmationNumber)")
            }
            
            if isExpired {
                expiredBookingIds.append(booking.id)
                print("🗑️ [BookingViewModel] Booking \(booking.confirmationNumber) is expired (\(expirationReason)), will be removed")
            }
            
            return !isExpired
        }
        
        // Supprimer les réservations expirées du serveur en arrière-plan
        if !expiredBookingIds.isEmpty {
            print("🗑️ [BookingViewModel] Found \(expiredBookingIds.count) expired bookings to delete from server")
            Task {
                await deleteExpiredBookings(ids: expiredBookingIds)
            }
        }
        
        return validBookings
    }
    
    /// Supprime les réservations expirées du serveur
    private func deleteExpiredBookings(ids: [String]) async {
        print("🗑️ [BookingViewModel] Deleting \(ids.count) expired bookings from server")
        for id in ids {
            do {
                try await service.cancelBooking(id: id)
                print("✅ [BookingViewModel] Deleted expired booking: \(id)")
            } catch {
                print("❌ [BookingViewModel] Failed to delete expired booking \(id): \(error.localizedDescription)")
            }
        }
    }
    
    /// Vérifie si une réservation annulée peut être réactivée
    func canReBook(_ booking: Booking) -> Bool {
        // Toute réservation annulée peut être réactivée
        return booking.status == .cancelled
    }
    
    /// Réserve à nouveau une réservation annulée
    func reBook(_ booking: Booking) async throws -> ConfirmBookingResponse {
        print("🔄 [BookingViewModel] Re-booking cancelled booking: \(booking.id)")
        
        // Pour réserver à nouveau, on doit avoir un offerId ou destinationId
        // On va utiliser la destination comme offerId si disponible
        let offerId = booking.destination
        
        // Créer les détails de paiement (peut être vide pour une réactivation)
        let paymentDetails: [String: Any] = [
            "method": "rebooking",
            "original_booking_id": booking.id
        ]
        
        // Utiliser le prix original si disponible
        let totalPrice = booking.price
        
        let response = try await confirmBooking(
            offerId: offerId,
            paymentDetails: paymentDetails,
            totalPrice: totalPrice
        )
        
        print("✅ [BookingViewModel] Re-booking successful: \(response.confirmationNumber)")
        return response
    }
    
    /// Réactive une réservation annulée en changeant son statut à "confirmed"
    func reactivateBooking(_ booking: Booking) async throws {
        guard booking.status == .cancelled else {
            throw NSError(domain: "BookingViewModel", code: -1, userInfo: [NSLocalizedDescriptionKey: "La réservation n'est pas annulée"])
        }
        
        print("🔄 [BookingViewModel] Reactivating cancelled booking: \(booking.id)")
        
        do {
            // Mettre à jour le statut de la réservation à "confirmed"
            _ = try await service.updateBooking(
                id: booking.id,
                status: "confirmed"
            )
            
            print("✅ [BookingViewModel] Booking reactivated: \(booking.confirmationNumber)")
        } catch {
            print("❌ [BookingViewModel] Error reactivating booking: \(error.localizedDescription)")
            // Même en cas d'erreur de décodage, le serveur a peut-être quand même mis à jour
            // On va recharger l'historique pour vérifier
            print("⚠️ [BookingViewModel] Reloading history to check if update succeeded")
        }
        
        // Toujours recharger l'historique pour mettre à jour la liste
        // Cela permet de récupérer l'état actuel depuis le serveur
        await loadHistory()
    }
}

