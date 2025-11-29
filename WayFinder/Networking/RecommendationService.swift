import Foundation

@MainActor
final class RecommendationService {
    static let shared = RecommendationService()
    private init() {}
    
    /// Récupère les recommandations personnalisées
    func getPersonalizedRecommendations(
        type: String = "all",
        limit: Int = 10
    ) async throws -> RecommendationResponse {
        let queryItems = [
            URLQueryItem(name: "type", value: type),
            URLQueryItem(name: "limit", value: String(limit))
        ]
        
        let builder = DefaultRequest(
            method: "GET",
            path: "recommendations/personalized",
            queryItems: queryItems
        )
        
        let payload = try await APIService.shared.request(builder, decodeTo: PersonalizedRecommendationsPayload.self)
        return makeResponse(from: payload)
    }
    
    /// Récupère les recommandations personnalisées et retourne le payload complet
    func getPersonalizedRecommendationsPayload(
        type: String = "all",
        limit: Int = 10
    ) async throws -> PersonalizedRecommendationsPayload {
        let queryItems = [
            URLQueryItem(name: "type", value: type),
            URLQueryItem(name: "limit", value: String(limit))
        ]
        
        let builder = DefaultRequest(
            method: "GET",
            path: "recommendations/personalized",
            queryItems: queryItems
        )
        
        return try await APIService.shared.request(builder, decodeTo: PersonalizedRecommendationsPayload.self)
    }
    
    /// Régénère les recommandations personnalisées
    func regenerateRecommendations() async throws -> RecommendationResponse {
        let builder = DefaultRequest(
            method: "GET",
            path: "recommendations/regenerate"
        )
        
        let payload = try await APIService.shared.request(builder, decodeTo: PersonalizedRecommendationsPayload.self)
        return makeResponse(from: payload)
    }
    
    /// Méthode de compatibilité avec l'ancien code
    func fetchRecommendations(preferenceId: String?) async throws -> RecommendationResponse {
        return try await getPersonalizedRecommendations(type: "home", limit: 6)
    }
}

private extension RecommendationService {
    func makeResponse(from payload: PersonalizedRecommendationsPayload) -> RecommendationResponse {
        let highlights = payload.destinations?.map { RecommendationHighlight(destination: $0) } ?? []
        let storedName = UserStorage.fetchDisplayName() ?? "Wayfinder"
        let user = RecommendationUser(firstName: storedName)
        return RecommendationResponse(user: user, regions: [], highlights: highlights)
    }
}
