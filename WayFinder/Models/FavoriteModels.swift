import Foundation

enum FavoriteItemType: String, Codable {
    case flight = "flight"
    case destination = "destination"
    case activity = "activity"
    case hotel = "hotel"
}

struct Favorite: Decodable, Identifiable {
    let id: String
    let userId: String
    let itemType: FavoriteItemType
    let itemId: String
    let itemData: FavoriteItemData
    let favoritedAt: Date
    let createdAt: Date?
    let updatedAt: Date?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case userId = "user_id"
        case itemType = "item_type"
        case itemId = "item_id"
        case itemData = "item_data"
        case favoritedAt = "favorited_at"
        case createdAt
        case updatedAt
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        // Handle MongoDB _id
        if let idString = try? container.decode(String.self, forKey: .id) {
            id = idString
        } else if let idDict = try? container.decode([String: String].self, forKey: .id),
                  let idValue = idDict["$oid"] {
            id = idValue
        } else {
            throw DecodingError.dataCorruptedError(forKey: .id, in: container, debugDescription: "Invalid id format")
        }
        
        userId = try container.decode(String.self, forKey: .userId)
        itemType = try container.decode(FavoriteItemType.self, forKey: .itemType)
        itemId = try container.decode(String.self, forKey: .itemId)
        itemData = try container.decode(FavoriteItemData.self, forKey: .itemData)
        
        if let dateString = try? container.decode(String.self, forKey: .favoritedAt) {
            let formatter = ISO8601DateFormatter()
            formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
            favoritedAt = formatter.date(from: dateString) ?? ISO8601DateFormatter().date(from: dateString) ?? Date()
        } else {
            favoritedAt = Date()
        }
        
        createdAt = nil
        updatedAt = nil
    }
}

struct FavoriteItemData: Decodable {
    let name: String?
    let city: String?
    let country: String?
    let imageUrl: String?
    let price: Double?
    let currency: String?
    let airline: String?
}

struct CreateFavoriteRequest: Encodable {
    let itemType: String
    let itemId: String
    let itemData: [String: String]?
    
    enum CodingKeys: String, CodingKey {
        case itemType = "item_type"
        case itemId = "item_id"
        case itemData = "item_data"
    }
}

struct FavoriteCountResponse: Decodable {
    let count: Int
}

struct CheckFavoriteResponse: Decodable {
    let isFavorite: Bool
}

