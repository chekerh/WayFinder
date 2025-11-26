import Foundation

struct UserPreview: Decodable {
    let id: String
    let username: String
    let firstName: String?
    let lastName: String?
    let profileImageUrl: String?
    let followedAt: String?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case username
        case firstName = "first_name"
        case lastName = "last_name"
        case profileImageUrl = "profile_image_url"
        case followedAt
    }
}

struct SharedTrip: Decodable, Identifiable {
    let id: String
    let userId: UserPreview
    let title: String
    let description: String?
    let tripType: String // "itinerary", "booking", "destination", "custom"
    let tripId: String?
    let images: [String]
    let tags: [String]
    let metadata: [String: AnyCodable]?
    let likesCount: Int
    let commentsCount: Int
    let sharesCount: Int
    let isPublic: Bool
    let isVisible: Bool
    let createdAt: String
    let updatedAt: String
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case userId
        case title
        case description
        case tripType = "trip_type"
        case tripId = "trip_id"
        case images
        case tags
        case metadata
        case likesCount = "likes_count"
        case commentsCount = "comments_count"
        case sharesCount = "shares_count"
        case isPublic = "is_public"
        case isVisible = "is_visible"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
    }
}

struct ShareTripRequest: Encodable {
    let title: String
    let description: String?
    let tripType: String
    let tripId: String?
    let images: [String]
    let tags: [String]
    let metadata: [String: AnyCodable]?
    let isPublic: Bool
    
    enum CodingKeys: String, CodingKey {
        case title
        case description
        case tripType
        case tripId
        case images
        case tags
        case metadata
        case isPublic
    }
}

struct UpdateSharedTripRequest: Encodable {
    let title: String?
    let description: String?
    let images: [String]?
    let tags: [String]?
    let isPublic: Bool?
    
    enum CodingKeys: String, CodingKey {
        case title
        case description
        case images
        case tags
        case isPublic
    }
}

struct LikeResponse: Decodable {
    let message: String
    let likesCount: Int
    
    enum CodingKeys: String, CodingKey {
        case message
        case likesCount
    }
}

// Helper pour gérer les valeurs JSON arbitraires
struct AnyCodable: Codable {
    let value: Any
    
    init(_ value: Any) {
        self.value = value
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        
        if let bool = try? container.decode(Bool.self) {
            value = bool
        } else if let int = try? container.decode(Int.self) {
            value = int
        } else if let double = try? container.decode(Double.self) {
            value = double
        } else if let string = try? container.decode(String.self) {
            value = string
        } else if let array = try? container.decode([AnyCodable].self) {
            value = array.map { $0.value }
        } else if let dictionary = try? container.decode([String: AnyCodable].self) {
            value = dictionary.mapValues { $0.value }
        } else {
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "AnyCodable value cannot be decoded")
        }
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        
        switch value {
        case let bool as Bool:
            try container.encode(bool)
        case let int as Int:
            try container.encode(int)
        case let double as Double:
            try container.encode(double)
        case let string as String:
            try container.encode(string)
        case let array as [Any]:
            try container.encode(array.map { AnyCodable($0) })
        case let dictionary as [String: Any]:
            try container.encode(dictionary.mapValues { AnyCodable($0) })
        default:
            throw EncodingError.invalidValue(value, EncodingError.Context(codingPath: container.codingPath, debugDescription: "AnyCodable value cannot be encoded"))
        }
    }
}

