import Foundation
import SwiftUI

@MainActor
final class SocialViewModel: ObservableObject {
    @Published var sharedTrips: [SharedTrip] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let service: SocialService
    
    init(service: SocialService = SocialService.shared) {
        self.service = service
    }
    
    func loadSocialFeed(limit: Int = 20, skip: Int = 0) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        
        do {
            sharedTrips = try await service.getSocialFeed(limit: limit, skip: skip)
        } catch {
            errorMessage = error.localizedDescription
            print("❌ [SocialViewModel] Error loading feed: \(error.localizedDescription)")
        }
    }
    
    func shareTrip(_ request: ShareTripRequest) async throws -> SharedTrip {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        
        do {
            let trip = try await service.shareTrip(request)
            sharedTrips.insert(trip, at: 0)
            return trip
        } catch {
            errorMessage = error.localizedDescription
            print("❌ [SocialViewModel] Error sharing trip: \(error.localizedDescription)")
            throw error
        }
    }
    
    func likeSharedTrip(id: String) async {
        do {
            _ = try await service.likeSharedTrip(id: id)
            // Note: SharedTrip est immutable, donc on doit recréer la liste
            // Pour simplifier, on recharge le feed
            await loadSocialFeed()
        } catch {
            print("❌ [SocialViewModel] Error liking trip: \(error.localizedDescription)")
        }
    }
}

