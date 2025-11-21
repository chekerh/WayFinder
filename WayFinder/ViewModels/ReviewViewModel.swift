import Foundation

@MainActor
final class ReviewViewModel: ObservableObject {
    @Published var reviews: [Review] = []
    @Published var reviewStats: ReviewStats?
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let service: ReviewService
    
    init(service: ReviewService = .shared) {
        self.service = service
    }
    
    func loadReviews(itemType: ReviewItemType, itemId: String) async {
        guard !isLoading else { return }
        isLoading = true
        defer { isLoading = false }
        errorMessage = nil
        
        print("🔄 [ReviewViewModel] Loading reviews")
        do {
            reviews = try await service.getReviews(itemType: itemType, itemId: itemId)
            print("✅ [ReviewViewModel] Loaded \(reviews.count) reviews")
        } catch {
            print("❌ [ReviewViewModel] Error loading reviews: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
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
            reviews.append(review)
            await loadReviewStats(itemType: itemType, itemId: itemId)
            print("✅ [ReviewViewModel] Review created")
        } catch {
            print("❌ [ReviewViewModel] Error creating review: \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }
}

