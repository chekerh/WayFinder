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
        
        // Utiliser un décodage personnalisé pour gérer item_data correctement
        let (data, _) = try await APIService.shared.requestRaw(builder)
        
        // Décoder avec JSONSerialization d'abord pour avoir le contrôle sur item_data
        guard let jsonArray = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
            throw APIError.decodingError(NSError(domain: "FavoriteService", code: -1, userInfo: [NSLocalizedDescriptionKey: "Invalid JSON format"]))
        }
        
        var favorites: [Favorite] = []
        for jsonDict in jsonArray {
            // Décoder manuellement chaque favori
            guard let id = jsonDict["_id"] as? String,
                  let userId = jsonDict["user_id"] as? String,
                  let itemTypeStr = jsonDict["item_type"] as? String,
                  let itemType = FavoriteItemType(rawValue: itemTypeStr),
                  let itemId = jsonDict["item_id"] as? String,
                  let itemDataDict = jsonDict["item_data"] as? [String: Any] else {
                print("⚠️ [FavoriteService] Skipping invalid favorite entry")
                continue
            }
            
            // Parser item_data
            let name = itemDataDict["name"] as? String
            let city = itemDataDict["city"] as? String
            let country = itemDataDict["country"] as? String
            let currency = itemDataDict["currency"] as? String
            let airline = itemDataDict["airline"] as? String
            
            // Gérer imageUrl (peut être imageUrl ou image_url)
            var imageUrl: String? = nil
            if let url = itemDataDict["imageUrl"] as? String {
                imageUrl = url
            } else if let url = itemDataDict["image_url"] as? String {
                imageUrl = url
            }
            
            // Gérer le prix (peut être String, Double ou Int)
            var price: Double? = nil
            if let priceDouble = itemDataDict["price"] as? Double {
                price = priceDouble
            } else if let priceString = itemDataDict["price"] as? String {
                price = Double(priceString)
            } else if let priceInt = itemDataDict["price"] as? Int {
                price = Double(priceInt)
            }
            
            let itemData = FavoriteItemData(
                name: name,
                city: city,
                country: country,
                imageUrl: imageUrl,
                price: price,
                currency: currency,
                airline: airline
            )
            
            // Parser la date
            var favoritedAt = Date()
            if let dateString = jsonDict["favorited_at"] as? String {
                let formatter = ISO8601DateFormatter()
                formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
                favoritedAt = formatter.date(from: dateString) ?? ISO8601DateFormatter().date(from: dateString) ?? Date()
            }
            
            let favorite = Favorite(
                id: id,
                userId: userId,
                itemType: itemType,
                itemId: itemId,
                itemData: itemData,
                favoritedAt: favoritedAt
            )
            favorites.append(favorite)
        }
        
        return favorites
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
        // Vérifier si un token est présent avant de faire l'appel
        guard let token = TokenStorage.fetch(), !token.isEmpty else {
            print("⚠️ [FavoriteService] No token found, cannot check favorite")
            return false // Retourner false si pas de token (pas en favori par défaut)
        }
        
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

