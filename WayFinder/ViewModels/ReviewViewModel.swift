import Foundation

// Helper function pour accéder à ReviewService.shared depuis un contexte non isolé
nonisolated private func getReviewServiceShared() -> ReviewService {
    // MainActor.assumeIsolated permet d'accéder à .shared depuis un contexte non isolé
    return MainActor.assumeIsolated {
        ReviewService.shared
    }
}

@MainActor
final class ReviewViewModel: ObservableObject {
    @Published var reviews: [Review] = []
    @Published var reviewStats: ReviewStats?
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let service: ReviewService
    
    nonisolated init(service: ReviewService? = nil) {
        // Accéder à .shared depuis un contexte non isolé
        if let service = service {
            self.service = service
        } else {
            // Utiliser une fonction helper nonisolated pour accéder à .shared
            self.service = getReviewServiceShared()
        }
    }
    
    func loadReviews(itemType: ReviewItemType, itemId: String) async {
        guard !isLoading else { return }
        isLoading = true
        defer { isLoading = false }
        errorMessage = nil
        
        print("🔄 [ReviewViewModel] Loading reviews for \(itemType.rawValue)/\(itemId)")
        do {
            var loadedReviews = try await service.getReviews(itemType: itemType, itemId: itemId)
            // Filter only visible reviews and sort by date (newest first)
            loadedReviews = loadedReviews
                .filter { $0.isVisible }
                .sorted { review1, review2 in
                    // Sort by createdAt, newest first
                    let date1 = review1.createdAt ?? Date.distantPast
                    let date2 = review2.createdAt ?? Date.distantPast
                    return date1 > date2
                }
            reviews = loadedReviews
            print("✅ [ReviewViewModel] Loaded \(reviews.count) visible reviews")
        } catch {
            print("❌ [ReviewViewModel] Error loading reviews: \(error.localizedDescription)")
            // Provide a more user-friendly error message
            if error.localizedDescription.contains("décodage JSON") || 
               error.localizedDescription.contains("decoding") ||
               error.localizedDescription.contains("format") {
                errorMessage = "Erreur de décodage JSON: Impossible de lire les données car le format n'est pas correct."
            } else {
                errorMessage = error.localizedDescription
            }
        }
    }
    
    func loadReviewStats(itemType: ReviewItemType, itemId: String) async {
        print("🔄 [ReviewViewModel] Loading review stats")
        do {
            reviewStats = try await service.getReviewStats(itemType: itemType, itemId: itemId)
            print("✅ [ReviewViewModel] Review stats loaded")
        } catch {
            print("❌ [ReviewViewModel] Error loading stats: \(error.localizedDescription)")
        }
    }
    
    func createReview(
        itemType: ReviewItemType,
        itemId: String,
        rating: Int,
        comment: String? = nil,
        details: ReviewDetails? = nil
    ) async {
        print("🔄 [ReviewViewModel] Creating review")
        do {
            let review = try await service.createReview(
                itemType: itemType,
                itemId: itemId,
                rating: rating,
                comment: comment,
                details: details
            )
            // Reload reviews to get the updated list from server
            await loadReviews(itemType: itemType, itemId: itemId)
            await loadReviewStats(itemType: itemType, itemId: itemId)
            print("✅ [ReviewViewModel] Review created and list refreshed")
        } catch {
            print("❌ [ReviewViewModel] Error creating review: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }
}

