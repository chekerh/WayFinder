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
        // Pour le type "home", charger depuis le cache d'abord
        if type == "home" {
            if let cachedPayload = AppDataCache.shared.recommendationsCache.load() {
                print("✅ [RecommendationService] Loaded recommendations from cache")
                
                // Mettre à jour en arrière-plan sans bloquer
                Task {
                    do {
                        let freshPayload = try await fetchRecommendationsFromAPI(type: type, limit: limit)
                        AppDataCache.shared.recommendationsCache.store(freshPayload)
                        print("✅ [RecommendationService] Updated cache with fresh recommendations")
                    } catch {
                        print("⚠️ [RecommendationService] Failed to update cache: \(error.localizedDescription)")
                    }
                }
                
                return makeResponse(from: cachedPayload)
            }
        }
        
        // Pas de cache : charger depuis l'API
        let payload = try await fetchRecommendationsFromAPI(type: type, limit: limit)
        
        // Sauvegarder dans le cache seulement pour le type "home"
        if type == "home" {
            AppDataCache.shared.recommendationsCache.store(payload)
        }
        
        return makeResponse(from: payload)
    }
    
    private func fetchRecommendationsFromAPI(
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
