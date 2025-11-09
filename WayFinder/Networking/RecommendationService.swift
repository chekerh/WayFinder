import Foundation

final class RecommendationService {
    static let shared = RecommendationService()
    private init() {}
    
    func fetchRecommendations(preferenceId: String?) async throws -> RecommendationResponse {
        var queryItems: [URLQueryItem]? = nil
        if let preferenceId, !preferenceId.isEmpty {
            queryItems = [URLQueryItem(name: "preferenceId", value: preferenceId)]
        }
        
        let builder = DefaultRequest(
            method: "GET",
            path: "recommendations",
            queryItems: queryItems
        )
        
        return try await APIService.shared.request(builder, decodeTo: RecommendationResponse.self)
    }
}
