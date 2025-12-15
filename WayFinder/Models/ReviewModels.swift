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
    
    // User info when populated
    let user: ReviewUser?
    
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
        
        // Handle userId - can be string, object ID dict, or populated user object
        var userIdString = ""
        var populatedUser: ReviewUser? = nil
        
        // Try to decode as populated user object first
        if let userInfo = try? container.decode(ReviewUserInfo.self, forKey: .userId) {
            // userId is populated with user object
            if let userIdValue = userInfo.id {
                userIdString = userIdValue
            }
            populatedUser = ReviewUser(
                username: userInfo.username,
                firstName: userInfo.firstName,
                lastName: userInfo.lastName,
                profileImageUrl: userInfo.profileImageUrl
            )
        } else if let userIdStringValue = try? container.decode(String.self, forKey: .userId) {
            userIdString = userIdStringValue
        } else if let userIdDict = try? container.decode([String: String].self, forKey: .userId),
                  let userIdValue = userIdDict["$oid"] {
            userIdString = userIdValue
        } else {
            userIdString = ""
        }
        
        userId = userIdString
        user = populatedUser
        
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

struct ReviewUser {
    let username: String?
    let firstName: String?
    let lastName: String?
    let profileImageUrl: String?
    
    var displayName: String {
        if let firstName = firstName, let lastName = lastName {
            return "\(firstName) \(lastName)"
        } else if let firstName = firstName {
            return firstName
        } else if let lastName = lastName {
            return lastName
        } else if let username = username {
            return username
        }
        return "User"
    }
}

// Helper struct for decoding populated user info
private struct ReviewUserInfo: Decodable {
    let id: String?
    let username: String?
    let firstName: String?
    let lastName: String?
    let profileImageUrl: String?
    
    enum CodingKeys: String, CodingKey {
        case id = "_id"
        case username
        case firstName = "first_name"
        case lastName = "last_name"
        case profileImageUrl = "profile_image_url"
    }
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        // Handle _id - can be string or object with $oid
        if let idString = try? container.decode(String.self, forKey: .id) {
            id = idString
        } else if let idDict = try? container.decode([String: String].self, forKey: .id),
                  let idValue = idDict["$oid"] {
            id = idValue
        } else {
            id = nil
        }
        
        username = try container.decodeIfPresent(String.self, forKey: .username)
        firstName = try container.decodeIfPresent(String.self, forKey: .firstName)
        lastName = try container.decodeIfPresent(String.self, forKey: .lastName)
        profileImageUrl = try container.decodeIfPresent(String.self, forKey: .profileImageUrl)
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

