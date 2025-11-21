import Foundation

enum ReviewItemType: String, Codable {
    case flight = "flight"
    case hotel = "hotel"
    case activity = "activity"
    case destination = "destination"
}

struct Review: Decodable, Identifiable {
    let id: String
    let userId: String
    let itemType: ReviewItemType
    let itemId: String
    let rating: Int
    let comment: String?
    let details: ReviewDetails?
    let isVisible: Bool
    let createdAt: Date?
    let updatedAt: Date?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case userId
        case itemType
        case itemId
        case rating
        case comment
        case details
        case isVisible
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
        
        // Handle userId
        if let userIdString = try? container.decode(String.self, forKey: .userId) {
            userId = userIdString
        } else if let userIdDict = try? container.decode([String: String].self, forKey: .userId),
                  let userIdValue = userIdDict["$oid"] {
            userId = userIdValue
        } else {
            userId = ""
        }
        
        itemType = try container.decode(ReviewItemType.self, forKey: .itemType)
        itemId = try container.decode(String.self, forKey: .itemId)
        rating = try container.decode(Int.self, forKey: .rating)
        comment = try container.decodeIfPresent(String.self, forKey: .comment)
        details = try container.decodeIfPresent(ReviewDetails.self, forKey: .details)
        isVisible = try container.decodeIfPresent(Bool.self, forKey: .isVisible) ?? true
        createdAt = nil
        updatedAt = nil
    }
}

struct ReviewDetails: Codable {
    let comfort: Double?
    let service: Double?
    let value: Double?
    let punctuality: Double?
    let cleanliness: Double?
    let location: Double?
    let amenities: Double?
    let experience: Double?
    let guide: Double?
}

struct ReviewStats: Decodable {
    let averageRating: Double
    let totalReviews: Int
    let ratingDistribution: [Int: Int]?
    
    enum CodingKeys: String, CodingKey {
        case averageRating = "average_rating"
        case totalReviews = "total_reviews"
        case ratingDistribution = "rating_distribution"
    }
}

struct CreateReviewRequest: Encodable {
    let itemType: String
    let itemId: String
    let rating: Int
    let comment: String?
    let details: ReviewDetails?
    
    enum CodingKeys: String, CodingKey {
        case itemType, itemId, rating, comment, details
    }
}

struct UpdateReviewRequest: Encodable {
    let rating: Int?
    let comment: String?
    let details: ReviewDetails?
}

