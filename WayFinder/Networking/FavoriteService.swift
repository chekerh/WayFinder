import Foundation

@MainActor
final class FavoriteService {
    static let shared = FavoriteService()
    private init() {}
    
    /// Ajoute un favori
    func addFavorite(
        itemType: FavoriteItemType,
        itemId: String,
        itemData: [String: String]? = nil
    ) async throws -> Favorite {
        let request = CreateFavoriteRequest(
            itemType: itemType.rawValue,
            itemId: itemId,
            itemData: itemData
        )
        
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        let body = try encoder.encode(request)
        
        let builder = DefaultRequest(
            method: "POST",
            path: "favorites",
            headers: ["Content-Type": "application/json"],
            body: body
        )
        return try await APIService.shared.request(builder, decodeTo: Favorite.self)
    }
    
    /// Récupère tous les favoris de l'utilisateur
    func getFavorites(itemType: FavoriteItemType? = nil) async throws -> [Favorite] {
        var queryItems: [URLQueryItem]? = nil
        if let itemType = itemType {
            queryItems = [URLQueryItem(name: "type", value: itemType.rawValue)]
        }
        
        let builder = DefaultRequest(
            method: "GET",
            path: "favorites",
            queryItems: queryItems
        )
        return try await APIService.shared.request(builder, decodeTo: [Favorite].self)
    }
    
    /// Récupère le nombre de favoris
    func getFavoriteCount(itemType: FavoriteItemType? = nil) async throws -> Int {
        var queryItems: [URLQueryItem]? = nil
        if let itemType = itemType {
            queryItems = [URLQueryItem(name: "type", value: itemType.rawValue)]
        }
        
        let builder = DefaultRequest(
            method: "GET",
            path: "favorites/count",
            queryItems: queryItems
        )
        let response = try await APIService.shared.request(builder, decodeTo: FavoriteCountResponse.self)
        return response.count
    }
    
    /// Vérifie si un item est en favori
    func checkFavorite(itemType: FavoriteItemType, itemId: String) async throws -> Bool {
        let builder = DefaultRequest(
            method: "GET",
            path: "favorites/check/\(itemType.rawValue)/\(itemId)"
        )
        let response = try await APIService.shared.request(builder, decodeTo: CheckFavoriteResponse.self)
        return response.isFavorite
    }
    
    /// Supprime un favori
    func removeFavorite(itemType: FavoriteItemType, itemId: String) async throws {
        let builder = DefaultRequest(
            method: "DELETE",
            path: "favorites/\(itemType.rawValue)/\(itemId)"
        )
        let _: EmptyResponse = try await APIService.shared.request(builder, decodeTo: EmptyResponse.self)
    }
}

