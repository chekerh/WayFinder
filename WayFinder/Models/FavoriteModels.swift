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
    
    // Initializer manuel pour permettre la création sans décodage
    init(id: String, userId: String, itemType: FavoriteItemType, itemId: String, itemData: FavoriteItemData, favoritedAt: Date, createdAt: Date? = nil, updatedAt: Date? = nil) {
        self.id = id
        self.userId = userId
        self.itemType = itemType
        self.itemId = itemId
        self.itemData = itemData
        self.favoritedAt = favoritedAt
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
    
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
        
        // Décoder itemData - le décodage normal devrait fonctionner
        // Si ça échoue, on crée un itemData vide pour ne pas bloquer le décodage
        do {
            itemData = try container.decode(FavoriteItemData.self, forKey: .itemData)
        } catch {
            print("❌ [Favorite] Error decoding itemData: \(error)")
            // Créer un itemData vide en cas d'erreur pour ne pas bloquer le décodage
            // (getFavorites utilise maintenant un décodage personnalisé qui gère item_data correctement)
            itemData = FavoriteItemData(
                name: nil,
                city: nil,
                country: nil,
                imageUrl: nil,
                price: nil,
                currency: nil,
                airline: nil
            )
        }
        
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
    
    // Initializer manuel pour permettre la création sans décodage
    init(name: String? = nil, city: String? = nil, country: String? = nil, imageUrl: String? = nil, price: Double? = nil, currency: String? = nil, airline: String? = nil) {
        self.name = name
        self.city = city
        self.country = country
        self.imageUrl = imageUrl
        self.price = price
        self.currency = currency
        self.airline = airline
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        // Décoder les champs simples
        name = try? container.decode(String.self, forKey: .name)
        city = try? container.decode(String.self, forKey: .city)
        country = try? container.decode(String.self, forKey: .country)
        currency = try? container.decode(String.self, forKey: .currency)
        airline = try? container.decode(String.self, forKey: .airline)
        
        // Gérer imageUrl qui peut être imageUrl ou image_url
        if let url = try? container.decode(String.self, forKey: .imageUrl) {
            imageUrl = url
        } else if let url = try? container.decode(String.self, forKey: .imageUrlSnakeCase) {
            imageUrl = url
        } else {
            imageUrl = nil
        }
        
        // Gérer le prix qui peut être String ou Double ou Int
        // Essayer d'abord comme Double
        if let priceDouble = try? container.decode(Double.self, forKey: .price) {
            price = priceDouble
        }
        // Puis comme String
        else if let priceString = try? container.decode(String.self, forKey: .price) {
            // Essayer de convertir la string en Double
            if let priceValue = Double(priceString) {
                price = priceValue
            } else {
                price = nil
            }
        }
        // Puis comme Int (au cas où)
        else if let priceInt = try? container.decode(Int.self, forKey: .price) {
            price = Double(priceInt)
        }
        // Sinon nil
        else {
            price = nil
        }
    }
    
    enum CodingKeys: String, CodingKey {
        case name
        case city
        case country
        case imageUrl
        case imageUrlSnakeCase = "image_url"
        case price
        case currency
        case airline
    }
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

// Helper pour décoder des clés dynamiques
struct DynamicCodingKeys: CodingKey {
    var stringValue: String
    var intValue: Int?
    
    init?(stringValue: String) {
        self.stringValue = stringValue
    }
    
    init?(intValue: Int) {
        self.intValue = intValue
        self.stringValue = "\(intValue)"
    }
}

