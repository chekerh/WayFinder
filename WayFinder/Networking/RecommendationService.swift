import Foundation

final class RecommendationService {
    static let shared = RecommendationService()
    private init() {}
    
    func fetchRecommendations(preferenceId: String?) async throws -> RecommendationResponse {
        var queryItems: [URLQueryItem]? = nil
        if let preferenceId, !preferenceId.isEmpty {
            queryItems = [URLQueryItem(name: "preferenceId", value: preferenceId)]
        }
        
        var requestQueryItems = queryItems ?? []
        requestQueryItems.append(URLQueryItem(name: "type", value: "home"))
        requestQueryItems.append(URLQueryItem(name: "limit", value: "6"))
        
        let builder = DefaultRequest(
            method: "GET",
            path: "recommendations/personalized",
            queryItems: requestQueryItems
        )
        
        return try await APIService.shared.request(builder, decodeTo: RecommendationResponse.self)
    }
}
