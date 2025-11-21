import Foundation

@MainActor
final class ReviewService {
    static let shared = ReviewService()
    private init() {}
    
    /// Crée un avis
    func createReview(
        itemType: ReviewItemType,
        itemId: String,
        rating: Int,
        comment: String? = nil,
        details: ReviewDetails? = nil
    ) async throws -> Review {
        let request = CreateReviewRequest(
            itemType: itemType.rawValue,
            itemId: itemId,
            rating: rating,
            comment: comment,
            details: details
        )
        
        let encoder = JSONEncoder()
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "POST",
            path: "reviews",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        return try await APIService.shared.request(builder, decodeTo: Review.self)
    }
    
    /// Met à jour un avis
    func updateReview(
        reviewId: String,
        rating: Int? = nil,
        comment: String? = nil,
        details: ReviewDetails? = nil
    ) async throws -> Review {
        let request = UpdateReviewRequest(
            rating: rating,
            comment: comment,
            details: details
        )
        
        let encoder = JSONEncoder()
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "PUT",
            path: "reviews/\(reviewId)",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        return try await APIService.shared.request(builder, decodeTo: Review.self)
    }
    
    /// Supprime un avis
    func deleteReview(reviewId: String) async throws {
        let builder = DefaultRequest(
            method: "DELETE",
            path: "reviews/\(reviewId)"
        )
        let _: EmptyResponse = try await APIService.shared.request(builder, decodeTo: EmptyResponse.self)
    }
    
    /// Récupère les avis pour un item
    func getReviews(itemType: ReviewItemType, itemId: String) async throws -> [Review] {
        let builder = DefaultRequest(
            method: "GET",
            path: "reviews/\(itemType.rawValue)/\(itemId)"
        )
        return try await APIService.shared.request(builder, decodeTo: [Review].self)
    }
    
    /// Récupère les statistiques d'avis pour un item
    func getReviewStats(itemType: ReviewItemType, itemId: String) async throws -> ReviewStats {
        let builder = DefaultRequest(
            method: "GET",
            path: "reviews/\(itemType.rawValue)/\(itemId)/stats"
        )
        return try await APIService.shared.request(builder, decodeTo: ReviewStats.self)
    }
    
    /// Récupère les avis de l'utilisateur
    func getUserReviews(itemType: ReviewItemType? = nil) async throws -> [Review] {
        var queryItems: [URLQueryItem]? = nil
        if let itemType = itemType {
            queryItems = [URLQueryItem(name: "type", value: itemType.rawValue)]
        }
        
        let builder = DefaultRequest(
            method: "GET",
            path: "reviews/user/my-reviews",
            queryItems: queryItems
        )
        return try await APIService.shared.request(builder, decodeTo: [Review].self)
    }
    
    /// Vérifie si l'utilisateur a déjà laissé un avis
    func checkUserReview(itemType: ReviewItemType, itemId: String) async throws -> Review? {
        let builder = DefaultRequest(
            method: "GET",
            path: "reviews/check/\(itemType.rawValue)/\(itemId)"
        )
        return try? await APIService.shared.request(builder, decodeTo: Review.self)
    }
}

