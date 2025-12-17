import Foundation

struct UserPreview: Codable {
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
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        // Handle _id - try string first, fallback to any string value
        if let idString = try? container.decode(String.self, forKey: .id) {
            id = idString
        } else {
            // If not a string, try to extract string value from any type
            id = String(describing: try container.decode(AnyCodable.self, forKey: .id).value)
        }
        
        username = try container.decodeIfPresent(String.self, forKey: .username) ?? ""
        firstName = try container.decodeIfPresent(String.self, forKey: .firstName)
        lastName = try container.decodeIfPresent(String.self, forKey: .lastName)
        profileImageUrl = try container.decodeIfPresent(String.self, forKey: .profileImageUrl)
        followedAt = try container.decodeIfPresent(String.self, forKey: .followedAt)
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(username, forKey: .username)
        try container.encodeIfPresent(firstName, forKey: .firstName)
        try container.encodeIfPresent(lastName, forKey: .lastName)
        try container.encodeIfPresent(profileImageUrl, forKey: .profileImageUrl)
        try container.encodeIfPresent(followedAt, forKey: .followedAt)
    }
    
    // Manual init for fallback cases
    init(id: String, username: String, firstName: String?, lastName: String?, profileImageUrl: String?, followedAt: String?) {
        self.id = id
        self.username = username
        self.firstName = firstName
        self.lastName = lastName
        self.profileImageUrl = profileImageUrl
        self.followedAt = followedAt
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
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        // Handle _id
        if let idString = try? container.decode(String.self, forKey: .id) {
            id = idString
        } else {
            id = try container.decode(String.self, forKey: .id)
        }
        
        // Handle userId - can be object or string
        if let userIdObj = try? container.decode(UserPreview.self, forKey: .userId) {
            userId = userIdObj
        } else if let userIdString = try? container.decode(String.self, forKey: .userId) {
            // Create a minimal UserPreview if userId is just a string
            userId = UserPreview(id: userIdString, username: "", firstName: nil, lastName: nil, profileImageUrl: nil, followedAt: nil)
        } else {
            // Try to decode as object with fallback
            userId = try container.decode(UserPreview.self, forKey: .userId)
        }
        
        title = try container.decode(String.self, forKey: .title)
        description = try container.decodeIfPresent(String.self, forKey: .description)
        tripType = try container.decodeIfPresent(String.self, forKey: .tripType) ?? "custom"
        tripId = try container.decodeIfPresent(String.self, forKey: .tripId)
        images = try container.decodeIfPresent([String].self, forKey: .images) ?? []
        tags = try container.decodeIfPresent([String].self, forKey: .tags) ?? []
        metadata = try container.decodeIfPresent([String: AnyCodable].self, forKey: .metadata)
        likesCount = try container.decodeIfPresent(Int.self, forKey: .likesCount) ?? 0
        commentsCount = try container.decodeIfPresent(Int.self, forKey: .commentsCount) ?? 0
        sharesCount = try container.decodeIfPresent(Int.self, forKey: .sharesCount) ?? 0
        isPublic = try container.decodeIfPresent(Bool.self, forKey: .isPublic) ?? true
        isVisible = try container.decodeIfPresent(Bool.self, forKey: .isVisible) ?? true
        
        // Handle date fields - try both snake_case and camelCase
        createdAt = (try? container.decode(String.self, forKey: .createdAt)) ?? ISO8601DateFormatter().string(from: Date())
        updatedAt = (try? container.decode(String.self, forKey: .updatedAt)) ?? ISO8601DateFormatter().string(from: Date())
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

struct CountryMemory: Codable, Identifiable {
    let id: String
    let country: String
    let lat: Double
    let lng: Double
    let trips: [SharedTrip]
    let count: Int
    
    enum CodingKeys: String, CodingKey {
        case country
        case lat
        case lng
        case trips
        case count
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        country = try container.decode(String.self, forKey: .country)
        id = country // Use country name as id
        lat = try container.decode(Double.self, forKey: .lat)
        lng = try container.decode(Double.self, forKey: .lng)
        trips = try container.decode([SharedTrip].self, forKey: .trips)
        count = try container.decode(Int.self, forKey: .count)
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(country, forKey: .country)
        try container.encode(lat, forKey: .lat)
        try container.encode(lng, forKey: .lng)
        try container.encode(trips, forKey: .trips)
        try container.encode(count, forKey: .count)
    }
}

struct MapMemoriesResponse: Codable {
    let countries: [CountryMemory]
    let totalCountries: Int
    let totalMemories: Int
    
    enum CodingKeys: String, CodingKey {
        case countries
        case totalCountries
        case totalMemories
    }
}

struct GoogleMapsApiKeyResponse: Decodable {
    let apiKey: String?
    
    enum CodingKeys: String, CodingKey {
        case apiKey = "api_key"
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
        
        // Handle null values first - decodeNil() returns true if nil
        if container.decodeNil() {
            value = Optional<Any>.none as Any
            return
        }
        
        // Try to decode each type in order
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
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "AnyCodable value cannot be decoded: unknown type")
        }
    }
    
    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        
        // Check if value represents nil (NSNull or similar)
        if value is NSNull {
            try container.encodeNil()
        } else {
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
}

