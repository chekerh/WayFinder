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
        
        // Use requestRaw to get raw data and manually decode for better error handling
        let (data, _) = try await APIService.shared.requestRaw(builder)
        
        // Log raw JSON for debugging
        if let jsonString = String(data: data, encoding: .utf8) {
            print("🔍 [ReviewService] Raw JSON response for reviews: \(jsonString.prefix(1000))")
        }
        
        let decoder = JSONDecoder()
        // Configure date decoder to handle ISO8601 with fractional seconds
        let dateFormatter = ISO8601DateFormatter()
        dateFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        decoder.dateDecodingStrategy = .custom { decoder in
            let container = try decoder.singleValueContainer()
            let dateString = try container.decode(String.self)
            
            // Try with fractional seconds first
            if let date = dateFormatter.date(from: dateString) {
                return date
            }
            
            // Fallback to ISO8601 without fractional seconds
            let simpleFormatter = ISO8601DateFormatter()
            simpleFormatter.formatOptions = [.withInternetDateTime]
            if let date = simpleFormatter.date(from: dateString) {
                return date
            }
            
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "Invalid date format: \(dateString)")
        }
        decoder.keyDecodingStrategy = .useDefaultKeys
        
        do {
            // First, check if it's a paginated response by checking for "data" key
            if let jsonObject = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               jsonObject["data"] != nil {
                // It's a paginated response
                do {
                    let paginatedResponse = try decoder.decode(PaginatedResponse<Review>.self, from: data)
                    print("✅ [ReviewService] Successfully decoded \(paginatedResponse.data.count) reviews from paginated response")
                    return paginatedResponse.data
                } catch let paginationError {
                    print("❌ [ReviewService] Failed to decode paginated response: \(paginationError)")
                    // Try to extract data array manually with the same decoder
                    if let dataArray = jsonObject["data"] as? [[String: Any]] {
                        print("⚠️ [ReviewService] Attempting manual extraction of reviews from data array")
                        let reviews = try dataArray.map { reviewDict -> Review in
                            let reviewData = try JSONSerialization.data(withJSONObject: reviewDict)
                            // Use the same decoder with proper date handling
                            return try decoder.decode(Review.self, from: reviewData)
                        }
                        print("✅ [ReviewService] Manually decoded \(reviews.count) reviews")
                        return reviews
                    }
                    throw paginationError
                }
            } else {
                // Try to decode as direct array (for backward compatibility)
                let result = try decoder.decode([Review].self, from: data)
                print("✅ [ReviewService] Successfully decoded \(result.count) reviews from array response")
                return result
            }
        } catch {
            print("❌ [ReviewService] Decoding error for reviews: \(error)")
            if let decodingError = error as? DecodingError {
                switch decodingError {
                case .keyNotFound(let key, let context):
                    print("❌ [ReviewService] Missing key: \(key.stringValue) at path: \(context.codingPath)")
                case .typeMismatch(let type, let context):
                    print("❌ [ReviewService] Type mismatch for type \(type) at path: \(context.codingPath)")
                case .valueNotFound(let type, let context):
                    print("❌ [ReviewService] Value not found for type \(type) at path: \(context.codingPath)")
                case .dataCorrupted(let context):
                    print("❌ [ReviewService] Data corrupted at path: \(context.codingPath), \(context.debugDescription)")
                @unknown default:
                    print("❌ [ReviewService] Unknown decoding error: \(decodingError)")
                }
            }
            throw error
        }
    }
    
    /// Récupère les statistiques d'avis pour un item
    func getReviewStats(itemType: ReviewItemType, itemId: String) async throws -> ReviewStats {
        let builder = DefaultRequest(
            method: "GET",
            path: "reviews/\(itemType.rawValue)/\(itemId)/stats"
        )
        
        // Use requestRaw to get raw data and manually decode
        let (data, _) = try await APIService.shared.requestRaw(builder)
        
        // Log raw JSON for debugging
        if let jsonString = String(data: data, encoding: .utf8) {
            print("🔍 [ReviewService] Raw JSON response for stats: \(jsonString)")
        }
        
        let decoder = JSONDecoder()
        // Backend returns camelCase (averageRating, totalReviews, ratingDistribution)
        // So we use default keys without conversion
        decoder.keyDecodingStrategy = .useDefaultKeys
        
        do {
            let result = try decoder.decode(ReviewStats.self, from: data)
            print("✅ [ReviewService] Successfully decoded review stats")
            return result
        } catch {
            print("❌ [ReviewService] Decoding error for stats: \(error)")
            if let decodingError = error as? DecodingError {
                switch decodingError {
                case .keyNotFound(let key, let context):
                    print("❌ [ReviewService] Missing key: \(key.stringValue) at path: \(context.codingPath)")
                case .typeMismatch(let type, let context):
                    print("❌ [ReviewService] Type mismatch for type \(type) at path: \(context.codingPath)")
                case .valueNotFound(let type, let context):
                    print("❌ [ReviewService] Value not found for type \(type) at path: \(context.codingPath)")
                case .dataCorrupted(let context):
                    print("❌ [ReviewService] Data corrupted at path: \(context.codingPath), \(context.debugDescription)")
                @unknown default:
                    print("❌ [ReviewService] Unknown decoding error: \(decodingError)")
                }
            }
            throw error
        }
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

